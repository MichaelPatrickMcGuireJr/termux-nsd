package com.termux.nsd

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log

class NsdService : Service() {

    private lateinit var nsdManager: NsdManager
    private lateinit var multicastLock: WifiManager.MulticastLock
    private var registrationListener: NsdManager.RegistrationListener? = null

    companion object {
        const val TAG = "NsdService"
        const val CHANNEL_ID = "nsd_service_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_REGISTER = "com.termux.nsd.REGISTER"
        const val ACTION_UNREGISTER = "com.termux.nsd.UNREGISTER"
        const val EXTRA_SERVICE_NAME = "service_name"
        const val EXTRA_SERVICE_TYPE = "service_type"
        const val EXTRA_PORT = "port"
    }

    override fun onCreate() {
        super.onCreate()
        nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock(TAG)
        multicastLock.setReferenceCounted(true)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("mDNS running"))

        when (intent?.action) {
            ACTION_REGISTER -> {
                val name = intent.getStringExtra(EXTRA_SERVICE_NAME) ?: "termux"
                val type = intent.getStringExtra(EXTRA_SERVICE_TYPE) ?: "_http._tcp."
                val port = intent.getIntExtra(EXTRA_PORT, 8080)
                registerService(name, type, port)
            }
            ACTION_UNREGISTER -> unregisterService()
        }
        return START_STICKY
    }

    private fun registerService(name: String, type: String, port: Int) {
        multicastLock.acquire()

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = name
            serviceType = type
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.i(TAG, "Registered: ${info.serviceName}")
                updateNotification("Broadcasting: ${info.serviceName} on :${info.port}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) {
                Log.e(TAG, "Registration failed: $code")
                updateNotification("Registration failed (code $code)")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.i(TAG, "Unregistered: ${info.serviceName}")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {
                Log.e(TAG, "Unregistration failed: $code")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener!!)
    }

    private fun unregisterService() {
        registrationListener?.let { nsdManager.unregisterService(it) }
        if (multicastLock.isHeld) multicastLock.release()
    }

    override fun onDestroy() {
        unregisterService()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "mDNS Service", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Termux NSD")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }
}

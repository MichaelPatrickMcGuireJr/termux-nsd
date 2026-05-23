package com.termux.nsd

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nameField = findViewById<EditText>(R.id.field_name)
        val typeField = findViewById<EditText>(R.id.field_type)
        val portField = findViewById<EditText>(R.id.field_port)
        val statusText = findViewById<TextView>(R.id.text_status)
        val btnStart = findViewById<Button>(R.id.btn_start)
        val btnStop = findViewById<Button>(R.id.btn_stop)

        btnStart.setOnClickListener {
            val intent = Intent(this, NsdService::class.java).apply {
                action = NsdService.ACTION_REGISTER
                putExtra(NsdService.EXTRA_SERVICE_NAME, nameField.text.toString().ifBlank { "termux" })
                putExtra(NsdService.EXTRA_SERVICE_TYPE, typeField.text.toString().ifBlank { "_http._tcp." })
                putExtra(NsdService.EXTRA_PORT, portField.text.toString().toIntOrNull() ?: 8080)
            }
            startForegroundService(intent)
            statusText.text = "Broadcasting..."
        }

        btnStop.setOnClickListener {
            val intent = Intent(this, NsdService::class.java).apply {
                action = NsdService.ACTION_UNREGISTER
            }
            startService(intent)
            statusText.text = "Stopped"
        }
    }
}

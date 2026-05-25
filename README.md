# termux-nsd

Android app that broadcasts mDNS (DNS-SD) service records using `NsdManager`.
Part of the [claude-rs](https://github.com/embedded-engineer/claude-rs) local network stack.

## What it does

Runs as a persistent foreground service. Accepts `ACTION_REGISTER` / `ACTION_UNREGISTER`
intents and uses `WifiManager.MulticastLock` + `NsdManager` to broadcast `.local` service
names on the LAN — making Termux services discoverable by Termius and other mDNS clients.

## Permissions required

| Permission | Why |
|------------|-----|
| `CHANGE_WIFI_MULTICAST_STATE` | Acquire multicast lock for mDNS packets |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Keep service alive while broadcasting |
| `ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE` | Read network interface |
| `POST_NOTIFICATIONS` | Foreground service notification (Android 13+) |

## Install via Obtainium

Add `https://github.com/embedded-engineer/termux-nsd` — Obtainium will track releases
and notify on new APKs. Filter: `termux-nsd-signed`.

## Build

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Signing (CI)

GitHub Actions builds a signed APK on every `v*` tag. Uses org-level secrets from
`github.com/orgs/embedded-engineer/settings/secrets` — no per-repo setup needed.

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | base64 of `termux-dev.p12` |
| `KEYSTORE_PASS` | keystore password |
| `KEY_ALIAS` | `termux-dev` |
| `KEY_PASS` | key password |

The `termux-dev.p12` keystore is shared across all `embedded-engineer` releases.
Never regenerate it — all installed APKs are permanently tied to this key.

## Usage from Termux

```bash
# Start broadcasting watchdog.local on port 8080
am startservice \
  -n com.termux.nsd/.NsdService \
  -a com.termux.nsd.REGISTER \
  --es service_name watchdog \
  --es service_type _http._tcp. \
  --ei port 8080

# Stop
am startservice \
  -n com.termux.nsd/.NsdService \
  -a com.termux.nsd.UNREGISTER
```

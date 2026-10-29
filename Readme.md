# Simple Android Tasmota Scanner / Controller App

---

# Demo

preview online simulated demo

https://ldijkman.github.io/Tasmota-android-test/

---

No Ads, no fee, Free, unlimited, as is

no data collecting

simple android app apk

no account, no login, no cloud

works local in your own wifi network

no internet needed after install

no MQTT server or Home Assistant needed

no tracking, no analytics

location is only sent to your own tasmota switch

settings and found devices stay on your phone

open source, all code in the zip

small app, starts fast

small download / small install size +/-100Kb ?

android 7.0 or newer

simple android app apk

changed dutch to englisch

---

# APK Android App

## Build date 28 October 2026

https://github.com/ldijkman/Tasmota-android-test/releases/download/build-107/PlafondLED.apk

![Tap Toch installeren](toch-installeren-flash.gif).

install anyway maybe in english


grok.com can also build apk app from plafond-led-app.zip

hmmmm, claude.ai can also build android apk app from plafond-led-app.zip


---

scan / control tasmota android app

find control timed tasmota wifi wall sockets

scan find control tasmota devices in local wifi network

scan network for tasmota wifi switches wall socket devices in +/- 2 seconds

control on off toggle

should i add countdown off?

      http://10.10.100.114/cm?cmnd=Backlog%20Power1%201%3B%20Delay%20600%3B%20Power1%200

set 8 on off timers

with or without suntimes

on off timers are saved in tasmota switch

android app made by claude.ai 

and build compiled by github


# Tasmota Android app

A small, fast Android app to find and control **Tasmota switches** on your own Wi-Fi network.
Fully local: no cloud, no account, no MQTT server needed.

<p align="center">
  <img src="Screenshot_20261009-205015.jpg" width="300" alt="Network scan: all Tasmota switches with name and IP">
  &nbsp;&nbsp;
  <img src="Screenshot_20261009-203512.jpg" width="300" alt="Controls and two schedules with sunset">
</p>
<p align="center"><sub>Left: network scan · Right: controls and schedules</sub></p>

 <img src="Screenshot_20261010-151334.jpg" width="300" alt="">

 <img src="Screenshot_20261010-151207.jpg" width="300" alt="">

setting for which ip range to scan


<img src="Screenshot_20261011-082013_Settings.jpg" width="300" alt="">

Button inside app to change / switch the phone's wifi connection easily

<img src="Screenshot_20261011-202604.jpg" width="300" alt="">

added sunrise sunset for active device

and ip link to visit the device page



<img src="Screenshot_20261012-171735.jpg" width="300" alt="">

store scan found devices

with x to remove them

<img src="Screenshot_20261012-173131.jpg" width="300" alt="">

change friendlyname and devicename from app


<img src="Screenshot_20261012-182930.jpg" width="300" alt="">

set phone location lat lon 

in tasmota device for sunrise sunset

<img src="Screenshot_20261012-213333.jpg" width="300" alt="">

<img src="Screenshot_20261012-213322.jpg" width="300" alt="">

set timezone maybe

<img src="Screenshot_20261013-165929.jpg" width="300" alt="">

toggle in scan/search result

<img src="Screenshot_20261014-164709.jpg" width="300" alt="">

drag drop order scan search results

and auto save store sort order

<img src="Screenshot_20261014-165907.jpg" width="300" alt="">

drag to order

<img src="Screenshot_20261015-172317.jpg" width="300" alt="">

create labels groups to drag into

bit strange silly to do this in search results


<img src="Screenshot_20261015-192401.jpg" width="300" alt="">

 < > minimize maximize group to labeltext or full view

<img src="Screenshot_20261019-172020.jpg" width="300" alt="">

wifi signal, time up, edit, switch group and more changes 

<img src="Screenshot_20261019-191637.jpg" width="300" alt="">

upto 8 on/off timers that live on tasmota

graphic timeline preview

floating top right hamburger menu


<img src="Screenshot_20261019-200153.jpg" width="300" alt="">

Hamburger about

<img src="Screenshot_20261021-173920.jpg" width="300" alt="">

group label edit to choose a color

<img src="Screenshot_20261021-174241.jpg" width="300" alt="">


<img src="Screenshot_20261024-082456.jpg" width="300" alt="">

added a clock schedules link to found devices

opens schedules view for that device

now possible to copy schedule from another device

<img src="Screenshot_20261024-190354.jpg" width="300" alt="">

added a button to generate tiles view

<img src="Screenshot_20261025-080634.jpg" width="300" alt="">

changed tiles


<img src="Screenshot_20261025-144023.jpg" width="300" alt="">

in device edit choose icon to use for tiles

<img src="Screenshot_20261025-194011.jpg" width="300" alt="">

some icons 

added widgets control a device from android homescreen
 - touch and hold main app icon until popup
 - then widgets, first widget is wifi changer, choose second

<img src="Screenshot_20261026-183040_One UI Home.jpg" width="300" alt="">

added widgets control a device from android homescreen
 - touch and hold main app icon until popup
 - then widgets, first widget is wifi changer, choose second

<img src="Screenshot_20261026-182902.jpg" width="300" alt="">


added widgets control a device from android homescreen
 - touch and hold main app icon until popup
 - then widgets, first widget is wifi changer, choose second


<img src="Screenshot_20261026-185117_One UI Home.jpg" width="300" alt="">


<img src="Screenshot_20261027-165442_Chrome.jpg" width="300" alt="">

text show on cards toggle 
opens a window
where you can set what to show hide

<img src="Screenshot_20261028-173105.jpg" width="300" alt="">

show energy if device has it


<img src="Screenshot_20261028-181040.jpg" width="300" alt="">

changes icons 


<img src="Screenshot_20261028-181522.jpg" width="300" alt="">

changes icons

<img src="Screenshot_20261028-182639_Chrome.jpg" width="300" alt="">

schedules sunrise sunset visual graphical above timeline bar

<img src="" width="300" alt="">
<img src="" width="300" alt="">
<img src="" width="300" alt="">
<img src="" width="300" alt="">
<img src="" width="300" alt="">
<img src="" width="300" alt="">
<img src="" width="300" alt="">



---

## Features

- 🔍 **Network scan**: finds all Tasmota devices on your network in a few seconds (`x.x.x.1–254`, 64 at a time).
  For each device you see the **FriendlyName**, **DeviceName**, **topic**, **IP address** and whether it is **on or off**.
  Found devices are saved; remove one with the **×** (after a confirmation).
- 🌐 **Scan range**: automatic (the phone's own Wi-Fi network) or pick a common router range / enter your own,
  e.g. `192.168.1.x`, `192.168.178.0/24` or `192.168.1.50-120`.
- 👆 **Tap to control**: pick a switch from the list. The app remembers it.
- 💡 **On / Off / Toggle** with live status. Only the button of the current state is highlighted.
- 🕒 **Tasmota time**: shows the switch's clock and warns if it is off
  (for example because of a wrong time zone or daylight saving time).
- 🌅 **Sunrise and sunset** of the active device, plus a **Set** button that stores the phone's location
  as `Latitude` / `Longitude` in Tasmota.
- 📅 **Two schedules**, each with an on and an off moment:
  - at a **fixed time**, or
  - at **sunrise** or **sunset**, with an offset (for example *sunset − 00:30*),
  - and switchable per day of the week.
- 💾 Schedules are stored **in the switch itself** as Tasmota timers 1–4,
  so they keep working when your phone is off or the app is closed.
- ✏️ Change the **FriendlyName** and **DeviceName** of a switch from the app.
- 📶 **Wi-Fi card**: shows the network the phone is connected to; tap it to switch networks.
  There is also a **home screen widget** for this.
- 🔗 Tap the **IP address** at the top to open the device's own Tasmota web page.
- 🔒 Devices with a web password are recognised. Set the password under *Settings*.

## Installing

1. Go to **[Releases](../../releases)** and download `PlafondLED.apk` from the newest build.
   *(Or open the **[Actions](../../actions)** tab, open the newest green run and download `PlafondLED-apk` under **Artifacts**.)*
2. Open the APK on your phone. Android asks once for permission to install *"unknown apps"*.
3. If Google Play Protect warns that the app is unknown, tap **Install anyway**. The warning appears because
   the app is not from the Play Store, not because something wrong was found.

> From version 1.1 the app uses a fixed signing key, so updates install over the old version.
> If you are coming from version 1.0, uninstall that first.

On first start the app asks for location permission. Android requires this to read the Wi-Fi network name
and to set the phone's location in Tasmota. The location is only sent to your own switch.

## One-time Tasmota setup

Open the switch in your browser → **Tools → Console** and enter:

```text
Backlog Timezone 99; TimeStd 0,0,10,1,3,60; TimeDst 0,0,3,1,2,120
Backlog Latitude 52.70; Longitude 5.29
```

- The **first line** sets Central European time with automatic summer/winter time. Without it, the timers switch an hour off.
- The **second line** sets your location, which is needed for sunrise and sunset. Fill in your own coordinates,
  or use the **Set** button in the app.

**Tip:** give your switches a fixed IP address outside your router's DHCP range, so the app can always find them:

```text
Backlog IPAddress1 10.10.100.200; IPAddress2 10.10.100.1; IPAddress3 255.255.255.0; IPAddress4 10.10.100.1; Restart 1
```

## How does it work?

The controls are a regular HTML page running inside the app (an Android WebView).
A web page in a browser is not allowed to read Tasmota's responses because of **CORS**, and the standard Tasmota build does not have the `CORS` command.
This app therefore makes the HTTP requests itself in Java, outside the browser. That way status, time, timers and the scan simply work.

All communication goes through the Tasmota HTTP API, for example:

```text
http://<ip>/cm?cmnd=Power%20TOGGLE
http://<ip>/cm?cmnd=Status
http://<ip>/cm?cmnd=Timer1%20{"Enable":1,"Mode":2,"Time":"-00:30","Days":"1111111","Repeat":1,"Output":1,"Action":1}
```

## Building and customising

The complete Android project is in **`plafond-led-app.zip`**. When the zip is updated, GitHub Actions automatically builds a new APK
(see [`.github/workflows`](.github/workflows)).

| File in the zip | Contents |
|---|---|
| `app/src/main/assets/index.html` | The control page (HTML/CSS/JS) |
| `app/src/main/java/.../MainActivity.java` | WebView, HTTP requests, network scan and location |
| `app/src/main/java/.../WifiWidget.java` | The Wi-Fi home screen widget |
| `app/build.gradle` | Version number and signing |

Want to change something? Unzip, edit the file, zip it again and upload it. GitHub then builds a new APK.

## Using it away from home

The app works on your **own Wi-Fi network**. To use it while away, use a **VPN to your home**,
such as [Tailscale](https://tailscale.com) or WireGuard on your router. Do **not** open ports to your Tasmota devices:
Tasmota has no https and should not be directly connected to the internet.

## Known limitations

- The scan covers one `/24` network at a time (for example `10.10.100.x`).
- Android does not let normal apps switch Wi-Fi by themselves; the Wi-Fi card and widget open Android's Wi-Fi panel instead.
- The app controls relay 1 (`Power`). Devices with multiple relays are not yet shown per channel.
- Tasmota over HTTP only. OpenBeken and ESPHome are not recognised.

---

Made with [claude.ai](https://claude.ai) and built by GitHub Actions.
Tasmota is a project by [Theo Arends](https://github.com/arendst/Tasmota).

---
other better android tasmota apps (Cripple & Pay)
---

tasmoshelf tasmo shelf, 

tasmotrol tasmo trol,
 - https://play.google.com/store/apps/details?id=de.bluetrigger.tasmotrol



tasmotacontrol tasmo control,
 - https://play.google.com/store/apps/details?id=de.grings_software.TasmotaControl

tasmotaremota tasmota remota
 - https://play.google.com/store/apps/details?id=com.robtech.tasmotaremota

tasmobile HTTP tas mobile HTTP, 
 - https://play.google.com/store/apps/details?id=com.robtech.tasremv2

tasmofinder tasmo finder.

homeswitch tasmota control home switch
 - https://play.google.com/store/apps/details?id=com.jamminc.my_tasmota

--

TasmoTastic tasmota control Tasmo Tastic
 - https://play.google.com/store/apps/details?id=com.unbox_digital_print.tasmotastic

---

iotorero atomtech tasmota

<img src="Screenshot_20261017-151453_AliExpress.jpg" width="300" alt="">

https://www.google.com/search?q=aliexpress+iotorero+tasmota+eu+plug

https://www.athom.tech/blank-1/EU-plug


<img src="Screenshot_20261017-165930_Chrome.jpg" width="300" alt="">

 *Tasmota OverKill.*

# Tasmota Scan & Control

**A free Android app to find and control Tasmota switches on your own Wi-Fi.**
No cloud, no account, no ads, no limits. 

Free as is

<img src="https://raw.githubusercontent.com/ldijkman/Tasmota-android-test/refs/heads/main/Screenshots/Android_scan_control_app.png" alt="Tasmota Scan & Control: main screen, tiles and schedules">

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-demo.mp4"><img src="demo/tasmota-demo-poster.jpg" width="260" alt="Watch the demo video: 1 minute, with sound"></a>

- **Watch:** [demo video, 1 minute with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-demo.mp4)
- **Download:** [latest APK from Releases](https://github.com/ldijkman/Tasmota-android-test/releases/latest)
- **Try it in your browser:** [online demo with simulated devices](https://ldijkman.github.io/Tasmota-android-test/)
- **Full feature list:** [Claude_Features.md](Claude_Features.md)
- **Use it away from home:** [Tailscale guide](tailscale.md)

---

## Why this app

- Free, no ads, no fee, use it as you like
- No limits: as many switches, groups, scenes and widgets as you like (only schedules are limited by Tasmota: up to 8 on/off sets per switch)
- No account, no login, no cloud
- No data collection, no tracking, no analytics
- Works locally on your own Wi-Fi; no internet needed after installing
- No MQTT server or Home Assistant needed
- Your settings and devices stay on your phone
- Your location is only sent to your own Tasmota switches, for sunrise and sunset
- Open source: all code is in `plafond-led-app.zip`
- Small (about 170 KB) and starts fast
- Android 7.0 or newer

---

## What it can do (version 17.2)

**Find and control**
- Scans your network and finds Tasmota switches and sockets in about 10 seconds
- On/off with one tap, including devices with several relays (one card per relay)
- Recognises a switch by its MAC address, so it keeps its settings when the router gives it a new IP address
- Works with password-protected devices
- Change the FriendlyName and DeviceName from the app
- Lamps: brightness slider, warm/cool white, colour and presets for dimmable Tasmota lamps

**Organise**
- Groups with a name and colour, with one switch for the whole group
- Drag to change the order or to move switches between groups
- An icon and an "on" colour per switch
- Tiles screen with large buttons
- Scenes: switch several devices at once with one tap
- Choose which details each card shows (IP, MAC, next schedule, energy and more)

**Schedules and timers**
- Up to 8 on/off schedules per switch, at fixed times or at sunrise and sunset with an offset
- Stored in the Tasmota itself, so they keep running when the phone is off
- Timeline preview, and a "Today" view of everything that will switch
- Copy schedules from one switch to another
- Auto-off after a set time (Tasmota PulseTime) with a live countdown, also when switched on with the button on the device

**Energy**
- Power, use today and cost for devices with an energy meter

**Home screen widgets**
- Switch, group, scene, Wi-Fi and Tailscale widgets
- The switch widget counts down when auto-off is running

**Extras**
- Share setup: send groups, icons, colours and scenes to another phone or tablet, or save the file as a backup
- Themes: Auto, Light, Dark, Blue, Black (OLED), Forest and Night warm. The Android bars at the top and bottom take the theme colour
- Wall panel (kiosk) mode: keep the screen on, dim after a few minutes, full screen without Android bars, a "swipe again to exit" message, and the option to make the app the phone's home screen
- Tailscale card and widget to switch your remote connection on and off
- Tips screen that explains the hold and drag gestures
- Updates itself from GitHub with **Check update**

---

## Install

1. Download `PlafondLED.apk` from the [latest release](https://github.com/ldijkman/Tasmota-android-test/releases/latest).
2. Open it on your phone. Android asks once for permission to install apps from this source.
3. If Google Play Protect warns that the app is unknown, tap **Install anyway**. The warning appears because the app is not from the Play Store, not because something is wrong with it.

![Tap Install anyway](Screenshots/toch-installeren-flash.gif)

On first start the app asks for location permission. Android needs this to show the Wi-Fi network name, and the app uses it to send your location to your own switches for sunrise and sunset.

Later versions install from inside the app: **menu → Check update**.

---

## Screenshots

These pictures show how the app grew, from the first version to now. Some show older layouts.

### Scan and find

<img src="Screenshots/Screenshot_20261009-205015.jpg" width="300" alt="Network scan: all Tasmota switches with name and IP">

Network scan: all Tasmota switches with name and IP


<img src="Screenshots/Screenshot_20261009-203512.jpg" width="300" alt="Controls and two schedules with sunset (early version)">

Controls and two schedules with sunset (early version)


<img src="Screenshots/Screenshot_20261010-151334.jpg" width="300" alt="Screenshot">


<img src="Screenshots/Screenshot_20261010-151207.jpg" width="300" alt="Setting for which IP range to scan">

Setting for which IP range to scan


<img src="Screenshots/Screenshot_20261011-082013_Settings.jpg" width="300" alt="Button to switch the phone's Wi-Fi network easily">

Button to switch the phone's Wi-Fi network easily


<img src="Screenshots/Screenshot_20261012-171735.jpg" width="300" alt="Found devices are stored; remove one with ×">

Found devices are stored; remove one with ×


<img src="Screenshots/Screenshot_20261013-165929.jpg" width="300" alt="Switch on and off straight from the scan results">

Switch on and off straight from the scan results



### Device settings

<img src="Screenshots/Screenshot_20261011-202604.jpg" width="300" alt="Sunrise and sunset for the active device, and a link to the device's own web page">

Sunrise and sunset for the active device, and a link to the device's own web page


<img src="Screenshots/Screenshot_20261012-173131.jpg" width="300" alt="Change the FriendlyName and DeviceName from the app">

Change the FriendlyName and DeviceName from the app


<img src="Screenshots/Screenshot_20261012-182930.jpg" width="300" alt="Send the phone's location (lat/lon) to the Tasmota device for sunrise and sunset">

Send the phone's location (lat/lon) to the Tasmota device for sunrise and sunset


<img src="Screenshots/Screenshot_20261012-213333.jpg" width="300" alt="Screenshot">


<img src="Screenshots/Screenshot_20261012-213322.jpg" width="300" alt="Set the time zone">

Set the time zone



### Organise: order, groups and colours

<img src="Screenshots/Screenshot_20261014-164709.jpg" width="300" alt="Drag to change the order; the order is saved">

Drag to change the order; the order is saved


<img src="Screenshots/Screenshot_20261014-165907.jpg" width="300" alt="Drag to order">

Drag to order


<img src="Screenshots/Screenshot_20261015-172317.jpg" width="300" alt="Create groups (labels) and drag switches into them">

Create groups (labels) and drag switches into them


<img src="Screenshots/Screenshot_20261015-192401.jpg" width="300" alt="Fold a group in or out">

Fold a group in or out


<img src="Screenshots/Screenshot_20261019-172020.jpg" width="300" alt="Wi-Fi signal, uptime, edit button and a switch for the whole group">

Wi-Fi signal, uptime, edit button and a switch for the whole group


<img src="Screenshots/Screenshot_20261021-173920.jpg" width="300" alt="Edit a group label to choose a colour">

Edit a group label to choose a colour


<img src="Screenshots/Screenshot_20261021-174241.jpg" width="300" alt="Screenshot">


<img src="Screenshots/Screenshot_20261019-200153.jpg" width="300" alt="About, in the hamburger menu">

About, in the hamburger menu


<img src="Screenshots/Screenshot_20261027-165442_Chrome.jpg" width="300" alt="Choose which details the cards show">

Choose which details the cards show


<img src="Screenshots/Screenshot_20261101-172547.jpg" width="300" alt="Hold a device icon or group name for 2 seconds to edit it">

Hold a device icon or group name for 2 seconds to edit it


<img src="Screenshots/Screenshot_20261113-182952.jpg" width="300" alt="Choose your own on colour per icon">

Choose your own "on" colour per icon


<img src="Screenshots/Screenshot_20261114-080430.jpg" width="300" alt="Themes in the hamburger menu: Auto, Light, Dark, Blue">

Themes in the hamburger menu: Auto, Light, Dark, Blue



### Schedules

<img src="Screenshots/Screenshot_20261019-191637.jpg" width="300" alt="Up to 8 on/off schedules stored in the Tasmota itself, with a timeline preview; floating hamburger menu">

Up to 8 on/off schedules stored in the Tasmota itself, with a timeline preview; floating hamburger menu


<img src="Screenshots/Screenshot_20261024-082456.jpg" width="300" alt="Clock button on each device opens its schedules; copy schedules from another device">

Clock button on each device opens its schedules; copy schedules from another device


<img src="Screenshots/Screenshot_20261028-182639_Chrome.jpg" width="300" alt="Sunrise and sunset shown above the timeline bar">

Sunrise and sunset shown above the timeline bar


<img src="Screenshots/Screenshot_20261031-070958.jpg" width="300" alt="Today: everything that will switch today">

Today: everything that will switch today



### Tiles, icons and scenes

<img src="Screenshots/Screenshot_20261024-190354.jpg" width="300" alt="Tiles screen">

Tiles screen


<img src="Screenshots/Screenshot_20261025-080634.jpg" width="300" alt="Tiles screen, new look">

Tiles screen, new look


<img src="Screenshots/Screenshot_20261025-144023.jpg" width="300" alt="Choose an icon per device">

Choose an icon per device


<img src="Screenshots/Screenshot_20261025-194011.jpg" width="300" alt="Some of the icons">

Some of the icons


<img src="Screenshots/Screenshot_20261028-181040.jpg" width="300" alt="New icons">

New icons


<img src="Screenshots/Screenshot_20261028-181522.jpg" width="300" alt="New icons">

New icons


<img src="Screenshots/Screenshot_20261031-073743.jpg" width="300" alt="More icons">

More icons


<img src="Screenshots/Screenshot_20261029-195221.jpg" width="300" alt="Scenes, and a scene widget">

Scenes, and a scene widget


<img src="Screenshots/Screenshot_20261029-201120.jpg" width="300" alt="Scenes on the Tiles screen; hold a scene to edit it, swipe scenes left and right">

Scenes on the Tiles screen; hold a scene to edit it, swipe scenes left and right


<img src="Screenshots/Screenshot_20261029-202500.jpg" width="300" alt="Change the order of the scenes by dragging">

Change the order of the scenes by dragging


<img src="Screenshots/Screenshot_20261031-071322.jpg" width="300" alt="A scene switches several devices on or off at once">

A scene switches several devices on or off at once



### Energy

<img src="Screenshots/Screenshot_20261028-173105.jpg" width="300" alt="Energy use is shown when a device has a meter">

Energy use is shown when a device has a meter



### Auto-off (PulseTime)

<img src="Screenshots/Screenshot_20261101-113021.jpg" width="300" alt="Auto-off after a set number of minutes">

Auto-off after a set number of minutes


<img src="Screenshots/Screenshot_20261101-113147.jpg" width="300" alt="Auto-off setting">

Auto-off setting


<img src="Screenshots/Screenshot_20261101-113931.jpg" width="300" alt="Bathroom fan on for 2 minutes, counting down on the tiles. Also counts down when switched on with the button on the device">

Bathroom fan on for 2 minutes, counting down on the tiles. Also counts down when switched on with the button on the device


<img src="Screenshots/Screenshot_20261101-194608.jpg" width="300" alt="Hourglass on the icon: this device has auto-off">

Hourglass on the icon: this device has auto-off


<img src="Screenshots/Screenshot_20261103-184226.jpg" width="300" alt="The auto-off setting shown on the card">

The auto-off setting shown on the card



### Devices with several relays

<img src="Screenshots/Screenshot_20261103-182438.jpg" width="300" alt="A separate card for each relay">

A separate card for each relay


<img src="Screenshots/Screenshot_20261103-203033.jpg" width="300" alt="A device with 4 relays: 4 cards">

A device with 4 relays: 4 cards



### Home screen widgets

<img src="Screenshots/Screenshot_20261026-183040_One_UI_Home.jpg" width="300" alt="Widgets: hold the app icon until the menu opens, then choose Widgets">

Widgets: hold the app icon until the menu opens, then choose Widgets


<img src="Screenshots/Screenshot_20261026-182902.jpg" width="300" alt="Adding a switch widget">

Adding a switch widget


<img src="Screenshots/Screenshot_20261026-185117_One_UI_Home.jpg" width="300" alt="Screenshot">


<img src="Screenshots/Screenshot_20261029-202951_One_UI_Home.jpg" width="300" alt="Scene widgets">

Scene widgets


<img src="Screenshots/Screenshot_20261029-210509_Chrome.jpg" width="300" alt="New preview pictures in the widget list">

New preview pictures in the widget list


<img src="Screenshots/Screenshot_20261030-060403_One_UI_Home.jpg" width="300" alt="Widgets for Wi-Fi, scenes, switches and groups">

Widgets for Wi-Fi, scenes, switches and groups


<img src="Screenshots/Screenshot_20261101-120519_One_UI_Home.jpg" width="300" alt="Bathroom fan widget counting down to off">

Bathroom fan widget counting down to off



### Lamps

<img src="Screenshots/brightness-15.0.png" width="600" alt="Brightness slider on the card, percentage on the tiles, and the light window">

Brightness on the card, the percentage on the tiles, and the light window with white, colour and presets (made with test lamps).

<img src="Screenshots/Screenshot_20261114-174300.jpg" width="300" alt="Light window on a real device: brightness, colour and presets">

The light window on a real device: a Sonoff Basic R4 set up in Tasmota as a colour light, with its status LED as the "lamp". Tap the percentage button with the sliders icon to open it.

### Remote control

<img src="Screenshots/Screenshot_20261108-115738_AnyDesk.jpg" width="300" alt="Controlling a phone at home from another phone with AnyDesk. Tailscale turned out more reliable, see the guide below">

Controlling a phone at home from another phone with AnyDesk. Tailscale turned out more reliable, see the guide below


---

## One-time Tasmota setup

Open the switch in your browser → **Tools → Console** and enter:

```text
Backlog Timezone 99; TimeStd 0,0,10,1,3,60; TimeDst 0,0,3,1,2,120
Backlog Latitude 52.70; Longitude 5.29
```

- The **first line** sets Central European time with automatic summer and winter time. Without it, timers can switch an hour off.
- The **second line** sets your location, needed for sunrise and sunset. Fill in your own coordinates, or use the location button in the app.

**Tip:** give your switches a fixed IP address, either in your router (DHCP reservation) or in Tasmota itself:

```text
Backlog IPAddress1 10.10.100.200; IPAddress2 10.10.100.1; IPAddress3 255.255.255.0; IPAddress4 10.10.100.1; Restart 1
```

Since version 14.2 the app also follows a switch by its MAC address when its IP address changes.

---

## Use it away from home

The app works on your own Wi-Fi. To use it while away, connect your phone to your home network with a VPN.

**[Step-by-step guide: remote control with Tailscale](tailscale.md)**: a spare Android phone that stays at home passes the traffic on. No changes to your modem are needed. Tested on a Ziggo connection.

With the Tailscale app installed, a **Tailscale card** appears under the Wi-Fi card to switch the connection on and off.

<img src="Screenshots/Screenshot_20261115-064307.jpg" width="300" alt="The app away from home: Tailscale card switched on, scan range set to the home range 10.10.100.x">

> [!CAUTION]
> **Scanning away from home:** the app scans the network your phone is on, not your home network.
> Before you tap **Scan network**, tap the range next to *Tasmota switches* and set it to **your home range**, for example **10.10.100.x**.
> Otherwise the scan searches the wrong network and finds nothing.

Do **not** open ports on your router to your Tasmota devices. Tasmota has no HTTPS and should not be directly reachable from the internet.

---

## Wall panel (kiosk mode)

Put a spare phone or tablet on the wall as a fixed control panel. Everything is in the hamburger menu:

| Setting | What it does |
|---|---|
| **Theme** | Pick **Black (OLED)**: on an OLED screen black pixels are off, which saves power and gives no glow in a dark room. |
| **Keep screen on** | The screen stays on while the app is open. Leave the app and the phone's normal timeout applies again. |
| **Dim after** | Dims the screen after 1, 2, 5, 10 or 30 minutes without a touch (or never). |
| **Dim to** | How dark it gets: 5%, 10%, 20%, 30% or 50%. Picking a value shows a 2-second preview. |
| **Full screen** | Hides the Android status bar and the 3-button bar. Swipe in from the top or bottom edge to see them for a moment. |
| **Home screen app** | Makes the app the phone's home screen: the **Home** button always brings you back to it. Tap the switch, then pick *Tasmota Scan & Control* in the Android screen that opens. To undo, tap it again and pick your old home app. |
| **Back / swipe to exit** | The first Back (or back swipe from the side) shows *Swipe or press Back again to exit*. Only a second one within 3 seconds closes the app. When the app is the home screen, Back never leaves it. |

The first tap on a dimmed screen only wakes it up. It does not switch anything, so you cannot switch a light by accident.

> [!TIP]
> **A wall panel is also a good Tailscale server.** It stays at home, on the charger and awake, so you can reach your switches from outside. See the [Tailscale guide](tailscale.md).

Good to know:
- Keep the phone on the charger. To protect the battery, see the charger tips in the [Tailscale guide](tailscale.md).
- For a panel that nobody may leave, Android's own **App pinning** can help (search for "pin" in Settings). Leaving then needs Back and Recents held together, optionally with your PIN.
- OLED screens can slowly burn in when the same image stays lit for months. Dimming and the Black theme reduce that.

---

## How it works

The screens are a normal HTML page running inside the app (an Android WebView).
A web page in a browser may not read Tasmota's replies because of **CORS**, and the standard Tasmota build has no `CORS` command.
So the app makes the HTTP requests itself in Java, outside the browser.

Everything goes through the Tasmota HTTP API, for example:

```text
http://<ip>/cm?cmnd=Power%20TOGGLE
http://<ip>/cm?cmnd=Status
http://<ip>/cm?cmnd=Timer1%20{"Enable":1,"Mode":2,"Time":"-00:30","Days":"1111111","Repeat":1,"Output":1,"Action":1}
```

## Build it yourself

The complete Android project is in **`plafond-led-app.zip`**. When the zip changes, GitHub Actions builds a new APK and release
(see [`.github/workflows`](.github/workflows)).

| File in the zip | Contents |
|---|---|
| `app/src/main/assets/index.html` | All screens (HTML, CSS and JavaScript) |
| `app/src/main/java/.../MainActivity.java` | WebView, HTTP requests, network scan, location, sharing and updates |
| `app/src/main/java/.../DeviceWidget.java`, `GroupWidget.java`, `SceneWidget.java`, `WifiWidget.java`, `TailscaleWidget.java` | The home screen widgets |
| `app/build.gradle` | Version number and signing |

To change something: unzip, edit, zip again and upload. GitHub then builds a new APK.

## Known limitations

- The scan covers one `/24` network at a time (for example `10.10.100.x`).
- Android does not let normal apps switch Wi-Fi by themselves; the Wi-Fi card and widget open Android's Wi-Fi panel instead.
- Made for Tasmota over HTTP. ESPHome is not supported.
- **OpenBeken may partly work** (not tested). OpenBeken accepts many of the same commands as Tasmota, so scanning, on/off, groups, scenes, tiles and widgets will probably work. **Schedules do not work on OpenBeken**: it has its own schedule system instead of Tasmota's timers. Auto-off, energy, sunrise/sunset and lamp controls may also not work.
- Lamp controls are new. Brightness and colour were tested on a Sonoff Basic R4 set up as a colour light, not yet on a real bulb or LED strip; warm/cool white has not been tested. Lamps on relay 2 or higher get no lamp controls.

---

## Hardware

Plugs that come with Tasmota already installed, for example from Athom, or *iotorero* plugs on AliExpress:

<img src="Screenshots/Screenshot_20261017-151453_AliExpress.jpg" width="300" alt="iotorero Tasmota EU plug on AliExpress">

<img src="Screenshots/Screenshot_20261017-165930_Chrome.jpg" width="300" alt="Athom EU plug">

- https://www.athom.tech/blank-1/EU-plug
- https://www.google.com/search?q=aliexpress+iotorero+tasmota+eu+plug

---

## Other Android Tasmota apps

Other apps, some of them paid or with limits in the free version:

- TasmoShelf
- [Tasmotrol](https://play.google.com/store/apps/details?id=de.bluetrigger.tasmotrol)
- [TasmotaControl](https://play.google.com/store/apps/details?id=de.grings_software.TasmotaControl)
- [Tasmota Remota](https://play.google.com/store/apps/details?id=com.robtech.tasmotaremota)
- [TasMobile HTTP](https://play.google.com/store/apps/details?id=com.robtech.tasremv2)
- TasmoFinder
- [Home Switch (Tasmota control)](https://play.google.com/store/apps/details?id=com.jamminc.my_tasmota)
- [TasmoTastic](https://play.google.com/store/apps/details?id=com.unbox_digital_print.tasmotastic)
- [TasmoHub](https://github.com/adelomer12/tasmo-hub): free and open source, for Android and iPhone, also supports OpenBeken

---

Made with [claude.ai](https://claude.ai) (Grok can also build the APK from the zip) and built by GitHub Actions.
Tasmota is a project by [Theo Arends](https://github.com/arendst/Tasmota).

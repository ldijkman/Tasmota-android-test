 *Tasmota OverKill.*

# Tasmota Scan & Control

**A free Android app to find and control Tasmota switches on your own Wi-Fi.**
No cloud, no account, no ads, no limits. 

Free as is


website
 - https://ldijkman.github.io/Tasmota-android-test/

<img src="https://raw.githubusercontent.com/ldijkman/Tasmota-android-test/refs/heads/main/Screenshots/tasmota-scan-control-android-app.png" alt="Tasmota Scan & Control: main screen, tiles and schedules">

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-demo.mp4"><img src="demo/tasmota-demo-poster.jpg" width="260" alt="Watch the demo video: 1 minute, with sound"></a>

- **Watch:** [demo video, 1 minute with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-demo.mp4)
- **Download:** [latest APK from Releases](https://github.com/ldijkman/Tasmota-android-test/releases/latest)
- **Website:** [ldijkman.github.io/Tasmota-android-test](https://ldijkman.github.io/Tasmota-android-test/)
- **Try it in your browser:** [online demo with simulated devices](https://ldijkman.github.io/Tasmota-android-test/demo/)
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

## What it can do (version 21.0)

**Find and control**
- Scans your network and finds Tasmota switches and sockets in about 10 seconds
- On/off with one tap, including devices with several relays (one card per relay)
- Recognises a switch by its MAC address, so it keeps its settings when the router gives it a new IP address
- Works with password-protected devices
- Change the FriendlyName and DeviceName from the app
- Lamps: brightness slider, warm/cool white, colour and presets for dimmable Tasmota lamps

**Organise**
- Groups with a name and colour, with one switch for the whole group ([video](https://ldijkman.github.io/Tasmota-android-test/tasmota-groups.mp4))
- Drag to change the order or to move switches between groups; drag a whole group to change the order of the groups (the group ⋮⋮ can be hidden in "Show details" options)
- An icon and an "on" colour per switch, from the built-in icons (now with farm and pet animals: chicken, rooster, horse, cow, dog, cat, rabbit, duck and more) or 7,000+ icons online ([video](https://ldijkman.github.io/Tasmota-android-test/tasmota-icons.mp4))
- Tiles screen with large buttons
- Scenes: switch several devices at once with one tap ([video](https://ldijkman.github.io/Tasmota-android-test/tasmota-scenes.mp4))
- Choose which details each card shows (IP, MAC, next schedule, energy, sensors, last restart reason and more)
- Tap the IP address on a card to open the device's own Tasmota web page
- Open the app from the website with the "Open the app" button (Android), or with links like `tasmotascan://scenes` and `tasmotascan://tiles`

**Schedules and timers**
- Up to 8 on/off schedules per switch, at fixed times or at sunrise and sunset with an offset
- Stored in the Tasmota itself, so they keep running when the phone is off
- Timeline preview, and a "Today" view of everything that will switch
- Copy schedules from one switch to another
- **Watch:** [video: timers with sunrise and sunset, 1 minute with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-timers.mp4)
- Auto-off after a set time (Tasmota PulseTime) with a live countdown, also when switched on with the button on the device

**Energy**
- Power, use today and cost for devices with an energy meter

**Sensors**
- Temperature, humidity, air pressure, light, CO2 and distance from sensors on a Tasmota device (for example AM2301, DS18B20, BME280), shown on the card and in Tiles view

**Home screen widgets**
- Switch, group, scene, Wi-Fi and Tailscale widgets
- The switch widget counts down when auto-off is running

**Extras**
- Share setup: send groups, icons, colours and scenes to another phone or tablet, or save the file as a backup
- Add a brand-new or reset Tasmota device from the menu: the app gives it your home Wi-Fi and finds it (Android 10+)
- Themes: Auto, Light, Dark, Blue, Black (OLED), Forest and Night warm. The Android bars at the top and bottom take the theme colour
- Wall panel (kiosk) mode: keep the screen on, dim after a few minutes, full screen without Android bars, a "swipe again to exit" message, and the option to make the app the phone's home screen ([video](https://ldijkman.github.io/Tasmota-android-test/tasmota-wall-panel.mp4))
- Tailscale card and widget to switch your remote connection on and off
- Tips screen that explains the hold and drag gestures
- Updates itself from GitHub with **Check update**

---

## Install

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-install.mp4"><img src="demo/tasmota-install-poster.jpg" width="260" alt="Watch the video: install the app and do the first scan, 1.5 minutes with sound"></a>

**Watch:** [install the app, Play Protect, and the first scan, 1.5 minutes with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-install.mp4)

1. Download `PlafondLED.apk` from the [latest release](https://github.com/ldijkman/Tasmota-android-test/releases/latest).
2. Open it on your phone. Android asks once for permission to install apps from this source.
3. If Google Play Protect warns that the app is unknown, tap **Install anyway**. The warning appears because the app is not from the Play Store, not because something is wrong with it.

![Tap Install anyway](Screenshots/tasmota-play-protect-install-anyway.gif)

On first start the app asks for location permission. Android needs this to show the Wi-Fi network name, and the app uses it to send your location to your own switches for sunrise and sunset.

Later versions install from inside the app: scroll to the bottom of the main screen and tap **Check for update**.

---

## Screenshots

These pictures show how the app grew, from the first version to now. Some show older layouts.

### Scan and find

<img src="Screenshots/tasmota-network-scan-results.jpg" width="300" alt="Network scan: all Tasmota switches with name and IP">

Network scan: all Tasmota switches with name and IP


<img src="Screenshots/tasmota-controls-sunset-schedules-early.jpg" width="300" alt="Controls and two schedules with sunset (early version)">

Controls and two schedules with sunset (early version)


<img src="Screenshots/tasmota-scan-range-by-router.jpg" width="300" alt="Choose the IP range to scan from a list of common routers: KPN, Ziggo, FRITZ!Box, TP-Link, ASUS and more">


<img src="Screenshots/tasmota-scan-ip-range-setting.jpg" width="300" alt="Setting for which IP range to scan">

Setting for which IP range to scan


<img src="Screenshots/tasmota-switch-phone-wifi-network.jpg" width="300" alt="Button to switch the phone's Wi-Fi network easily">

Button to switch the phone's Wi-Fi network easily


<img src="Screenshots/tasmota-saved-devices-remove.jpg" width="300" alt="Found devices are stored; remove one with ×">

Found devices are stored; remove one with ×


<img src="Screenshots/tasmota-switch-from-scan-results.jpg" width="300" alt="Switch on and off straight from the scan results">

Switch on and off straight from the scan results



### Device settings

<img src="Screenshots/tasmota-sunrise-sunset-device-webpage.jpg" width="300" alt="Sunrise and sunset for the active device, and a link to the device's own web page">

Sunrise and sunset for the active device, and a link to the device's own web page


<img src="Screenshots/tasmota-friendlyname-devicename.jpg" width="300" alt="Change the FriendlyName and DeviceName from the app">

Change the FriendlyName and DeviceName from the app


<img src="Screenshots/tasmota-send-location-sunrise-sunset.jpg" width="300" alt="Send the phone's location (lat/lon) to the Tasmota device for sunrise and sunset">

Send the phone's location (lat/lon) to the Tasmota device for sunrise and sunset


<img src="Screenshots/tasmota-time-zone-list.jpg" width="300" alt="Time zone list: Central Europe, UK, Eastern Europe, US and Australia">


<img src="Screenshots/tasmota-set-time-zone.jpg" width="300" alt="Set the time zone">

Set the time zone



### Organise: order, groups and colours

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-groups.mp4"><img src="demo/tasmota-groups-poster.jpg" width="260" alt="Watch the video: groups, dragging switches, show details, and removing a switch, 1.5 minutes with sound"></a>

**Watch:** [make a group, drag switches into it, choose what the cards show, and remove a switch, 1.5 minutes with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-groups.mp4)

<img src="Screenshots/tasmota-drag-to-reorder-switches.jpg" width="300" alt="Drag to change the order; the order is saved">

Drag to change the order; the order is saved


<img src="Screenshots/tasmota-drag-to-order.jpg" width="300" alt="Drag to order">

Drag to order


<img src="Screenshots/tasmota-create-groups.jpg" width="300" alt="Create groups (labels) and drag switches into them">

Create groups (labels) and drag switches into them


<img src="Screenshots/tasmota-fold-group.jpg" width="300" alt="Fold a group in or out">

Fold a group in or out


<img src="Screenshots/tasmota-wifi-signal-uptime-group-switch.jpg" width="300" alt="Wi-Fi signal, uptime, edit button and a switch for the whole group">

Wi-Fi signal, uptime, edit button and a switch for the whole group


<img src="Screenshots/tasmota-group-label-colour.jpg" width="300" alt="Edit a group label to choose a colour">

Edit a group label to choose a colour


<img src="Screenshots/tasmota-coloured-groups-details.jpg" width="300" alt="Coloured groups with switches, IP address, Wi-Fi signal, uptime and next schedule">


<img src="Screenshots/tasmota-about-hamburger-menu.jpg" width="300" alt="About, in the hamburger menu">

About, in the hamburger menu


<img src="Screenshots/tasmota-card-details-choice.jpg" width="300" alt="Choose which details the cards show">

Choose which details the cards show


<img src="Screenshots/tasmota-hold-to-edit.jpg" width="300" alt="Hold a device icon or group name for 2 seconds to edit it">

Hold a device icon or group name for 2 seconds to edit it


<img src="Screenshots/tasmota-own-on-colour-per-icon.jpg" width="300" alt="Choose your own on colour per icon">

Choose your own "on" colour per icon


<img src="Screenshots/tasmota-themes-menu.jpg" width="300" alt="Themes in the hamburger menu: Auto, Light, Dark, Blue">

Themes in the hamburger menu: Auto, Light, Dark, Blue



### Schedules

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-timers.mp4"><img src="demo/tasmota-timers-poster.jpg" width="260" alt="Watch the video: timers with sunrise and sunset, 1 minute with sound"></a>

**Watch:** [how to set timers with sunrise and sunset, 1 minute with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-timers.mp4)

<img src="Screenshots/tasmota-8-schedules-timeline.jpg" width="300" alt="Up to 8 on/off schedules stored in the Tasmota itself, with a timeline preview; floating hamburger menu">

Up to 8 on/off schedules stored in the Tasmota itself, with a timeline preview; floating hamburger menu


<img src="Screenshots/tasmota-schedules-clock-copy.jpg" width="300" alt="Clock button on each device opens its schedules; copy schedules from another device">

Clock button on each device opens its schedules; copy schedules from another device


<img src="Screenshots/tasmota-sunrise-sunset-timeline.jpg" width="300" alt="Sunrise and sunset shown above the timeline bar">

Sunrise and sunset shown above the timeline bar


<img src="Screenshots/tasmota-today-overview.jpg" width="300" alt="Today: everything that will switch today">

Today: everything that will switch today



### Tiles, icons and scenes

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-icons.mp4"><img src="demo/tasmota-icons-poster.jpg" width="260" alt="Watch the video: change icon and colour, 1 minute with sound"></a>

**Watch:** [how to change a switch's icon and on colour, 1 minute with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-icons.mp4)

<img src="Screenshots/tasmota-tiles-screen.jpg" width="300" alt="Tiles screen">

Tiles screen


<img src="Screenshots/tasmota-tiles-screen-new-look.jpg" width="300" alt="Tiles screen, new look">

Tiles screen, new look


<img src="Screenshots/tasmota-choose-icon.jpg" width="300" alt="Choose an icon per device">

Choose an icon per device


<img src="Screenshots/tasmota-icon-set.jpg" width="300" alt="Some of the icons">

Some of the icons


<img src="Screenshots/tasmota-new-icons-1.jpg" width="300" alt="New icons">

New icons


<img src="Screenshots/tasmota-new-icons-2.jpg" width="300" alt="New icons">

New icons


<img src="Screenshots/tasmota-more-icons.jpg" width="300" alt="More icons">

More icons


<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-scenes.mp4"><img src="demo/tasmota-scenes-poster.jpg" width="260" alt="Watch the video: make a scene, 1 minute with sound"></a>

**Watch:** [how to make and run a scene, 1 minute with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-scenes.mp4)

<img src="Screenshots/tasmota-scenes-and-scene-widget.jpg" width="300" alt="Scenes, and a scene widget">

Scenes, and a scene widget


<img src="Screenshots/tasmota-scenes-on-tiles.jpg" width="300" alt="Scenes on the Tiles screen; hold a scene to edit it, swipe scenes left and right">

Scenes on the Tiles screen; hold a scene to edit it, swipe scenes left and right


<img src="Screenshots/tasmota-scenes-order.jpg" width="300" alt="Change the order of the scenes by dragging">

Give each scene its own colours for the icon, the name and the border, from the swatches or with the colour sliders (also on the scene widget). Arrange the scenes: tap ✥ Arrange on the Scenes screen and drag each scene to any spot on the 2-wide grid. Empty spaces are allowed, so you can keep day scenes left and night scenes right


<img src="Screenshots/tasmota-scene-editor.jpg" width="300" alt="A scene switches several devices on or off at once">

A scene switches several devices on or off at once



### Energy

<img src="Screenshots/tasmota-energy-use.jpg" width="300" alt="Energy use is shown when a device has a meter">

Energy use is shown when a device has a meter



### Auto-off (PulseTime)

<img src="Screenshots/tasmota-auto-off-minutes.jpg" width="300" alt="Auto-off after a set number of minutes">

Auto-off after a set number of minutes


<img src="Screenshots/tasmota-auto-off-setting.jpg" width="300" alt="Auto-off setting">

Auto-off setting


<img src="Screenshots/tasmota-auto-off-countdown-tiles.jpg" width="300" alt="Bathroom fan on for 2 minutes, counting down on the tiles. Also counts down when switched on with the button on the device">

Bathroom fan on for 2 minutes, counting down on the tiles. Also counts down when switched on with the button on the device


<img src="Screenshots/tasmota-auto-off-hourglass-icon.jpg" width="300" alt="Hourglass on the icon: this device has auto-off">

Hourglass on the icon: this device has auto-off


<img src="Screenshots/tasmota-auto-off-on-card.jpg" width="300" alt="The auto-off setting shown on the card">

The auto-off setting shown on the card



### Devices with several relays

<img src="Screenshots/tasmota-card-per-relay.jpg" width="300" alt="A separate card for each relay">

A separate card for each relay


<img src="Screenshots/tasmota-4-relay-device.jpg" width="300" alt="A device with 4 relays: 4 cards">

A device with 4 relays: 4 cards



### Home screen widgets

<img src="Screenshots/tasmota-widgets-open-menu.jpg" width="300" alt="Widgets: hold the app icon until the menu opens, then choose Widgets">

Widgets: hold the app icon until the menu opens, then choose Widgets


<img src="Screenshots/tasmota-add-switch-widget.jpg" width="300" alt="Adding a switch widget">

Adding a switch widget


<img src="Screenshots/tasmota-home-screen-widgets.jpg" width="300" alt="Android home screen with a Wi-Fi widget and switch widgets">


<img src="Screenshots/tasmota-scene-widgets.jpg" width="300" alt="Scene widgets">

Scene widgets


<img src="Screenshots/tasmota-widget-previews.jpg" width="300" alt="New preview pictures in the widget list">

New preview pictures in the widget list


<img src="Screenshots/tasmota-widgets-wifi-scenes-switches-groups.jpg" width="300" alt="Widgets for Wi-Fi, scenes, switches and groups">

Widgets for Wi-Fi, scenes, switches and groups


<img src="Screenshots/tasmota-widget-auto-off-countdown.jpg" width="300" alt="Bathroom fan widget counting down to off">

Bathroom fan widget counting down to off



### Lamps

<img src="Screenshots/tasmota-brightness-dimmer-lamp.png" width="600" alt="Brightness slider on the card, percentage on the tiles, and the light window">

Brightness on the card, the percentage on the tiles, and the light window with white, colour and presets (made with test lamps).

<img src="Screenshots/tasmota-light-window-colour-brightness.jpg" width="300" alt="Light window on a real device: brightness, colour and presets">

The light window on a real device: a Sonoff Basic R4 set up in Tasmota as a colour light, with its status LED as the "lamp". Tap the percentage button with the sliders icon to open it.

### Remote control

<img src="Screenshots/tasmota-remote-control-anydesk.jpg" width="300" alt="Controlling a phone at home from another phone with AnyDesk. Tailscale turned out more reliable, see the guide below">

Controlling a phone at home from another phone with AnyDesk. Tailscale turned out more reliable, see the guide below


---

## Add a new device

Set up a brand-new or reset Tasmota device straight from the app, without the browser and without 192.168.4.1.

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-add-device.mp4"><img src="demo/tasmota-add-device-poster.jpg" width="260" alt="Watch the video: add a new device, 1 minute with sound"></a>

**Watch:** [video of the whole procedure, 1 minute with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-add-device.mp4)

1. Plug in the new device. A new or reset Tasmota device starts its own Wi-Fi network called **tasmota-…**
2. In the app: hamburger menu → **Add new device**. The app lists the nearby tasmota-… networks.
3. Pick the device. Your current Wi-Fi is selected; tap it to choose another nearby network (for example a separate network for smart devices), or **Other network…** for a hidden one. Enter its password, and optionally a name (for example *Garden lights*).
4. Tap **Connect device**, then tap **Connect** when Android asks to join the tasmota-… network.
5. The app sends your Wi-Fi details, your phone goes back to your own Wi-Fi, and the device restarts and joins it.
6. The app finds the device on your network and sets its name and location (for sunrise and sunset). Then try it with the **on/off switch** in the window, or pick an icon, colour and group.

> [!TIP]
> **Device not in the list?** Press its button 6 times quickly. Tasmota opens its own tasmota-… network again and keeps its settings. Wait about 30 seconds, then tap **Search again**.

> [!CAUTION]
> **Last resort: hold the button for 40 seconds.** This is a full reset to the firmware defaults. The device loses its name, timers and settings, and usually also its **template** (GPIO setup): with standard Tasmota it then works as a plain Sonoff Basic until you set the template again.
>
> **Before you do this,** open the device's web page, go to **Console**, type `Template` and copy the answer. After the reset, paste it back with `Template <the copied text>` followed by `Module 0`.
>
> If the device was already in your list, choose **Keep** afterwards: the app gives it its name back. Timers you need to set again.

Good to know:
- Before you start: phone on the Wi-Fi the device should join, **mobile data off**, and **Tailscale off**. The app shows this as a short checklist.
- Tasmota only works on **2.4 GHz** Wi-Fi. The app warns you if your network looks 5 GHz only.
- Needs Android 10 or newer.
- If the automatic connection fails, the app offers **Connect by hand**: connect to the tasmota-… network in Android's Wi-Fi settings yourself, and the app does the rest.
- **Reset a device that is already in your list?** The app recognises it by its MAC address (part of it is in the tasmota-… name) and shows *already in your list*. After setup you choose: **Keep** the existing card (same name, icon, group and scenes), or **Add as new device** (a fresh card with the new name; it is taken out of groups and scenes).
- Wrong Wi-Fi password? The device opens its own tasmota-… network again after a minute; run **Add new device** once more.

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

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-tailscale.mp4"><img src="demo/tasmota-tailscale-poster.jpg" width="260" alt="Watch the video: remote control with Tailscale, 2 minutes with sound"></a>

**Watch:** [video: remote control with Tailscale, 2 minutes with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-tailscale.mp4)

**[Step-by-step guide: remote control with Tailscale](tailscale.md)**: a spare Android phone that stays at home passes the traffic on. No changes to your modem are needed. Tested on a Ziggo connection.

With the Tailscale app installed, a **Tailscale card** appears under the Wi-Fi card to switch the connection on and off.

<img src="Screenshots/tasmota-tailscale-away-from-home.jpg" width="300" alt="The app away from home: Tailscale card switched on, scan range set to the home range 10.10.100.x">

> [!CAUTION]
> **Scanning away from home:** the app scans the network your phone is on, not your home network.
> Before you tap **Scan network**, tap the range next to *Tasmota switches* and set it to **your home range**, for example **10.10.100.x**.
> Otherwise the scan searches the wrong network and finds nothing.

Do **not** open ports on your router to your Tasmota devices. Tasmota has no HTTPS and should not be directly reachable from the internet.

---

## Wall panel (kiosk mode)

<a href="https://ldijkman.github.io/Tasmota-android-test/tasmota-wall-panel.mp4"><img src="demo/tasmota-wall-panel-poster.jpg" width="260" alt="Watch the video: an old phone or tablet as a wall panel, and as Tailscale VPN server, 1.5 minutes with sound"></a>

**Watch:** [an old phone or tablet as a wall panel, and as Tailscale VPN server, 1.5 minutes with sound](https://ldijkman.github.io/Tasmota-android-test/tasmota-wall-panel.mp4)

Put a spare phone or tablet on the wall as a fixed control panel. Any old Android phone or tablet (Android 7 or newer) will do: it only needs your Wi-Fi, no SIM card. Everything is in the hamburger menu:

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

<img src="Screenshots/tasmota-iotorero-eu-plug.jpg" width="300" alt="iotorero Tasmota EU plug on AliExpress">

<img src="Screenshots/tasmota-athom-eu-plug.jpg" width="300" alt="Athom EU plug">

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

Animal icons: some come from [Game-icons.net](https://game-icons.net) by Lorc and Delapouite (CC BY 3.0) and [Healthicons](https://healthicons.org) (MIT); the rest are [Material Design Icons](https://pictogrammers.com) (Apache 2.0).

## License

Tasmota Scan & Control is free software, licensed under the [GNU General Public License v3.0](LICENSE) (GPL-3.0).
You may use, study, share and change it. If you share a changed version, it must stay open source under the same license.

Made by Dirk Lubèrth Dijkman, together with Claude Opus 5.5.

# Tasmota Scan & Control: what the app can do

A free, open-source Android app for finding and controlling Tasmota Wi-Fi switches
(smart plugs, sockets and relays running the Tasmota firmware).

This overview describes version 17.3.

## In short

- **Fully local.** No cloud, no account, no MQTT and no Home Assistant needed. The app talks directly to the devices on your own Wi-Fi.
- **No ads, no tracking, no data collection.** Your location is only sent to your own Tasmota devices, so they can calculate sunrise and sunset.
- **Small.** The APK is about 165 KB.
- **Android 7.0 and newer.**
- **Open source.** The full source is in `plafond-led-app.zip` in this repository, and GitHub Actions builds the APK from it.

## Find and control

- **Network scan:** finds every Tasmota device on the Wi-Fi network and adds it to the list.
- **On and off:** one tap on the icon, with live status.
- **Several relays:** a device with more than one relay shows each relay as its own switch.
- **MAC recognition:** a switch is recognised by its MAC address. If the router gives it a new IP address, a scan moves its name, group, icon, colour, scenes and widgets to the new address.
- **Passwords:** works with devices that have a web password, with one password for all or one per switch.
- **Web interface:** a link opens the device's own Tasmota page.

## Organise

- **Names:** edit the friendly name and the device name from the app.
- **Groups:** give a group a name and a colour, fold it in or out, and switch the whole group with one tap.
- **Order:** drag switches to sort them or to move them to another group.
- **Icons:** choose an icon per switch, including icons downloaded from Material Design Icons.
- **On colour:** choose the colour a switch shows when it is on, from a palette or with a colour picker.
- **Show or hide details:** choose which lines each switch shows: device name, IP address, MAC address, next schedule, auto-off, energy, time and sun times, Wi-Fi signal and uptime.

## Lamps

For dimmable Tasmota lamps (new in 15.0; brightness and colour tested on a Sonoff Basic R4 set up as a colour light):

- **Brightness slider** on the card, and the percentage on the tiles.
- **Light window:** tap the percentage button (sliders icon) for brightness, warm/cool white, colour and presets (Bright, Reading, Evening, Night).
- Only the controls a lamp supports are shown; plugs and switches stay on/off.

## Tiles and scenes

- **Tiles screen:** large buttons per group, made for quick switching.
- **Scenes:** set several switches on or off with one tap, for example "Evening" or "All off". A scene can also pause or resume schedules.
- **Quick buttons:** Tiles and Scenes buttons stay at the top of the main screen while you scroll.

## Schedules and timers

- **Schedules:** up to 8 on/off schedules per switch, with a choice of weekdays.
- **Stored on the device:** schedules live in the Tasmota itself, so they keep running when the phone is off or away.
- **Sunrise and sunset:** use them as switch times, with an offset.
- **Today screen:** a timeline of everything that will switch today, across all switches.
- **Next action:** each switch can show what it will do next, such as "next: on at 17:13".
- **Copy schedules:** copy the schedules of one switch to another.
- **Auto-off:** a switch can turn itself off after a set time (Tasmota PulseTime), from seconds up to 18 hours. A ring and a countdown show the time left.
- **Clock:** shows the device time and can set the time zone and location for correct sun times.

## Energy

For switches with an energy meter:

- current power
- use today, yesterday and in total
- cost, based on a price per kWh that you set.

## Home screen widgets

- **Switch widget:** on/off with status, and a countdown when auto-off is running.
- **Group widget:** switches a whole group.
- **Scene widget:** runs a scene.
- **Wi-Fi widget:** shows the Wi-Fi network and opens the Wi-Fi settings.

## Outside your home

- **Tailscale card:** when the Tailscale app is installed, a card under the Wi-Fi card shows whether Tailscale is on and switches it on or off.
- **Guide:** [remote control with Tailscale](tailscale.md) explains the setup with a spare Android phone at home.
- **Tailscale widget:** switch Tailscale on or off from the home screen.

## Add a new device

Menu → **Add new device** sets up a brand-new or reset Tasmota device without the browser:

1. The app lists nearby **tasmota-…** Wi-Fi networks (a new or reset device makes its own).
2. Pick one, enter your home Wi-Fi and password, and optionally a name. The app warns when a network is 5 GHz only, because Tasmota needs 2.4 GHz.
3. The phone connects to the device for a moment (Android asks you to confirm), sends your Wi-Fi details, and goes back to your own Wi-Fi.
4. The app finds the device on your network, sets its name and location (for sunrise and sunset), and offers a quick on/off test.

Needs Android 10 or newer; on older phones the app explains how to do it by hand.

## Wall panel (kiosk mode)

Turn a spare phone or tablet into a fixed control panel on the wall. All settings are in the hamburger menu.

- **Keep screen on:** the screen stays lit while the app is open.
- **Dim after:** dims after 1, 2, 5, 10 or 30 minutes without a touch, or never.
- **Dim to:** how dark it gets, from 5% to 50%, with a 2-second preview.
- **Tap to wake:** the first tap on a dimmed screen only wakes it up, so no light is switched by accident.
- **Full screen:** hides the Android status bar and the button bar.
- **Home screen app:** makes the app the phone's home screen, so the Home button always brings you back to it. Easy to undo.
- **Back to exit:** the first Back shows *Swipe or press Back again to exit*; only a second one closes the app.
- **Good Tailscale server:** a wall panel stays at home, on the charger and awake, which is exactly what the Tailscale setup needs.

## Share and back up

- **Share setup:** send switches, groups, scenes, icons, colours and display options to another phone or tablet with the Android share screen.
- **Save as file:** keep the same file as a backup.
- **Import:** merge the file with what is already there, or replace it.
- **Passwords** are only included when you tick that option.

## Look and feel

- **Themes:** Auto (follows the phone), Light, Dark, Blue, Black (OLED), Forest and Night warm, picked with colour swatches in the menu.
- **Black (OLED):** pure black, so OLED screens use almost no power and do not glow in a dark room.
- **Night warm:** no blue light, for a bedroom or bedside table.
- **Android bars** at the top and bottom take the colour of the theme.
- **Tips screen:** explains the gestures that are easy to miss, such as holding an icon for two seconds to edit a switch.

## Updates

- **Check for update** (bottom of the main screen): the app looks at the GitHub releases, then downloads and installs a newer version from inside the app.
- **Play Protect:** while Google Play Protect checks the update, a message with a counter explains what happens and which choices Android will offer (*Scan app* or *Install without scanning*).

## Download

- Latest APK: [Releases](https://github.com/ldijkman/Tasmota-android-test/releases/latest)
- Browser demo: <https://ldijkman.github.io/Tasmota-android-test/demo/>: try the app with pretend switches, including one with a web password (password `demo`). **Reset demo** starts it fresh.

## About

The app was made together by ldijkman and Claude: ldijkman came up with the ideas and tested everything on real switches and phones, Claude wrote much of the code. It is built automatically by GitHub Actions.

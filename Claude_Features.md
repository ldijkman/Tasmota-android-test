# Tasmota Scan & Control: what the app can do

A free, open-source Android app for finding and controlling Tasmota Wi-Fi switches
(smart plugs, sockets and relays running the Tasmota firmware).

This overview describes version 14.5.

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

## Share and back up

- **Share setup:** send switches, groups, scenes, icons, colours and display options to another phone or tablet with the Android share screen.
- **Save as file:** keep the same file as a backup.
- **Import:** merge the file with what is already there, or replace it.
- **Passwords** are only included when you tick that option.

## Look and feel

- **Themes:** Auto (follows the phone), Light, Dark and Blue.
- **Tips screen:** explains the gestures that are easy to miss, such as holding an icon for two seconds to edit a switch.

## Updates

- **Check update:** the app looks at the GitHub releases, then downloads and installs a newer version from inside the app.

## Download

- Latest APK: [Releases](https://github.com/ldijkman/Tasmota-android-test/releases/latest)
- Browser demo: <https://ldijkman.github.io/Tasmota-android-test/>

## About

The app was made with help from Claude and is built automatically by GitHub Actions.

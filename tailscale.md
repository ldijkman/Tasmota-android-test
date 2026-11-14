# Control your Tasmota switches from outside your home with Tailscale

Tasmota Scan & Control talks directly to the switches on your own Wi-Fi.
Away from home that normally does not work, because your phone cannot reach your home network.

[Tailscale](https://tailscale.com) solves this. It makes a private, encrypted connection between your phone and your home network.
The app then works outside exactly as it does at home.

Tested with app version 14.6 on a Ziggo cable connection: it works.

## Why this way

- **Nothing changes on the modem or router.** No port forwarding, and it does not matter that a Ziggo modem has no VPN server.
- **Nothing is opened to the internet.** Only devices signed in to your own Tailscale account can reach your switches.
- **Nothing changes in the app or in Tasmota.** Same addresses, same schedules, same widgets.
- **Free for home use** on Tailscale's personal plan.

## What you need

- A spare Android phone or tablet that **stays at home**, on Wi-Fi and on the charger. It passes the traffic on to your switches. This guide calls it the *home phone*.
- The phone you carry with you. This guide calls it *your phone*.
- A Tailscale account. You can sign in with a Google account.

An Android TV box, a Raspberry Pi, a NAS or a PC that is always on can do the job of the home phone too.

## Step 1: find your home network range

You need the address range of your home network, written like `192.168.1.0/24`.

1. Open Tasmota Scan & Control at home.
2. Look at the Wi-Fi card at the top. It shows your phone's address, for example `192.168.1.57`.
3. Replace the last number with `0` and add `/24`. In this example: `192.168.1.0/24`.

Your switches have addresses that start with the same three numbers.

## Step 2: set up the home phone

1. Install **Tailscale** from the Play Store and sign in.
2. Tap your avatar (top right) and open **Settings**.
3. Choose **Subnet routing**, then **Add route**.
4. Enter the range from step 1, for example `192.168.1.0/24`, and tap **OK**.
5. In the **Exit Node** section, leave **Allow LAN access** off.
6. Leave Tailscale switched on.

Then make sure Android does not put it to sleep:

- Keep the home phone on the charger and on Wi-Fi.
- In Android's settings, set battery use for Tailscale to **Unrestricted** (Samsung: Settings → Apps → Tailscale → Battery).

Tailscale's own note: running a subnet router uses a lot of battery, so it is meant for devices that stay on power.

If you do not see **Subnet routing** in the settings, update the Tailscale app. On an old Android version the option may be missing.

## Step 3: approve the route (once, in a browser)

The route does not work until you approve it.

1. Open <https://login.tailscale.com/admin/machines> and sign in with the same account.
2. Find the home phone in the **Machines** list. It has a small **Subnets** label.
3. Tap the **⋯** menu on that row and choose **Edit route settings**.
4. Tick the box for your range, for example `192.168.1.0/24`, and tap **Save**.

On a phone the page is cramped. If you cannot find the ⋯ menu, turn the phone sideways or choose **Desktop site** in the browser menu.

## Step 4: set up your phone

1. Install **Tailscale** and sign in with the **same account**.
2. That is all. Switch Tailscale on when you are away from home.

## Step 5: test it

1. On your phone, turn Wi-Fi off so it uses mobile data.
2. Switch Tailscale on.
3. Open Tasmota Scan & Control and tap a switch.

If the switch reacts, everything works.

## Using it

- **Away from home:** switch Tailscale on, then use the app as usual. Switches, tiles, scenes, schedules and widgets all work.
- **At home:** you can leave Tailscale on or switch it off.
- **Scan network** is meant for use at home. Away from home, use the switches you saved earlier.
- **Speed:** switching takes a little longer than at home, because the traffic goes through the home phone.

## If it does not work

| What you see | What to check |
| --- | --- |
| Switches show "not reachable" | Is Tailscale on, on both phones? Is the home phone on Wi-Fi? |
| It worked, then stopped after a while | Android put Tailscale to sleep on the home phone. Set its battery use to Unrestricted and keep it on the charger. |
| It never worked | Was the route approved in step 3? Does the range match your home network from step 1? |
| It works on mobile data but not on someone else's Wi-Fi | That network may use the same address range as your home. Try mobile data there. |
| It stopped after some months | Tailscale asks devices to sign in again from time to time. Open Tailscale on the home phone and sign in. In the admin page you can switch off key expiry for the home phone. |

## Good to know

- **Who can reach your switches:** only devices signed in to your Tailscale account. Family members can get access by signing in with that account, or by being invited to your Tailscale network.
- **The home phone must stay on.** If it is off, loses Wi-Fi or runs out of battery, remote control stops until it is back.
- **Privacy:** the traffic between your phones is encrypted. Tailscale's servers help the devices find each other, but they cannot read the traffic.

## More information

- [Tailscale: Configure a subnet router](https://tailscale.com/kb/1406/quick-guide-subnets?tab=android)
- [Tailscale: Subnet routers](https://tailscale.com/docs/features/subnet-routers?tab=android)

# To do (ideas for later)

Ideas we talked about and want to build later. Nothing here is in the app yet.

## Done in 26.7
- Thermostat 0.1 °C steps, hold − / + to repeat, send after 1.5 s, wheel picker (Heating screen and Edit), border around the Edit thermostat part.

## Heating screen: real fire
- Replace the 🔥 emoji with a more real fire. Two options:
  - a live **particle fire** (glowing particles that rise and cool down, drawn in the app, almost no extra size), or
  - a short loop made from **your own wood-stove video** (about 10 s, close up, phone steady, in the dark).
- Only animates while it is really heating; respects "reduce motion".


## Thermostat: sensor log to Google Forms / Sheets (optional, off by default)
- Idea from Lubèrth (did this before with an Arduino): the thermostat script sends every 5–10 min
  an HTTPS request to a Google Form; each send becomes a row in a Google Sheet -> make a graph there.
- Script: `Shelly.call("HTTP.GET", {url: FORM + "?entry.111=" + temp + "&entry.222=" + target + "&entry.333=" + heating, timeout: 15})`
  (`FORM` = `https://docs.google.com/forms/d/e/<form id>/formResponse`; field numbers from the form's "pre-filled link").
- App: in Edit (thermostat part) a field for the form link + the entry numbers, stored in KVS (e.g. `th_glog`);
  the script only sends when it is filled in. Short how-to (make form, find entry numbers, Insert → Chart).
- Notes: uses the user's own Google account (the only cloud part, so optional); no internet = only that line is lost,
  the thermostat keeps working; not more often than every 5 min. Alternative: a small Google Apps Script web app.
- Fully local alternative: keep the last 24 h in the Shelly and draw the graph in the app (see "history graph").
- Template: Lubèrth makes the form once; the app's **Make my copy** button opens its `/copy` link, so every user
  gets his own copy in his own Google account. Each Shelly logs to its own copy: nobody sees other people's data.
  (Copying on a phone may need "Desktop site" in Chrome: check and put it in the how-to.)
- Charts:
  1. In Google Sheets itself: Insert → Chart (room temperature and target over time). No app work needed.
  2. In the app, on the Heating screen: read the history from the user's Sheet and draw a chart:
     day / week / month, room temperature line, target line, orange bands while the boiler was heating
     (e.g. you see the boiler resting on wood-stove evenings).
     The Sheet must be readable: "Publish to web → CSV" gives a secret link; the app explains that anyone
     with that link can read the temperatures. (Alternative: a small Apps Script that returns JSON with a key.)
- Easy setup (about 2 minutes, most Android users have a Google account):
  1. One ready-made template form (fields: temperature, target, heating). In the app a button **Make my copy**
     opens the form's `/copy` link: one tap and the user has the form in his own Google account.
  2. In the form: Send → copy link, paste that link in the app.
  3. The app reads the form page itself and finds the `entry.` numbers automatically (nobody needs to know them).
     Fallback: if Google changes its page and this fails, the app asks for the numbers by hand.
  4. The user links the Sheet once (Responses → Link to Sheets) and makes the chart (Insert → Chart),
     with a short how-to and a small picture in the app.

## Thermostat warning: also when the app is closed
- 27.8 beeps only while the app is open (option A).
- B: Android background notification: a small foreground service polls each thermostat's /state every few minutes
  and shows a notification with sound when `alarm` is set (needs notification permission, uses some battery).
- C: the Shelly script itself sends a push (ntfy.sh, Pushover or Telegram) when the alarm starts; works with the phone off,
  but needs internet and a topic/token set in Edit (KVS e.g. `th_push`).

## Other ideas from earlier
- Thermostat: Boost (warm for an hour) and Away (one low temperature until you are back).
- Temperature history graph (hourly min/max kept by the script, longer history on the phone or wall panel).
- Wall panel mode for an old tablet.
- Text, link and hashtags when sharing a video from the website.

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
- Easy setup (about 2 minutes, most Android users have a Google account):
  1. One ready-made template form (fields: temperature, target, heating). In the app a button **Make my copy**
     opens the form's `/copy` link: one tap and the user has the form in his own Google account.
  2. In the form: Send → copy link, paste that link in the app.
  3. The app reads the form page itself and finds the `entry.` numbers automatically (nobody needs to know them).
     Fallback: if Google changes its page and this fails, the app asks for the numbers by hand.
  4. The user links the Sheet once (Responses → Link to Sheets) and makes the chart (Insert → Chart),
     with a short how-to and a small picture in the app.

## Other ideas from earlier
- Thermostat: Boost (warm for an hour) and Away (one low temperature until you are back).
- Temperature history graph (hourly min/max kept by the script, longer history on the phone or wall panel).
- Wall panel mode for an old tablet.
- Text, link and hashtags when sharing a video from the website.

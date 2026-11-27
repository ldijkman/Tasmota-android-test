# To do (ideas for later)

Ideas we talked about and want to build later. Nothing here is in the app yet.

## Thermostat: finer steps
- − / + in steps of **0.1 °C** everywhere (Heating screen, device card, tile, Edit).
- Send to the Shelly only **1.5 seconds after the last tap**, so not every tap is sent.
  While waiting the number is shown in amber, then a short ✓ when the Shelly has it.
- **Hold − or +** to keep stepping, first slowly, then faster.
- The switching margin stays 0.3 °C.

## Thermostat: wheel picker (demo approved 27 November)
- Heating screen: keep **− and +** on both sides of the target temperature, and make
  the **target number a button** that opens a **wheel picker** (bottom sheet).
- Wheel: 5.0 – 30.0 °C in **0.1 steps**, scroll-snap, the middle value big, a highlight band.
  Tap a number to jump to it. Title "Comfort temperature" or "Eco temperature" (the active level).
- Sent to the Shelly **1.5 s after the wheel stops**; **Done** or tapping outside closes it
  and sends right away if something is still waiting.
- Built in HTML/CSS (scroll-snap), no library needed. WheelPickerCompose
  (github.com/commandiron/WheelPickerCompose) was the idea, but it is Jetpack Compose and the
  app screen is a WebView.
- Device card and tile: only − / + (0.1, hold to repeat, send after 1.5 s), no wheel.

## Heating screen: real fire
- Replace the 🔥 emoji with a more real fire. Two options:
  - a live **particle fire** (glowing particles that rise and cool down, drawn in the app, almost no extra size), or
  - a short loop made from **your own wood-stove video** (about 10 s, close up, phone steady, in the dark).
- Only animates while it is really heating; respects "reduce motion".

- device card edit: thin box outline around thermostat section

## Other ideas from earlier
- Thermostat: Boost (warm for an hour) and Away (one low temperature until you are back).
- Temperature history graph (hourly min/max kept by the script, longer history on the phone or wall panel).
- Wall panel mode for an old tablet.
- Text, link and hashtags when sharing a video from the website.

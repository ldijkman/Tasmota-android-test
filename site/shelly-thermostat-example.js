// Simple thermostat for a Shelly Gen2/Gen3 with the Sensor Add-on (DS18B20)
// EXAMPLE, A LITTLE BIT TESTED: try it with a lamp on the relay before you connect the boiler.
// Start with TEST_MODE = true and read the log; set it to false to really switch.
//
// Paste on the Shelly's own web page: Scripts -> Create script -> paste -> Save -> Start.
// Turn on "Run on startup" so it starts again after a power cut.
// Watch what it does in the log under the script (lots of console.log lines),
// or open the log page in Chrome to copy it: http://<shelly-ip>/script/<script-number>/log
// (the exact address is in the first log line).

// ================= SETTINGS: change these =================

let TEST_MODE        = true;    // true  = only write in the log, never switch the relay
                                // false = really switch the relay (heating)

let ROOM_TEMPERATURE = 20.0;    // the temperature you want (°C), the app can change it via KVS "th_set"
let SWITCH_MARGIN    = 0.3;     // heat below 19.7, stop above 20.3
let FROST_GUARD      = 5.0;     // always heat below this (°C), also when the thermostat is off

let SENSORS          = [100, 101];  // Add-on sensor numbers; one sensor: [100]
let WARN_DIFFERENCE  = 1.5;     // warn if two sensors differ more than this (°C)

let CHECK_EVERY_SECONDS = 30;   // how often to look at the temperature
let MIN_MINUTES_BETWEEN = 3;    // protect the boiler: wait between switching
let SAFETY_OFF_MINUTES  = 10;   // relay turns off by itself if the script stops

// ================= do not change below this line =================

let CFG = {
  dryRun: TEST_MODE,
  sensors: SENSORS,
  setpoint: ROOM_TEMPERATURE,
  hyst: SWITCH_MARGIN,
  frost: FROST_GUARD,
  maxDiff: WARN_DIFFERENCE,
  minSwitchSec: MIN_MINUTES_BETWEEN * 60,
  everySec: CHECK_EVERY_SECONDS,
  watchdogSec: SAFETY_OFF_MINUTES * 60
};

let state = { heating: false, lastSwitch: 0, mode: "on", round: 0 };  // mode: "on" or "off" (KVS "th_mode")

// clock time for the log: "04:43:12" (local time of the Shelly),
// or the seconds since start ("29332s") as long as the Shelly does not know the time yet
function clock() {
  let sys = Shelly.getComponentStatus("sys");
  if (sys && sys.time && sys.unixtime) {
    let sec = sys.unixtime % 60;
    return sys.time + ":" + (sec < 10 ? "0" : "") + sec;
  }
  return (sys ? sys.uptime : 0) + "s";
}

// keep the last log lines in memory for the log page (small: the Shelly has little memory)
let LOG = [];
let LOG_MAX = 60;
// one decimal, rounded the same way everywhere (26.55 -> "26.6")
function f1(x) {
  return (Math.round(x * 10) / 10).toFixed(1);
}

function log(msg) {
  console.log("[thermostat] " + msg);
  LOG.push(clock() + " " + msg);
  if (LOG.length > LOG_MAX) LOG.splice(0, LOG.length - LOG_MAX);
}

// log page: http://<shelly-ip>/script/<script-number>/log  (plain text, easy to copy)
HTTPServer.registerEndpoint("log", function (req, res) {
  res.code = 200;
  res.headers = [["Content-Type", "text/plain; charset=utf-8"]];
  res.body = "Thermostat log (last " + LOG.length + " lines, newest at the bottom)\n\n" + LOG.join("\n") + "\n";
  res.send();
});

// read the settings the app could write in KVS (the Shelly's own storage)
function loadSettings() {
  log("reading settings from KVS ...");
  Shelly.call("KVS.Get", { key: "th_set" }, function (r, err) {
    if (err) { log("KVS th_set not found (error " + err + "), keep setpoint " + CFG.setpoint); return; }
    if (r && r.value !== undefined) {
      let v = Number(r.value);
      if (v >= 5 && v <= 30) {
        if (v !== CFG.setpoint) log("setpoint changed: " + CFG.setpoint + " -> " + v + " °C");
        CFG.setpoint = v;
      } else {
        log("KVS th_set = " + r.value + " is out of range (5-30), ignored");
      }
    }
  });
  Shelly.call("KVS.Get", { key: "th_mode" }, function (r, err) {
    if (err) { log("KVS th_mode not found, keep mode " + state.mode); return; }
    if (r && (r.value === "on" || r.value === "off")) {
      if (r.value !== state.mode) log("mode changed: " + state.mode + " -> " + r.value);
      state.mode = r.value;
    } else {
      log("KVS th_mode = " + (r ? r.value : "?") + " is not on/off, ignored");
    }
  });
}

// room temperature: the average of the sensors that give a value, or null
function roomTemp() {
  let vals = [];
  for (let i = 0; i < CFG.sensors.length; i++) {
    let id = CFG.sensors[i];
    let st = Shelly.getComponentStatus("temperature", id);
    if (st && typeof st.tC === "number") {
      log("sensor " + id + ": " + f1(st.tC) + " °C");
      vals.push(st.tC);
    } else {
      log("sensor " + id + ": NO VALUE (not connected, wrong id or broken?)");
    }
  }
  if (vals.length === 0) return null;
  if (vals.length === 2) {
    let diff = Math.abs(vals[0] - vals[1]);
    log("difference between sensors: " + f1(diff) + " °C");
    if (diff > CFG.maxDiff) log("WARNING: sensors differ more than " + CFG.maxDiff + " °C");
  }
  let sum = 0;
  for (let i = 0; i < vals.length; i++) sum += vals[i];
  let avg = sum / vals.length;
  log("room temperature (average of " + vals.length + "): " + f1(avg) + " °C");
  return avg;
}

function setHeating(on, why) {
  if (on === state.heating) {
    log("heating stays " + (on ? "ON" : "OFF") + " (" + why + ")");
    return;
  }
  let now = Shelly.getComponentStatus("sys").uptime;
  let since = now - state.lastSwitch;
  // switching off for safety is always allowed; otherwise wait the minimum time
  if (why !== "safety" && state.lastSwitch > 0 && since < CFG.minSwitchSec) {
    log("want heating " + (on ? "ON" : "OFF") + " but last switch was " + since + " s ago, wait until " + CFG.minSwitchSec + " s");
    return;
  }
  if (CFG.dryRun) {
    log("TEST MODE: would turn heating " + (on ? "ON" : "OFF") + " (" + why + "), relay NOT switched");
  } else {
    log("switching heating " + (on ? "ON" : "OFF") + " (" + why + ")" + (on ? ", watchdog " + CFG.watchdogSec + " s" : ""));
    let args = on ? { id: 0, on: true, toggle_after: CFG.watchdogSec } : { id: 0, on: false };
    Shelly.call("Switch.Set", args, function (r, err, msg) {
      if (err) log("ERROR switching relay: " + err + " " + msg);
      else log("relay switched " + (on ? "ON" : "OFF") + ", was " + (r && r.was_on ? "ON" : "OFF"));
    });
  }
  state.heating = on;
  state.lastSwitch = now;
}

// watchdog: while heating, set "on, and off again after watchdogSec" every check.
// Each refresh starts the timer over; if the script stops, the Shelly switches off by itself.
function refreshWatchdog() {
  if (!state.heating) return;
  if (CFG.dryRun) { log("TEST MODE: would refresh watchdog (" + CFG.watchdogSec + " s)"); return; }
  Shelly.call("Switch.Set", { id: 0, on: true, toggle_after: CFG.watchdogSec }, function (r, err, msg) {
    if (err) log("ERROR refreshing watchdog: " + err + " " + msg);
    else log("watchdog refreshed: relay turns off by itself in " + CFG.watchdogSec + " s if the script stops");
  });
}

function check() {
  state.round++;
  // compare with the real relay: it may have been switched by hand, by the watchdog or a schedule
  if (!CFG.dryRun) {
    let sw = Shelly.getComponentStatus("switch", 0);
    let real = !!(sw && sw.output);
    if (real !== state.heating) {
      log("relay is " + (real ? "ON" : "OFF") + " but script thought " + (state.heating ? "ON" : "OFF") + " (switched by hand, watchdog or schedule?)");
      state.heating = real;
    }
  }
  log("---- check " + state.round + " · setpoint " + CFG.setpoint + " °C · mode " + state.mode +
      " · heating " + (state.heating ? "ON" : "OFF") + (CFG.dryRun ? " · TEST MODE" : "") + " ----");
  let t = roomTemp();
  if (t === null) {                       // no sensor works: heating off
    log("no temperature at all: heating off for safety");
    setHeating(false, "safety");
    return;
  }
  if (t < CFG.frost) {                    // frost guard always wins
    log("below frost guard " + CFG.frost + " °C");
    setHeating(true, "frost");
    refreshWatchdog();
    return;
  }
  if (state.mode === "off") {
    setHeating(false, "mode off");
    return;
  }
  let low = CFG.setpoint - CFG.hyst, high = CFG.setpoint + CFG.hyst;
  log("heat below " + f1(low) + " °C, stop above " + f1(high) + " °C");
  if (t < low) setHeating(true, f1(t) + " < " + f1(low));
  else if (t > high) setHeating(false, f1(t) + " > " + f1(high));
  else log(f1(t) + " °C is between " + f1(low) + " and " + f1(high) + ": no change (heating stays " + (state.heating ? "ON" : "OFF") + ")");
  refreshWatchdog();
}

// start: read the relay as it is now, then check every 30 s (settings every 5 min)
let wifi = Shelly.getComponentStatus("wifi");
log("log page: http://" + (wifi && wifi.sta_ip ? wifi.sta_ip : "<shelly-ip>") + "/script/" + Shelly.getCurrentScriptId() + "/log");
let sw = Shelly.getComponentStatus("switch", 0);
state.heating = !!(sw && sw.output);
log("script started · TEST MODE " + (TEST_MODE ? "on (relay is never switched)" : "OFF (relay really switches!)") +
    " · room " + f1(ROOM_TEMPERATURE) + " °C · margin " + SWITCH_MARGIN + " · frost guard " + f1(FROST_GUARD) + " °C");
log("sensors " + SENSORS.join(", ") + " · check every " + CHECK_EVERY_SECONDS + " s · wait " + MIN_MINUTES_BETWEEN +
    " min between switching · safety off after " + SAFETY_OFF_MINUTES + " min · relay is now " + (state.heating ? "ON" : "OFF"));
loadSettings();
Timer.set(CFG.everySec * 1000, true, check);
Timer.set(300 * 1000, true, loadSettings);
check();

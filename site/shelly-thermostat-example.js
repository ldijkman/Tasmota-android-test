// Simple thermostat for a Shelly Gen2/Gen3 with the Sensor Add-on (DS18B20)
// EXAMPLE, A LITTLE BIT TESTED: try it with a lamp on the relay before you connect the boiler.
// NO FROST GUARD in this script: a script can stop. For frost protection use MECHANICAL HARDWARE:
// a bimetal frost thermostat wired in parallel with the Shelly relay (like the old Honeywell ones),
// plus the boiler's own frost protection. Those also work without power, Wi-Fi or software.
// For safety against overheating: a normally closed (NC) max thermostat (e.g. 30 °C) in series with the relay.
// Start with TEST_MODE = true and read the log; set it to false to really switch.
//
// Paste on the Shelly's own web page: Scripts -> Create script -> paste -> Save -> Start.
// Turn on "Run on startup" so it starts again after a power cut.
// It reacts at once when the temperature changes, and checks every 30 s as a backup.
// Watch what it does in the log under the script (lots of console.log lines),
// or open the log page in Chrome to copy it: http://<shelly-ip>/script/<script-number>/log
// (the exact address is in the first log line).
// The app "Shelly & Tasmota Scan & Control" can install this script and change the settings
// through KVS: th_comfort, th_eco (temperatures), th_level (comfort/eco), th_mode (on/off),
// th_sensor (number), th_test (on/off), th_margin (0.1-1.0), th_stuck (minutes, 0 = off). The app's schedules set th_level at the switch times.

// ================= SETTINGS: change these =================

let TEST_MODE        = true;    // true  = only write in the log, never switch the relay (app: KVS "th_test")
                                // false = really switch the relay (heating)

let COMFORT          = 20.5;    // Comfort temperature (°C), app: KVS "th_comfort"
let ECO              = 16.0;    // Eco temperature (°C), app: KVS "th_eco"
let LEVEL            = "comfort";  // "comfort" or "eco": set by the schedules in the Shelly, app: KVS "th_level"
let SWITCH_MARGIN    = 0.3;     // heat below 20.2, stop above 20.8 (0.1 - 1.0), app: KVS "th_margin"

let SENSOR           = 100;     // Add-on sensor number (see the Shelly page: Temperature (100)), app: KVS "th_sensor"

let CHECK_EVERY_SECONDS = 30;   // how often to look at the temperature
let MIN_MINUTES_BETWEEN = 0;    // 0 = react directly; some boilers want e.g. 3 minutes between switching
let SAFETY_OFF_MINUTES  = 10;   // relay turns off by itself if the script stops
let STUCK_MINUTES       = 60;   // warn if heating this long without the room getting 0.2 °C warmer (0 = off), app: KVS "th_stuck"

// ================= do not change below this line =================

let VERSION = 5;                // the app checks this to offer an update of the script

let CFG = {
  dryRun: TEST_MODE,
  sensor: SENSOR,
  comfort: COMFORT,
  eco: ECO,
  level: LEVEL,
  setpoint: COMFORT,
  hyst: SWITCH_MARGIN,
  minSwitchSec: MIN_MINUTES_BETWEEN * 60,
  everySec: CHECK_EVERY_SECONDS,
  watchdogSec: SAFETY_OFF_MINUTES * 60,
  stuckSec: STUCK_MINUTES * 60
};

let state = { heating: false, lastSwitch: 0, mode: "on", round: 0, alarm: "" };   // alarm: "", "sensor" or "stuck"  // mode: "on" or "off" (KVS "th_mode")

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

// read the settings the app could write in KVS (the Shelly's own storage); done() runs when all are read
function kvs(key, use, done) {
  Shelly.call("KVS.Get", { key: key }, function (r, err) {
    if (!err && r && r.value !== undefined) use(String(r.value));
    done();
  });
}
function temp(v) { let n = Number(v); return n >= 5 && n <= 30 ? n : null; }
function useLevel() {   // the temperature the thermostat heats to: Comfort or Eco
  let t = CFG.level === "eco" ? CFG.eco : CFG.comfort;
  if (t !== CFG.setpoint) log("now " + CFG.level + ": " + f1(t) + " °C");
  CFG.setpoint = t;
}
// all settings in ONE call: a Shelly allows only a few calls at the same time
function loadSettings(then) {
  Shelly.call("KVS.GetMany", { match: "th_*" }, function (r, err) {
    let kv = {};
    if (!err && r && r.items) {
      if (r.items.length !== undefined) {           // newer firmware: a list
        for (let i = 0; i < r.items.length; i++) kv[r.items[i].key] = String(r.items[i].value);
      } else {                                      // older firmware: an object
        for (let k in r.items) kv[k] = String(r.items[k].value);
      }
    } else if (err) log("could not read the settings (error " + err + "), keep the old ones");
    if (kv.th_comfort !== undefined) {
      let n = temp(kv.th_comfort);
      if (n === null) log("KVS th_comfort = " + kv.th_comfort + " is out of range (5-30), ignored");
      else { if (n !== CFG.comfort) log("Comfort changed: " + f1(CFG.comfort) + " -> " + f1(n) + " °C"); CFG.comfort = n; }
    }
    if (kv.th_eco !== undefined) {
      let n = temp(kv.th_eco);
      if (n === null) log("KVS th_eco = " + kv.th_eco + " is out of range (5-30), ignored");
      else { if (n !== CFG.eco) log("Eco changed: " + f1(CFG.eco) + " -> " + f1(n) + " °C"); CFG.eco = n; }
    }
    let v = kv.th_level;
    if (v === "comfort" || v === "eco") { if (v !== CFG.level) log("level changed: " + CFG.level + " -> " + v); CFG.level = v; }
    v = kv.th_mode;
    if (v === "on" || v === "off") { if (v !== state.mode) log("mode changed: " + state.mode + " -> " + v); state.mode = v; }
    if (kv.th_sensor !== undefined) {
      let n = Number(kv.th_sensor);
      if (n >= 100 && n <= 199) { if (n !== CFG.sensor) log("sensor changed: " + CFG.sensor + " -> " + n); CFG.sensor = n; }
    }
    if (kv.th_margin !== undefined) {
      let n = Math.round(Number(kv.th_margin) * 10) / 10;
      if (n >= 0.1 && n <= 1) { if (n !== CFG.hyst) log("margin changed: " + CFG.hyst + " -> " + n + " °C"); CFG.hyst = n; }
      else log("KVS th_margin = " + kv.th_margin + " is out of range (0.1-1.0), ignored");
    }
    if (kv.th_stuck !== undefined) {
      let n = Math.round(Number(kv.th_stuck));
      if (n >= 0 && n <= 240) { if (n * 60 !== CFG.stuckSec) log("not-rising warning changed: " + (n ? n + " min" : "off")); CFG.stuckSec = n * 60; }
    }
    v = kv.th_test;
    if (v === "on" || v === "off") {
      let test = v === "on";
      if (test !== CFG.dryRun) log("TEST MODE changed: " + (test ? "on (relay is never switched)" : "OFF (relay really switches!)"));
      CFG.dryRun = test;
    }
    useLevel();
    if (then) then();
  });
}

// the schedules write th_level; read it at every check (reading KVS does not wear the flash)
function loadLevel(then) {
  kvs("th_level", function (v) { if ((v === "comfort" || v === "eco") && v !== CFG.level) { log("schedule: level " + CFG.level + " -> " + v); CFG.level = v; } },
    function () { useLevel(); then(); });
}

// state page for the app: http://<shelly-ip>/script/<script-number>/state  (JSON)
let lastTemp = null, lastCheck = 0;
let heatFrom = 0, heatT0 = null;   // since when it heats without the room getting warmer, and the temperature at that moment
function upNow() { let s = Shelly.getComponentStatus("sys"); return s && s.uptime ? s.uptime : 0; }
HTTPServer.registerEndpoint("state", function (req, res) {
  res.code = 200;
  res.headers = [["Content-Type", "application/json"]];
  res.body = JSON.stringify({ thermostat: 1, temp: lastTemp, set: CFG.setpoint, comfort: CFG.comfort, eco: CFG.eco,
    level: CFG.level, margin: CFG.hyst, mode: state.mode,
    heating: state.heating, test: CFG.dryRun, sensor: CFG.sensor, round: state.round, checked: lastCheck,
    watchdog: CFG.watchdogSec, alarm: state.alarm, stuck: CFG.stuckSec / 60, heatMin: heatFrom ? Math.round((upNow() - heatFrom) / 60) : 0,
    version: VERSION });
  res.send();
});

// the app calls this right after changing a setting, so it works at once instead of within 5 minutes
HTTPServer.registerEndpoint("reload", function (req, res) {
  log("settings reload asked (by the app)");
  loadSettings(check);
  res.code = 200;
  res.headers = [["Content-Type", "application/json"]];
  res.body = JSON.stringify({ ok: true });
  res.send();
});

// room temperature from the sensor, or null if it gives no value
function roomTemp() {
  let st = Shelly.getComponentStatus("temperature", CFG.sensor);
  if (st && typeof st.tC === "number") {
    log("sensor " + CFG.sensor + ": " + f1(st.tC) + " °C");
    return st.tC;
  }
  log("sensor " + CFG.sensor + ": NO VALUE (not connected, wrong number or broken?)");
  return null;
}

function setHeating(on, why) {
  if (on === state.heating) {
    log("heating stays " + (on ? "ON" : "OFF") + " (" + why + ")");
    return;
  }
  let now = Shelly.getComponentStatus("sys").uptime;
  let since = now - state.lastSwitch;
  // switching off for safety or because you turned the thermostat off is always allowed; otherwise wait
  if (why !== "safety" && why !== "mode off" && state.lastSwitch > 0 && since < CFG.minSwitchSec) {
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
  if (!CFG.dryRun) state.lastSwitch = now;   // test mode: no real switch, so no boiler wait afterwards
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

let busy = false;   // one check at a time (a Shelly allows only a few calls at once)
function check() {
  if (busy) return;
  busy = true;
  loadLevel(function () { busy = false; doCheck(); });
}
function doCheck() {
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
  log("---- check " + state.round + " · " + CFG.level + " " + f1(CFG.setpoint) + " °C · mode " + state.mode +
      " · heating " + (state.heating ? "ON" : "OFF") + (CFG.dryRun ? " · TEST MODE" : "") + " ----");
  let t = roomTemp();
  lastTemp = t === null ? null : Math.round(t * 10) / 10;
  let sys = Shelly.getComponentStatus("sys");
  lastCheck = sys && sys.unixtime ? sys.unixtime : 0;
  if (t === null) {                       // no sensor works: heating off
    if (state.alarm !== "sensor") log("WARNING: the sensor gives no temperature: heating off for safety");
    state.alarm = "sensor"; heatFrom = 0;
    setHeating(false, "safety");
    return;
  }
  if (state.alarm === "sensor") { log("the sensor works again"); state.alarm = ""; }
  if (state.mode === "off") {
    setHeating(false, "mode off");
    return;
  }
  let low = CFG.setpoint - CFG.hyst, high = CFG.setpoint + CFG.hyst;
  log("heat below " + f1(low) + " °C, stop above " + f1(high) + " °C");
  if (t < low) setHeating(true, f1(t) + " < " + f1(low));
  else if (t > high) setHeating(false, f1(t) + " > " + f1(high));
  else log(f1(t) + " °C is between " + f1(low) + " and " + f1(high) + ": no change (heating stays " + (state.heating ? "ON" : "OFF") + ")");
  watchRise(t);
  refreshWatchdog();
}

// warning (only a warning, the heating keeps running): it heats for STUCK_MINUTES but the room does not get
// 0.2 °C warmer. Then the sensor fell off / sits in the wrong place, or the boiler does not heat.
function watchRise(t) {
  if (!state.heating || CFG.dryRun || !CFG.stuckSec) {
    heatFrom = 0;
    if (state.alarm === "stuck") { log("not-rising warning cleared (heating off)"); state.alarm = ""; }
    return;
  }
  let now = upNow();
  if (!heatFrom || t >= heatT0 + 0.2) {          // started heating, or the room got warmer: start counting again
    if (heatFrom && state.alarm === "stuck") { log("temperature rises again (" + f1(heatT0) + " -> " + f1(t) + " °C): warning cleared"); state.alarm = ""; }
    heatFrom = now; heatT0 = t;
    return;
  }
  if (now - heatFrom >= CFG.stuckSec && state.alarm !== "stuck") {
    state.alarm = "stuck";
    log("WARNING: heating for " + Math.round((now - heatFrom) / 60) + " min but the room did not get warmer (" + f1(heatT0) + " -> " + f1(t) +
        " °C): check the sensor and the boiler. Heating keeps running.");
  }
}

// start: read the relay as it is now, then check every 30 s (settings every 5 min)
let wifi = Shelly.getComponentStatus("wifi");
log("log page: http://" + (wifi && wifi.sta_ip ? wifi.sta_ip : "<shelly-ip>") + "/script/" + Shelly.getCurrentScriptId() + "/log");
let sw = Shelly.getComponentStatus("switch", 0);
state.heating = !!(sw && sw.output);
log("script started · TEST MODE " + (TEST_MODE ? "on (relay is never switched)" : "OFF (relay really switches!)") +
    " · Comfort " + f1(COMFORT) + " °C · Eco " + f1(ECO) + " °C · margin " + SWITCH_MARGIN + " · warn if not rising after " + STUCK_MINUTES + " min · no frost guard (use a mechanical one)");
log("sensor " + SENSOR + " (KVS th_sensor can change it) · check every " + CHECK_EVERY_SECONDS + " s · wait " + MIN_MINUTES_BETWEEN +
    " min between switching · safety off after " + SAFETY_OFF_MINUTES + " min · relay is now " + (state.heating ? "ON" : "OFF"));
// react at once when the sensor reports a new temperature (every change of its threshold, e.g. 0.1 °C);
// the check every 30 s stays as a backup
let soon = null;
Shelly.addStatusHandler(function (e) {
  if (!e || e.component !== "temperature:" + CFG.sensor || !e.delta || e.delta.tC === undefined) return;
  if (soon !== null) return;                        // several reports at once: one check
  soon = Timer.set(300, false, function () { soon = null; log("new temperature from the sensor"); check(); });
});

loadSettings(function () {   // first the settings from KVS, then the first check
  Timer.set(CFG.everySec * 1000, true, check);
  Timer.set(300 * 1000, true, function () { loadSettings(); });
  check();
});

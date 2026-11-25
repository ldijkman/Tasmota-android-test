// Simple thermostat for a Shelly Gen2/Gen3 with the Sensor Add-on (DS18B20)
// EXAMPLE, NOT TESTED YET: try it with a lamp on the relay before you connect the boiler.
//
// Paste on the Shelly's own web page: Scripts -> Create script -> paste -> Save -> Start.
// Turn on "Run on startup" so it starts again after a power cut.

let CFG = {
  sensors: [100, 101],   // Add-on sensor ids; use [100] for one sensor
  setpoint: 20.0,        // default room temperature (°C), can be changed via KVS "th_set"
  hyst: 0.3,             // heat below 19.7, stop above 20.3
  frost: 5.0,            // frost guard: always heat below this, even when "off"
  maxDiff: 1.5,          // warn if two sensors differ more than this
  minSwitchSec: 180,     // at least 3 minutes between switching (protects the boiler)
  everySec: 30           // check every 30 seconds
};

let state = { heating: false, lastSwitch: 0, mode: "on" };  // mode: "on" or "off" (KVS "th_mode")

// read the settings the app could write in KVS (the Shelly's own storage)
function loadSettings() {
  Shelly.call("KVS.Get", { key: "th_set" }, function (r) {
    if (r && r.value !== undefined) {
      let v = Number(r.value);
      if (v >= 5 && v <= 30) CFG.setpoint = v;
    }
  });
  Shelly.call("KVS.Get", { key: "th_mode" }, function (r) {
    if (r && (r.value === "on" || r.value === "off")) state.mode = r.value;
  });
}

// room temperature: the average of the sensors that give a value, or null
function roomTemp() {
  let vals = [];
  for (let i = 0; i < CFG.sensors.length; i++) {
    let st = Shelly.getComponentStatus("temperature", CFG.sensors[i]);
    if (st && typeof st.tC === "number") vals.push(st.tC);
  }
  if (vals.length === 0) return null;
  if (vals.length === 2 && Math.abs(vals[0] - vals[1]) > CFG.maxDiff) {
    print("Warning: sensors differ: " + vals[0] + " / " + vals[1]);
  }
  let sum = 0;
  for (let i = 0; i < vals.length; i++) sum += vals[i];
  return sum / vals.length;
}

function setHeating(on, why) {
  if (on === state.heating) return;
  let now = Shelly.getComponentStatus("sys").uptime;
  // switching off for safety is always allowed; otherwise wait the minimum time
  if (why !== "safety" && now - state.lastSwitch < CFG.minSwitchSec) return;
  Shelly.call("Switch.Set", { id: 0, on: on });
  state.heating = on;
  state.lastSwitch = now;
  print("Heating " + (on ? "ON" : "OFF") + " (" + why + ")");
}

function check() {
  let t = roomTemp();
  if (t === null) {                       // no sensor works: heating off
    setHeating(false, "safety");
    print("No temperature: heating off");
    return;
  }
  if (t < CFG.frost) {                    // frost guard always wins
    setHeating(true, "frost");
    return;
  }
  if (state.mode === "off") {
    setHeating(false, "mode off");
    return;
  }
  if (t < CFG.setpoint - CFG.hyst) setHeating(true, t.toFixed(1) + " < " + CFG.setpoint);
  else if (t > CFG.setpoint + CFG.hyst) setHeating(false, t.toFixed(1) + " > " + CFG.setpoint);
}

// start: read the relay as it is now, then check every 30 s (settings every 5 min)
let sw = Shelly.getComponentStatus("switch", 0);
state.heating = !!(sw && sw.output);
loadSettings();
Timer.set(CFG.everySec * 1000, true, check);
Timer.set(300 * 1000, true, loadSettings);
check();

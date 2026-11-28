// Shelly thermostat log → Google Form → Google Sheet   (version 2: all Add-on sensors)
// A SECOND script next to the thermostat script. The app installs it (Heating screen → 📈 History),
// or by hand: Shelly web page → Scripts → Create script.
// Every 10 minutes, on round times (:00, :10, :20 …), it sends one row with what this Shelly has:
//   temp (thermostat sensor), target, heating (1/0), level (comfort/eco/off),
//   t2–t5 (more DS18B20), humidity (DHT22), input (digital 1/0), analog (%), volt, shelly_c (own temperature).
// Sensors that are not there are left out. Only reads; it never switches anything.
// No internet = that row is lost, the thermostat keeps working.

// ▼ by hand: paste the link of YOUR form here (in your Sheet: Tools → Manage form → Go to live form, copy the address) ▼
//   The app does not change this line: it puts the link in KVS "th_glog", which wins over this line.
let FORM  = "https://docs.google.com/forms/d/e/PASTE-YOUR-FORM-ID/viewform";
// ▲ ------------------------------------------------------------------------------------------------------ ▲

// the field numbers of the template form: a copy keeps the same numbers, so these are the same for everyone
let ENTRY = { device: "525109377", temp: "1890615910", target: "858344194", heating: "133537199", level: "247918794",
              t2: "1879753428", t3: "955815247", t4: "1740040625", t5: "1572772288", humidity: "944997455",
              input: "360241412", analog: "834668660", volt: "1369450860", shelly_c: "1867081969" };

// a "viewform" link (with or without ?usp=…) works too: it becomes the "formResponse" link here
function formUrl(f) { if (f.indexOf("?") >= 0) f = f.slice(0, f.indexOf("?")); return f.replace("/viewform", "/formResponse"); }

let EVERY_MINUTES = 10;    // on round times: 10 = at :00, :10, :20 … (not less than 5, please)
let SWITCH_ID     = 0;     // the relay of the thermostat
let SENSOR_DEF    = 100;   // Add-on sensor of the thermostat, used when KVS th_sensor is not set

// simple URL encoding (letters, digits and - _ . stay, the rest becomes %XX)
function enc(s) { s = String(s); let o = "", ok = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.";
  for (let i = 0; i < s.length; i++) { let c = s.charAt(i);
    if (ok.indexOf(c) >= 0) o += c; else { let h = s.charCodeAt(i) & 255; o += "%" + (h < 16 ? "0" : "") + h.toString(16).toUpperCase(); } }
  return o; }

let NAME = Shelly.getDeviceInfo().name || Shelly.getDeviceInfo().id;

function st(c) { return Shelly.getComponentStatus(c); }        // null when the Shelly does not have it

function send() {
  Shelly.call("KVS.GetMany", { match: "th_*" }, function (r, err) {
    let kv = {};
    if (!err && r && r.items) {
      if (r.items.length !== undefined) { for (let i = 0; i < r.items.length; i++) kv[r.items[i].key] = String(r.items[i].value); }
      else { for (let k in r.items) kv[k] = String(r.items[k].value); }
    }
    let sensor = kv.th_sensor !== undefined ? Number(kv.th_sensor) : SENSOR_DEF;
    let v = {};                                               // column → value

    // thermostat sensor
    let t = st("temperature:" + sensor);
    if (!t || typeof t.tC !== "number") { print("log: no temperature from sensor " + sensor + ", skip"); return; }
    v.temp = t.tC.toFixed(1);

    // the other DS18B20s (100–104), in order, become t2–t5
    let k = 2;
    for (let id = 100; id <= 104 && k <= 5; id++) {
      if (id === sensor) continue;
      let o = st("temperature:" + id);
      if (o && typeof o.tC === "number") { v["t" + k] = o.tC.toFixed(1); k++; }
    }

    // DHT22 humidity
    let h = st("humidity:100");
    if (h && typeof h.rh === "number") v.humidity = h.rh.toFixed(0);

    // Add-on input: digital (state true/false) or analog (percent)
    let inp = st("input:100");
    if (inp) {
      if (typeof inp.percent === "number") v.analog = inp.percent.toFixed(1);
      else if (typeof inp.state === "boolean") v.input = inp.state ? 1 : 0;
    }

    // voltmeter
    let vm = st("voltmeter:100");
    if (vm && typeof vm.voltage === "number") v.volt = vm.voltage.toFixed(2);

    // relay + the Shelly's own temperature
    let sw = st("switch:" + SWITCH_ID);
    v.heating = sw && sw.output ? 1 : 0;
    if (sw && sw.temperature && typeof sw.temperature.tC === "number") v.shelly_c = sw.temperature.tC.toFixed(1);

    // thermostat settings
    let level = kv.th_level === "eco" ? "eco" : "comfort";
    let target = level === "eco" ? kv.th_eco : kv.th_comfort;
    if (target !== undefined) v.target = target;
    v.level = kv.th_mode === "off" ? "off" : level;
    v.device = NAME;

    // build the link: only fields that are in ENTRY and have a value
    let form = formUrl(kv.th_glog ? kv.th_glog : FORM);
    if (form.indexOf("PASTE-YOUR") >= 0) { print("log: no form link yet (KVS th_glog or FORM at the top)"); return; }
    let url = form, sep = "?";
    for (let f in ENTRY) {
      if (v[f] === undefined || v[f] === "") continue;
      url += sep + "entry." + ENTRY[f] + "=" + enc(v[f]); sep = "&";
    }

    Shelly.call("HTTP.GET", { url: url, timeout: 20 }, function (res, e, msg) {
      // Google answers with a big page; the row is saved even if the Shelly says the answer is too long
      if (res && res.code === 200) print("log: sent " + v.temp + " °C");
      else print("log: send failed (" + (res ? res.code : e) + " " + (msg || "") + ")");
    });
  });
}

// send on round times: with 5 minutes at :00, :05, :10 ... (the Shelly clock comes from the internet)
function plan() {
  let step = EVERY_MINUTES * 60;
  let now  = st("sys").unixtime;
  let wait = now ? (step - (now % step)) : step;           // no clock yet: just wait one step and try again
  if (wait < 5) wait += step;                               // too close: take the next one
  Timer.set(wait * 1000 + 500, false, function () {         // +0.5 s so we are surely past the round time
    if (st("sys").unixtime) send();
    plan();                                                 // plan the next one again from the clock: no drift
  });
}

send();                                                     // one row right away, to test
plan();

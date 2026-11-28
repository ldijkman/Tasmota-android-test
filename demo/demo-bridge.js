// Demo bridge for GitHub Pages.
// The Android app gives the page a `window.Tasmota` object (see MainActivity.java -> Bridge).
// This file fakes that object with a few simulated Tasmota switches, so the real
// index.html runs unchanged in any browser. Nothing here talks to a real network.
(function () {
  'use strict';

  // After "Reset demo": clear once more before the page starts (the page saves a few things while it closes)
  const clearDemo = () => { try { Object.keys(localStorage).filter(k => k.startsWith('tas_')).forEach(k => localStorage.removeItem(k)); } catch (e) {} };
  try { if (sessionStorage.getItem('demo_reset')) { sessionStorage.removeItem('demo_reset'); clearDemo(); } } catch (e) {}

  // First start of the demo (also after "Reset demo"): example switches with icons and on colours
  try {
    if (!localStorage.getItem('tas_devs')) {
      const D = (ip, friendly, name, icon, oncolor, group) => Object.assign({ ip: '10.10.100.' + ip, friendly, name, icon, oncolor }, group ? { group } : {});
      localStorage.setItem('tas_devs', JSON.stringify([
        D(114, 'Plafond LED', 'plafond-led', 'ceiling', '#ffffff', 'living'),
        D(131, 'Desk lamp', 'bureau', 'desk', '#b48cff', 'living'),
        D(115, 'Kitchen', 'keuken', 'spot', '#ffd27a'),
        D(120, 'Garden lights', 'tuin', 'tree', '#3ccf6e')
      ]));
      localStorage.setItem('tas_groups', JSON.stringify([{ id: 'living', name: 'Living', color: '#3ccf6e' }]));
      localStorage.setItem('tas_devs_ordered', '1');
    }
    // the thermostat Shelly (also added once for people who opened the demo before)
    if (!localStorage.getItem('tas_demo_th1')) {
      const devs = JSON.parse(localStorage.getItem('tas_devs') || '[]');
      if (!devs.some(d => d.ip === '10.10.100.153')) devs.push({ ip: '10.10.100.153', kind: 'shelly', gen: 3, comp: 'switch', model: 'S1G3',
        name: 'shelly1g3-ecda3bc1e2f0', friendly: 'Living room', mac: 'EC:DA:3B:C1:E2:F0', icon: 'fire', oncolor: '#ff6a1f', group: 'living', thermo: 1 });
      localStorage.setItem('tas_devs', JSON.stringify(devs));
      if (!localStorage.getItem('tas_groups')) localStorage.setItem('tas_groups', JSON.stringify([{ id: 'living', name: 'Living', color: '#3ccf6e' }]));
      localStorage.setItem('tas_demo_th1', '1');
    }
  } catch (e) {}

  const SUBNET = '10.10.100.';
  const devices = {
    '10.10.100.114': dev('plafond-led', 'Plafond LED', true),
    '10.10.100.115': Object.assign(dev('keuken', 'Kitchen', false), { sns: { AM2301: { Temperature: 21.4, Humidity: 55 } } }),
    '10.10.100.120': Object.assign(dev('tuin', 'Garden lights', false), { sns: { DS18B20: { Temperature: 6.3 } } }),
    '10.10.100.131': dev('bureau', 'Desk lamp', true),
    '10.10.100.140': Object.assign(dev('garage', 'Garage', false), { protected: true })   // switch with web password "demo"
  };

  function dev(name, friendly, on) {
    const t = (n, mode, time, action) => ({ Enable: 1, Mode: mode, Time: time, Window: 0, Days: '1111111', Repeat: 1, Output: 1, Action: action });
    return {
      name, friendly, topic: 'tasmota_' + name, on,
      timers: { Timer1: t(1, 2, '00:30', 1), Timer2: t(2, 0, '23:00', 0), Timer3: t(3, 0, '07:00', 1), Timer4: t(4, 1, '00:00', 0) },
      lat: 52.37, lon: 4.89
    };
  }

  const pad = n => String(n).padStart(2, '0');
  function localIso(d = new Date()) {
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }
  // Rough sunrise/sunset (NOAA approximation), good enough for a demo
  function sunTimes(lat, lon) {
    const now = new Date(), rad = Math.PI / 180;
    const doy = Math.floor((now - new Date(now.getFullYear(), 0, 0)) / 864e5);
    const g = 2 * Math.PI / 365 * (doy - 1);
    const decl = 0.006918 - 0.399912 * Math.cos(g) + 0.070257 * Math.sin(g) - 0.006758 * Math.cos(2 * g) + 0.000907 * Math.sin(2 * g);
    const eq = 229.18 * (0.000075 + 0.001868 * Math.cos(g) - 0.032077 * Math.sin(g) - 0.014615 * Math.cos(2 * g) - 0.040849 * Math.sin(2 * g));
    const ha = Math.acos(Math.cos(90.833 * rad) / (Math.cos(lat * rad) * Math.cos(decl)) - Math.tan(lat * rad) * Math.tan(decl)) / rad;
    const tz = -now.getTimezoneOffset();
    const fmt = m => { m = ((Math.round(m) % 1440) + 1440) % 1440; return pad(Math.floor(m / 60)) + ':' + pad(m % 60); };
    return { rise: fmt(720 - 4 * (lon + ha) - eq + tz), set: fmt(720 - 4 * (lon - ha) - eq + tz) };
  }

  function status(d) {
    return { Status: { Module: 1, DeviceName: d.name, FriendlyName: [d.friendly], Topic: d.topic, Power: d.on ? 1 : 0 } };
  }

  // Answer one Tasmota command the way real firmware does
  // Auto-off (PulseTime1): 1-111 = tenths of a second, 112+ = seconds + 100
  const pulseSec = v => { v = +v || 0; return v > 111 ? v - 100 : v / 10; };
  const secPulse = s => s <= 0 ? 0 : s <= 11.1 ? Math.round(s * 10) : Math.round(s) + 100;
  function checkPulse(d) { if (d.offAt && Date.now() >= d.offAt) { d.on = false; d.offAt = null; } }
  function command(d, cmnd) {
    checkPulse(d);
    const m = cmnd.match(/^(\S+)\s*(.*)$/s) || [];
    const key = (m[1] || '').toLowerCase(), arg = (m[2] || '').trim();
    const power = () => ({ POWER: d.on ? 'ON' : 'OFF' });

    if (key === 'power' || key === 'power1') {
      const a = arg.toLowerCase();
      if (a === 'on' || a === '1') d.on = true;
      else if (a === 'off' || a === '0') d.on = false;
      else if (a === 'toggle' || a === '2') d.on = !d.on;
      d.offAt = d.on && d.pulse ? Date.now() + d.pulse * 1000 : null;   // every "on" starts the auto-off again
      return power();
    }
    if (key === 'status') {
      if (arg === '7') {
        const s = sunTimes(d.lat, d.lon);
        return { StatusTIM: { UTC: new Date().toISOString().slice(0, 19), Local: localIso(), Timezone: 99, Sunrise: s.rise, Sunset: s.set } };
      }
      if (arg === '2') {
        // firmware version and chip, like real firmware (StatusFWR)
        return { StatusFWR: { Version: d.name === 'tuin' ? '13.4.0(release-tasmota)' : '14.3.0(release-tasmota)', BuildDateTime: '2024-10-15T10:00:00',
          Core: '2_7_7', SDK: '2.2.2-dev(38a443e)', CpuFrequency: 80, Hardware: d.name === 'bureau' ? 'ESP32-C3' : 'ESP8285N08' } };
      }
      if (arg === '1') {
        // last restart reason and boot count, like real firmware (StatusPRM)
        return { StatusPRM: { Baudrate: 115200, GroupTopic: 'tasmotas', OtaUrl: 'http://ota.tasmota.com/tasmota/release/tasmota.bin.gz',
          RestartReason: d.name === 'tuin' ? 'Hardware Watchdog' : d.name === 'keuken' ? 'Software/System restart' : 'Power On',
          BootCount: d.name === 'tuin' ? 41 : d.name === 'keuken' ? 9 : 17 } };
      }
      if (arg === '10') {
        // sensors (temperature, humidity) with a small drift, like a real sensor
        const sns = { Time: localIso() };
        Object.keys(d.sns || {}).forEach(k => { const o = {}; Object.keys(d.sns[k]).forEach(f => { o[f] = +(d.sns[k][f] + (Math.random() - .5) * (f === 'Humidity' ? 2 : .2)).toFixed(1); }); sns[k] = o; });
        if (d.sns) sns.TempUnit = 'C';
        return { StatusSNS: sns };
      }
      return status(d);
    }
    if (key === 'time') return { Time: localIso() };
    if (key === 'pulsetime1' || key === 'pulsetime') {
      if (arg !== '') { d.pulse = pulseSec(arg); d.offAt = d.on && d.pulse ? Date.now() + d.pulse * 1000 : null; }
      const rem = d.offAt ? Math.max(0, (d.offAt - Date.now()) / 1000) : 0;
      return { PulseTime1: { Set: secPulse(d.pulse || 0), Remaining: secPulse(rem) } };
    }
    if (key === 'timers') return Object.assign({ Timers: 'ON' }, d.timers);
    const tm = key.match(/^timer(\d+)$/);
    if (tm) {
      const k = 'Timer' + tm[1];
      if (arg) { try { d.timers[k] = Object.assign({}, d.timers[k], JSON.parse(arg)); } catch (e) { return { Command: 'Error' }; } }
      return { [k]: d.timers[k] || { Enable: 0 } };
    }
    if (key === 'friendlyname1' || key === 'friendlyname') { if (arg) d.friendly = arg; return { FriendlyName1: d.friendly }; }
    if (key === 'devicename') { if (arg) d.name = arg; return { DeviceName: d.name }; }
    if (key === 'latitude') { if (arg) d.lat = parseFloat(arg); return { Latitude: d.lat }; }
    if (key === 'longitude') { if (arg) d.lon = parseFloat(arg); return { Longitude: d.lon }; }
    return { Command: 'Unknown' };
  }

  // Same callback format as the Java bridge: __tasCb(id, ok, base64(utf8 text))
  // Shellys in the demo (Gen2+ RPC API): a Plus 1PM switch, a Duo Bulb Gen3 (white) and a Multicolor Bulb Gen3
  const shellies = {
    [SUBNET + '150']: { on: false, comp: 'switch', app: 'Plus1PM', model: 'SNSW-001P16EU', gen: 2, id: 'shellyplus1pm-a8032ab1c2d4', mac: 'A8032AB1C2D4' },
    [SUBNET + '151']: { on: false, comp: 'cct', app: 'DuoBulbG3', model: 'S3BL-D010009AEU', gen: 3, id: 'shellyduobulbg3-e4b063b1a2c4', mac: 'E4B063B1A2C4', bri: 70, ct: 3000 },
    [SUBNET + '152']: { on: false, comp: 'rgbcct', app: 'RGBCCTBulbG3', model: 'S3BL-C010007AEU', gen: 3, id: 'shellyrgbcctbulbg3-e4b063b1d7f0', mac: 'E4B063B1D7F0', bri: 80, ct: 4000, rgb: [255, 80, 0], mode: 'rgb' },
    // Shelly 1 Gen3 with the Sensor Add-on + 1 DS18B20, the thermostat script already installed (runs here in the browser)
    [SUBNET + '153']: { on: false, comp: 'switch', app: 'S1G3', model: 'S3SW-001X16EU', gen: 3, id: 'shelly1g3-ecda3bc1e2f0', mac: 'ECDA3BC1E2F0', name: 'Living room', room: 19.6, thermo: true, radio: { ap: false, open: false, ble: false } },
  };
  const NS = { switch: 'Switch', cct: 'CCT', rgbcct: 'RGBCCT' };
  Object.values(shellies).forEach(sh => Object.assign(sh, { kvs: {}, jobs: [], nextJob: 1, rev: 1, ver: '1.7.1', loc: { lat: 52.37, lon: 4.9, tz: 'Europe/Amsterdam' }, scripts: {}, nextScript: 1, handlers: [] }));
  const ipOf = sh => Object.keys(shellies).find(ip => shellies[ip] === sh);
  const r1 = x => Math.round(x * 10) / 10;

  // ---- A tiny Shelly script runtime: the real thermostat script (thermostat-script.js) runs in this page ----
  function startScript(sh, sc) {
    stopScript(sc); sc.ep = {}; sc.tm = [];
    const call = (m, a, cb) => {
      const u = new URL('http://' + ipOf(sh) + '/rpc/' + m);
      Object.keys(a || {}).forEach(k => u.searchParams.set(k, JSON.stringify(a[k])));
      const res = shellyApi(sh, u);
      setTimeout(() => { if (cb) { if (res && res.__err) cb(null, -1, res.__err); else cb(res, 0, ''); } }, 20);
    };
    const comp = (k, id) => {
      if (k === 'sys') { const d = new Date(); return { uptime: Math.floor((Date.now() - T0) / 1000) + 86400, time: pad(d.getHours()) + ':' + pad(d.getMinutes()), unixtime: Math.floor(Date.now() / 1000) }; }
      if (k === 'wifi') return { sta_ip: ipOf(sh), status: 'got ip' };
      if (k === 'switch') return { id: 0, output: sh.on };
      if (k === 'temperature') return sh.room != null && +id === 100 ? { id: 100, tC: r1(sh.room) } : null;
      return null;
    };
    const env = {
      Shelly: { call, getComponentStatus: comp, getCurrentScriptId: () => sc.id, addStatusHandler: f => { sh.handlers.push(f); sc.hs = (sc.hs || []).concat(f); } },
      Timer: { set: (ms, rep, f) => { const h = rep ? setInterval(f, ms) : setTimeout(f, ms); sc.tm.push(h); return h; }, clear: h => { clearInterval(h); clearTimeout(h); } },
      HTTPServer: { registerEndpoint: (n, f) => { sc.ep[n] = f; } },
      console: { log: () => {} }
    };
    try { new Function('Shelly', 'Timer', 'HTTPServer', 'console', sc.code)(env.Shelly, env.Timer, env.HTTPServer, env.console); sc.running = true; }
    catch (e) { sc.running = false; console.warn('demo script error', e); }
  }
  function stopScript(sc) {
    (sc.tm || []).forEach(h => { clearInterval(h); clearTimeout(h); }); sc.tm = []; sc.ep = {}; sc.running = false;
    Object.values(shellies).forEach(sh => { sh.handlers = sh.handlers.filter(f => !(sc.hs || []).includes(f)); }); sc.hs = [];
  }
  const T0 = Date.now();
  // install the thermostat once the page has the script code (option A: it is there from the start)
  function ensureThermo(sh) {
    if (!sh.thermo || sh.thInstalled || !window.SH_THERMO_CODE) return;
    sh.thInstalled = true;
    Object.assign(sh.kvs, { th_comfort: 21, th_eco: 17, th_level: 'comfort', th_mode: 'on', th_test: 'off', th_sensor: 100 });
    const sc = sh.scripts[1] = { id: 1, name: 'Thermostat (Scan & Control)', code: window.SH_THERMO_CODE, enable: true, running: false };
    sh.nextScript = 2; startScript(sh, sc);
  }
  document.addEventListener('DOMContentLoaded', () => Object.values(shellies).forEach(ensureThermo));
  // a link to a screen: wait until the app (and for the Heating screen the thermostat) is ready, then open it
  if (/^#(thermostat|tiles|scenes)$/i.test(location.hash || '')) window.addEventListener('load', () => {
    const want = location.hash.slice(1).toLowerCase(); let n = 0;
    const t = setInterval(() => {
      n++;
      const ready = typeof window.__openLink === 'function' && (want !== 'thermostat' ||
        (typeof devThermo === 'object' && Object.values(devThermo).some(x => x && !x.err)));
      if (!ready && n < 40) return;
      clearInterval(t);
      try { if (typeof closeSheet === 'function') closeSheet(true); } catch (e) {}
      window.__openLink();
    }, 300);
  });
  // the room: warms up while the heating is on, cools down slowly when it is off; the sensor reports every 0.1 °C
  setInterval(() => {
    Object.values(shellies).forEach(sh => {
      if (sh.room == null) return;
      if (sh.tgAt && Date.now() / 1000 >= sh.tgAt) { sh.on = !sh.on; sh.tgAt = null; }   // watchdog: toggle_after ran out
      const before = r1(sh.room);
      sh.room = Math.max(14, Math.min(27, sh.room + (sh.on ? 0.1 : -0.04)));
      const now = r1(sh.room);
      if (now !== before) sh.handlers.slice().forEach(f => { try { f({ component: 'temperature:100', id: 100, delta: { id: 100, tC: now } }); } catch (e) {} });
    });
  }, 5000);
  shellies[SUBNET + '152'].errors = ['overtemp'];   // demo: the colour bulb shows a warning
  function shellyApi(sh, u) {
    const p = u.pathname, q = k => u.searchParams.get(k), js = k => { try { return JSON.parse(q(k)); } catch (e) { return q(k); } };
    const ns = NS[sh.comp];
    if (sh.offAt && Date.now() / 1000 >= sh.offAt) { sh.on = false; sh.offAt = null; }   // auto-off reached
    const startTimer = () => { sh.offAt = sh.on && sh.autoOff ? Date.now() / 1000 + sh.autoOff : null; sh.started = sh.offAt ? Date.now() / 1000 : null; };
    const status = () => Object.assign({ id: 0, source: 'init', output: sh.on },
      sh.comp === 'switch' ? { apower: sh.on ? 18.2 : 0, voltage: 231.4, temperature: { tC: 41.2 } }
        : { brightness: sh.bri, ct: sh.ct, apower: sh.on ? +(sh.bri * 0.09).toFixed(1) : 0, temperature: { tC: sh.comp === 'cct' ? 47.5 : 52.1 } },
      sh.errors ? { errors: sh.errors } : {},
      sh.comp === 'rgbcct' ? { mode: sh.mode, rgb: sh.rgb } : {},
      sh.offAt ? { timer_started_at: sh.started, timer_duration: sh.autoOff } : {},
      sh.tgAt ? { timer_started_at: sh.tgStart, timer_duration: sh.tgDur } : {});
    if (p === '/shelly') return { name: sh.name || null, id: sh.id, mac: sh.mac, model: sh.model, gen: sh.gen, fw_id: '20250924-062743/1.7.1-gd336f31', ver: '1.7.1', app: sh.app, auth_en: false };
    if (p === '/rpc/Shelly.GetDeviceInfo') return { name: sh.name || null, id: sh.id, mac: sh.mac, model: sh.model, gen: sh.gen, fw_id: '20250924-062743/' + sh.ver + '-gd336f31', ver: sh.ver, app: sh.app, auth_en: false };
    if (p === '/rpc/Shelly.Update') { sh.updating = Date.now(); setTimeout(() => { sh.ver = '1.7.2'; sh.updating = 0; }, 4000); return null; }
    ensureThermo(sh);
    if (sh.tgAt && Date.now() / 1000 >= sh.tgAt) { sh.on = !sh.on; sh.tgAt = null; }
    const sp = p.match(/^\/script\/(\d+)\/(\w+)$/);
    if (sp) {
      const sc = sh.scripts[sp[1]], f = sc && sc.running && sc.ep[sp[2]];
      if (!f) return { __err: 'HTTP 404 Not Found' };
      const res = { code: 200, body: '', send() { this.sent = true; } }; f({ query: '' }, res);
      try { return JSON.parse(res.body); } catch (e) { return { __raw: res.body }; }
    }
    if (p === '/rpc/Script.List') return { scripts: Object.values(sh.scripts).map(x => ({ id: x.id, name: x.name, enable: x.enable, running: x.running })) };
    if (p === '/rpc/Script.Create') { const id = sh.nextScript++; sh.scripts[id] = { id, name: js('name') || 'script_' + id, code: '', enable: false, running: false }; return { id }; }
    if (p === '/rpc/Script.PutCode') { const x = sh.scripts[js('id')]; if (!x) return { __err: 'HTTP 500 {"code":-105,"message":"Script not found"}' }; x.code = js('append') ? x.code + js('code') : js('code'); return { len: x.code.length }; }
    if (p === '/rpc/Script.SetConfig') { const x = sh.scripts[js('id')]; if (x) { const c = js('config') || {}; if ('enable' in c) x.enable = !!c.enable; if (c.name) x.name = c.name; } return { restart_required: false }; }
    if (p === '/rpc/Script.Start') { const x = sh.scripts[js('id')]; if (!x) return { __err: 'HTTP 500 {"code":-105,"message":"Script not found"}' }; const was = x.running; startScript(sh, x); return { was_running: was }; }
    if (p === '/rpc/Script.Stop') { const x = sh.scripts[js('id')]; if (x) { const was = x.running; stopScript(x); return { was_running: was }; } return { __err: 'HTTP 500 {"code":-105,"message":"Script not found"}' }; }
    if (p === '/rpc/Script.Delete') { const x = sh.scripts[js('id')]; if (x) { stopScript(x); delete sh.scripts[js('id')]; } return null; }
    if (p === '/rpc/KVS.Delete') { delete sh.kvs[js('key')]; return { rev: ++sh.rev }; }
    if (p === '/rpc/Shelly.GetStatus' && sh.room != null) return { sys: { uptime: 86400 * 3 + 5123, available_updates: sh.ver === '1.7.2' ? {} : { stable: { version: '1.7.2' } } }, wifi: { sta_ip: u.hostname, status: 'got ip', ssid: 'Demo-WiFi', rssi: -55 },
      mqtt: { connected: !!(sh.mqtt && sh.mqtt.enable && sh.mqtt.connected) }, 'switch:0': status(), 'temperature:100': { id: 100, tC: r1(sh.room) } };
    if (p === '/rpc/Shelly.GetStatus') return { sys: { uptime: 86400 * 3 + 5123, available_updates: sh.ver === '1.7.2' ? {} : { stable: { version: '1.7.2' } } }, wifi: { sta_ip: u.hostname, status: 'got ip', ssid: 'Demo-WiFi', rssi: -58 }, mqtt: { connected: !!(sh.mqtt && sh.mqtt.enable && sh.mqtt.connected) }, [sh.comp + ':0']: status() };
    if (p === '/rpc/Sys.SetConfig') { try { const c = JSON.parse(q('config') || '{}'); if (c.device && 'name' in c.device) sh.name = c.device.name; if (c.location) Object.assign(sh.loc, c.location); } catch (e) {} return { restart_required: false }; }
    if (!sh.radio) sh.radio = sh.comp === 'switch' ? { ap: true, open: true, ble: true } : { ap: false, open: false, ble: true };
    if (!sh.mqtt) sh.mqtt = sh.comp === 'switch' ? { enable: true, server: '10.10.100.2:1883', connected: true } : sh.comp === 'rgbcct' ? { enable: true, server: '10.10.100.9:1883', connected: false } : { enable: false, server: null, connected: false };
    if (p === '/rpc/Shelly.GetConfig' && sh.room != null) return { ble: { enable: sh.radio.ble, rpc: { enable: sh.radio.ble } }, wifi: { ap: { ssid: 'Shelly-' + sh.mac, is_open: sh.radio.open, enable: sh.radio.ap }, sta: { ssid: 'Demo-WiFi', enable: true } },
      sys: { device: { name: sh.name || null } }, mqtt: { enable: sh.mqtt.enable, server: sh.mqtt.server, client_id: sh.id }, 'temperature:100': { id: 100, name: 'Living room' } };
    if (p === '/rpc/Shelly.GetConfig') return { ble: sh.comp === 'switch' ? { rpc: { enable: sh.radio.ble } } : { enable: sh.radio.ble, rpc: { enable: true } }, wifi: { ap: { ssid: 'Shelly-' + sh.mac, is_open: sh.radio.open, enable: sh.radio.ap }, sta: { ssid: 'Demo-WiFi', enable: true } }, sys: { device: { name: sh.name || null } }, mqtt: { enable: sh.mqtt.enable, server: sh.mqtt.server, client_id: sh.id } };
    if (p === '/rpc/Wifi.SetConfig') { try { const c = JSON.parse(q('config') || '{}'); if (c.ap && 'enable' in c.ap) sh.radio.ap = !!c.ap.enable; } catch (e) {} return { restart_required: false }; }
    if (p === '/rpc/BLE.SetConfig') { try { const c = JSON.parse(q('config') || '{}'); if ('enable' in c) sh.radio.ble = !!c.enable; if (c.rpc && 'enable' in c.rpc) sh.radio.ble = !!c.rpc.enable; } catch (e) {} return { restart_required: true }; }
    if (p === '/rpc/Sys.GetConfig') return { device: { name: sh.name || null, mac: sh.mac }, location: Object.assign({}, sh.loc) };
    if (p === '/rpc/Sys.GetStatus') return { mac: sh.mac, unixtime: Math.floor(Date.now() / 1000), time: new Date().toTimeString().slice(0, 5), uptime: 86400 * 3 + 5123 };
    // KVS and Schedule (schedules from the app are stored as jobs, like on a real Gen2+ Shelly)
    if (p === '/rpc/KVS.Get') { const k = js('key'); return k in sh.kvs ? { etag: 'x', value: sh.kvs[k] } : { __err: 'HTTP 500 {"code":-105,"message":"Argument key not found"}' }; }
    // like Shelly firmware 1.4: a string value from a GET parameter is stored without unescaping \" inside it
    if (p === '/rpc/KVS.Set') { const raw = String(q('value') || ''); sh.kvs[js('key')] = /^".*"$/.test(raw) ? raw.slice(1, -1) : js('value'); return { etag: 'x', rev: ++sh.rev }; }
    if (p === '/rpc/KVS.GetMany') { const m = new RegExp('^' + String(js('match') || '*').replace(/[.]/g, '\\.').replace(/\*/g, '.*') + '$'); return { items: Object.keys(sh.kvs).filter(k => m.test(k)).map(k => ({ key: k, etag: 'x', value: sh.kvs[k] })), offset: 0, total: 0 }; }
    if (p === '/rpc/Schedule.List') return { jobs: sh.jobs, rev: sh.rev };
    if (p === '/rpc/Schedule.Create') { if (sh.jobs.length >= 20) return { __err: 'HTTP 500 {"code":-103,"message":"Too many jobs"}' }; const j = { id: sh.nextJob++, enable: js('enable') !== false, timespec: js('timespec'), calls: js('calls') }; sh.jobs.push(j); return { id: j.id, rev: ++sh.rev }; }
    if (p === '/rpc/Schedule.Update') { const j = sh.jobs.find(x => x.id === +js('id')); if (!j) return { __err: 'HTTP 500 {"code":-105,"message":"Job not found"}' }; ['enable', 'timespec', 'calls'].forEach(k => { if (q(k) != null) j[k] = js(k); }); return { rev: ++sh.rev }; }
    if (p === '/rpc/Schedule.Delete') { const i = sh.jobs.findIndex(x => x.id === +js('id')); if (i < 0) return { __err: 'HTTP 500 {"code":-105,"message":"Job not found"}' }; sh.jobs.splice(i, 1); return { rev: ++sh.rev }; }
    if (p === `/rpc/${ns}.GetConfig`) return Object.assign({ id: 0, name: null, auto_on: false, auto_off: !!sh.autoOff, auto_off_delay: sh.autoOff || 60 }, ns === 'Switch' ? { in_mode: sh.inMode || 'follow' } : {});
    if (p === `/rpc/${ns}.SetConfig`) { try { const c = JSON.parse(q('config') || '{}'); if ('auto_off' in c) sh.autoOff = c.auto_off ? +c.auto_off_delay || 0 : 0; if (c.in_mode) sh.inMode = c.in_mode; } catch (e) {} startTimer(); return { restart_required: false }; }
    if (p === '/rpc/Input.GetConfig') return { id: 0, name: null, type: sh.inType || 'switch', enable: true, invert: false };
    if (p === '/rpc/Input.SetConfig') { try { const c = JSON.parse(q('config') || '{}'); if (c.type) sh.inType = c.type; } catch (e) {} return { restart_required: false }; }
    if (p === `/rpc/${ns}.GetStatus`) return status();
    if (p === `/rpc/${ns}.Set`) {
      const was = sh.on;
      if (q('on') != null) sh.on = q('on') === 'true';
      const ta = +js('toggle_after') || 0;   // the thermostat's watchdog: switch back after ta seconds
      if (q('on') != null) { sh.tgAt = ta ? Date.now() / 1000 + ta : null; sh.tgStart = ta ? Date.now() / 1000 : null; sh.tgDur = ta || null; }
      if (q('brightness') != null) sh.bri = Math.max(0, Math.min(100, +q('brightness')));
      if (q('ct') != null) sh.ct = Math.max(2700, Math.min(6500, +q('ct')));
      if (q('rgb') != null) sh.rgb = js('rgb');
      if (q('mode') != null) sh.mode = js('mode');
      startTimer(); return sh.comp === 'switch' ? { was_on: was } : null;
    }
    if (p === `/rpc/${ns}.Toggle`) { const was = sh.on; sh.on = !sh.on; startTimer(); return { was_on: was }; }
    return { code: 404, message: 'No handler for ' + p };
  }
  function reply(id, ok, text, delay) {
    const b64 = btoa(unescape(encodeURIComponent(text)));
    setTimeout(() => window.__tasCb && window.__tasCb(id, ok, b64), delay == null ? 150 + Math.random() * 250 : delay);
  }

  function inRange(range, ip) {
    const r = (range || '').trim().toLowerCase();
    if (!r || r === 'auto') return ip.startsWith(SUBNET);
    const m = r.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})(?:\.(?:x|\*|0\/24|(\d{1,3})(?:\s*-\s*(\d{1,3}))?))?(?:\/24)?$/);
    if (!m) return null;
    const pre = `${m[1]}.${m[2]}.${m[3]}.`, last = +ip.split('.')[3];
    const from = m[4] ? +m[4] : 1, to = m[5] ? +m[5] : (m[4] ? +m[4] : 254);
    return ip.startsWith(pre) && last >= from && last <= to;
  }

  // for recordings: make a running auto-off end in a few seconds
  window.__demoPulseSoon = (ip, sec) => { const d = devices[ip]; if (d && d.offAt) d.offAt = Date.now() + sec * 1000; };

  window.Tasmota = {
    get(url, id) {
      let u; try { u = new URL(url, 'http://' + SUBNET + '114/'); } catch (e) { return reply(id, false, 'java.net.MalformedURLException'); }
      const sh = shellies[u.hostname];
      if (sh) { const r = shellyApi(sh, u); return r && r.__err ? reply(id, false, r.__err) : reply(id, true, r && r.__raw != null ? r.__raw : JSON.stringify(r)); }
      const d = devices[u.hostname];
      if (!d) return reply(id, false, `java.net.SocketTimeoutException: failed to connect to /${u.hostname}`, 1500);
      if (d.protected && u.searchParams.get('password') !== 'demo')
        return reply(id, false, 'HTTP 401 {"WARNING":"Need user=<username>&password=<password>"}');
      reply(id, true, JSON.stringify(command(d, u.searchParams.get('cmnd') || '')));
    },
    pendingLink() {   // demo/#thermostat, #tiles, #scenes: open that screen (like tasmotascan:// in the app)
      const h = (location.hash || '').slice(1).toLowerCase();
      if (!['thermostat', 'tiles', 'scenes'].includes(h)) return '';
      try { history.replaceState(null, '', location.pathname); } catch (e) {}
      return h;
    },
    post(url, body, id) {   // Shelly RPC with a JSON body (the app uploads the thermostat script this way)
      let u; try { u = new URL(url); } catch (e) { return reply(id, false, 'java.net.MalformedURLException'); }
      const sh = shellies[u.hostname];
      if (!sh) return reply(id, false, `java.net.SocketTimeoutException: failed to connect to /${u.hostname}`, 1500);
      let b = {}; try { b = JSON.parse(body || '{}'); } catch (e) {}
      Object.keys(b).forEach(k => u.searchParams.set(k, JSON.stringify(b[k])));
      const r = shellyApi(sh, u); return r && r.__err ? reply(id, false, r.__err) : reply(id, true, JSON.stringify(r));
    },
    scan(id) { this.scanRange('', id); },
    scanRange(range, id) {
      const ips = Object.keys(devices);
      if (ips.some(ip => inRange(range, ip) === null)) return reply(id, false, 'Invalid range: ' + range, 50);
      const found = ips.filter(ip => inRange(range, ip)).map(ip => {
        const d = devices[ip];
        return d.protected ? { ip, protected: true } : { ip, name: d.name, topic: d.topic, friendly: d.friendly, power: d.on ? '1' : '0' };
      }).concat(Object.keys(shellies).filter(ip => inRange(range, ip)).map(ip => {
        const sh = shellies[ip];
        return { ip, kind: 'shelly', gen: sh.gen, comp: sh.comp, protected: false, model: sh.app, name: sh.id, friendly: sh.name || '', mac: sh.mac.replace(/(..)(?!$)/g, '$1:') };
      }));
      const r = (range || '').trim();
      reply(id, true, JSON.stringify({ subnet: r && r !== 'auto' ? r : SUBNET + '0/24', devices: found }), 1800);
    },
    wifiInfo() { return JSON.stringify({ ssid: 'Demo-WiFi', ip: SUBNET + '23', status: 'ok', perm: true, loc: true }); },
    location(id) { reply(id, true, JSON.stringify({ lat: 52.37, lon: 4.89, acc: 25 }), 600); },
    appInfo() { return JSON.stringify({ version: 'demo', code: 0, date: 'browser demo (simulated switches)' }); },
    openUrl(url) {
      const lm = url.match(/^http:\/\/([\d.]+)\/script\/(\d+)\/log$/), sh0 = lm && shellies[lm[1]];
      if (sh0) {   // the thermostat log: shown from the script running in this page
        const r = shellyApi(sh0, new URL(url)), txt = r && r.__raw != null ? r.__raw : 'No log: the script is not running.';
        const w = window.open('', '_blank');
        if (w) { w.document.title = 'Thermostat log (demo)'; const pre = w.document.createElement('pre'); pre.textContent = txt;
          pre.style.cssText = 'font:13px/1.45 monospace;white-space:pre-wrap;margin:10px;color:#ddd'; w.document.body.style.background = '#111'; w.document.body.appendChild(pre); }
        else demoNote('The log page could not open (pop-up blocked).');
        return;
      }
      if (/\/#\/script\/\d+$/.test(url)) return demoNote('In the app this opens the script page on the Shelly itself, where you can see and stop the script. Not available in the demo.');
      if (/^https:\/\//.test(url)) window.open(url, '_blank', 'noopener');
      else demoNote('In the app this opens the switch\'s own Tasmota web page. Not available in the demo.');
    },
    openWifi() { demoNote('In the app this opens the Android Wi-Fi panel.'); },
    fixName() { demoNote('In the app this asks for location permission (needed to read the Wi-Fi name).'); }
  };

  function demoNote(t) { alert('Demo: ' + t); }

  // The page shows an "https blocks the switch" warning; in the demo that is not true,
  // so swap it for a demo banner once the page has loaded.
  document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('main > .card').forEach(c => {
      if (/running over https/i.test(c.textContent)) c.remove();
    });
    const main = document.querySelector('main');
    if (main) main.insertAdjacentHTML('afterbegin',
      '<div class="card" style="border-color:var(--ok, #2a7);">' +
      'Demo</b> — simulated switches, nothing is sent to a real network. <br>' +                               
      '<b>Some options only show in the app with real Tasmota devices. <br>' +
      'in App Tap <i>Scan network</i> to find Tasmota Devices. <br>The protected one uses password <code>demo</code>. <br>' +
      '<a href="https://github.com/ldijkman/Tasmota-android-test/releases/latest" style="display:block;text-align:center;margin-top:8px;color:#3ddc84;font-weight:700">Get the Android app</a>' +
      '<a href="../#videos" style="display:block;text-align:center;margin-top:6px;color:#f0a630;font-weight:700">A video says more than a thousand words ▶</a>' +
      '<a href="#" onclick="event.preventDefault();demoWhy()" style="display:block;text-align:center;margin-top:6px;color:#b48cff;font-weight:700">Why free?</a>' +
      '<br><button type="button" class="b-tog" style="margin-top:10px" onclick="demoReset()">Reset demo</button></div>');
    // also in the hamburger menu
    const menu = document.getElementById('menu');
    if (menu) menu.insertAdjacentHTML('beforeend',
      '<button role="menuitem" onclick="demoReset()">' +
      '<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5"/></svg>' +
      'Reset demo</button>' +
      '<button role="menuitem" onclick="demoWhy()">' +
      '<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M12 21s-7-4.5-9.5-9A5.5 5.5 0 0 1 12 6a5.5 5.5 0 0 1 9.5 6c-2.5 4.5-9.5 9-9.5 9z"/></svg>' +
      'Why free?</button>');
  });

  // Share button at the bottom of the Heating screen (demo only)
  window.demoShareThermo = function () {
    const url = 'https://ldijkman.github.io/Tasmota-android-test/demo/#thermostat';
    const text = 'Try this thermostat in your browser: a Shelly 1 Gen3 with a DS18B20, free Android app, no cloud.';
    if (navigator.share) { navigator.share({ title: 'Live demo thermostat', text, url }).catch(() => {}); return; }
    const ok = () => { if (typeof showToast === 'function') showToast('Link copied'); };
    (navigator.clipboard ? navigator.clipboard.writeText(url) : Promise.reject()).then(ok, () => { window.prompt('Copy this link:', url); });
  };
  document.addEventListener('DOMContentLoaded', () => {
    const body = document.getElementById('thermoBody'); if (!body || document.getElementById('demoShareTh')) return;
    const w = document.createElement('div'); w.id = 'demoShareTh';
    w.style.cssText = 'text-align:center;padding:10px 16px 18px';
    w.innerHTML = '<button type="button" onclick="demoShareThermo()" style="display:inline-flex;align-items:center;gap:8px;height:46px;padding:0 22px;border-radius:14px;border:1.5px solid #f0a630;background:transparent;color:#f0a630;font:inherit;font-weight:700">' +
      '<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M18 16.1c-.8 0-1.5.3-2 .8l-7.1-4.2c.1-.2.1-.5.1-.7s0-.5-.1-.7L16 7.2c.5.5 1.2.8 2 .8 1.7 0 3-1.3 3-3s-1.3-3-3-3-3 1.3-3 3c0 .2 0 .5.1.7L8 9.8C7.5 9.3 6.8 9 6 9c-1.7 0-3 1.3-3 3s1.3 3 3 3c.8 0 1.5-.3 2-.8l7.1 4.2c-.1.2-.1.4-.1.6 0 1.6 1.3 2.9 2.9 2.9s2.9-1.3 2.9-2.9-1.2-2.9-2.8-2.9z"/></svg>' +
      'Share this demo</button>';
    body.insertAdjacentElement('afterend', w);
  });

  // "Why free?" (same text as on the website)
  window.demoWhy = function () {
    let d = document.getElementById('demoWhyDlg');
    if (!d) {
      d = document.createElement('dialog'); d.id = 'demoWhyDlg';
      d.style.cssText = 'max-width:440px;width:calc(100% - 32px);border:1px solid var(--line,#333);border-radius:16px;padding:20px 20px 14px;background:var(--card,#fff);color:var(--ink,#111);line-height:1.5';
      d.innerHTML = '<button type="button" aria-label="Close" style="position:absolute;top:8px;right:10px;width:40px;height:40px;border:0;background:transparent;color:inherit;font-size:1.2rem" onclick="this.closest(\'dialog\').close()">✕</button>' +
        '<h3 style="margin:0 0 8px">Why free?</h3>' +
        '<p>I made this app for my own home, for fun. I’m a handy guy with the lowest possible education, and I like home automation that just works. If it helps you too, that makes me happy.</p>' +
        '<h4 style="margin:12px 0 2px">Why no tracking?</h4><p style="margin:0">The app talks to your own switches on your own Wi-Fi. No analytics: nothing about you or your home is sent anywhere. It only goes online to check GitHub for a new version, and for icon search if you use it.</p>' +
        '<h4 style="margin:12px 0 2px">Why no account?</h4><p style="margin:0">There is no server to log in to. Your switches, your phone, your home.</p>' +
        '<h4 style="margin:12px 0 2px">Why no ads?</h4><p style="margin:0">Ads bring tracking and clutter. The app stays small and quiet.</p>' +
        '<p style="margin:16px 0 4px;text-align:center">Like the app? <a href="https://paypal.me/ldijkman" target="_blank" rel="noopener" style="color:#f0a630;font-weight:700">☕ Buy me a coffee</a><br><small style="opacity:.7">Never required, always appreciated.</small></p>' +
        '<p style="margin:10px 0 4px;text-align:center"><a href="https://youtu.be/AmGVAu9lk6I" target="_blank" rel="noopener" style="color:#b48cff">Money didn’t matter yesterday</a></p>' +
        '<p style="margin:10px 0 0;text-align:right;font-style:italic">Lubèrth</p>';
      d.addEventListener('click', e => { if (e.target === d) d.close(); });
      document.body.appendChild(d);
    }
    const m = document.getElementById('menu'); if (m && !m.hidden && typeof closeMenu === 'function') try { closeMenu(); } catch (e) {}
    d.showModal();
  };

  // Forget everything the demo saved in this browser (only this app's own settings, keys starting with "tas_")
  window.demoReset = function () {
    if (!confirm('Reset the demo?\n\nThis forgets all switches, groups, scenes, icons and settings you changed in this demo.')) return;
    clearDemo();
    try { sessionStorage.clear(); sessionStorage.setItem('demo_reset', '1'); } catch (e) {}
    location.reload();
  };
})();

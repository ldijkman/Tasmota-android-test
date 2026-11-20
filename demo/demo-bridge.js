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
  };
  const NS = { switch: 'Switch', cct: 'CCT', rgbcct: 'RGBCCT' };
  Object.values(shellies).forEach(sh => Object.assign(sh, { kvs: {}, jobs: [], nextJob: 1, rev: 1, ver: '1.7.1', loc: { lat: 52.37, lon: 4.9, tz: 'Europe/Amsterdam' } }));
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
      sh.offAt ? { timer_started_at: sh.started, timer_duration: sh.autoOff } : {});
    if (p === '/shelly') return { name: sh.name || null, id: sh.id, mac: sh.mac, model: sh.model, gen: sh.gen, fw_id: '20250924-062743/1.7.1-gd336f31', ver: '1.7.1', app: sh.app, auth_en: false };
    if (p === '/rpc/Shelly.GetDeviceInfo') return { name: sh.name || null, id: sh.id, mac: sh.mac, model: sh.model, gen: sh.gen, fw_id: '20250924-062743/' + sh.ver + '-gd336f31', ver: sh.ver, app: sh.app, auth_en: false };
    if (p === '/rpc/Shelly.Update') { sh.updating = Date.now(); setTimeout(() => { sh.ver = '1.7.2'; sh.updating = 0; }, 4000); return null; }
    if (p === '/rpc/Shelly.GetStatus') return { sys: { uptime: 86400 * 3 + 5123, available_updates: sh.ver === '1.7.2' ? {} : { stable: { version: '1.7.2' } } }, wifi: { sta_ip: u.hostname, status: 'got ip', ssid: 'Demo-WiFi', rssi: -58 }, [sh.comp + ':0']: status() };
    if (p === '/rpc/Sys.SetConfig') { try { const c = JSON.parse(q('config') || '{}'); if (c.device && 'name' in c.device) sh.name = c.device.name; if (c.location) Object.assign(sh.loc, c.location); } catch (e) {} return { restart_required: false }; }
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
    if (p === `/rpc/${ns}.GetConfig`) return { id: 0, name: null, auto_on: false, auto_off: !!sh.autoOff, auto_off_delay: sh.autoOff || 60 };
    if (p === `/rpc/${ns}.SetConfig`) { try { const c = JSON.parse(q('config') || '{}'); sh.autoOff = c.auto_off ? +c.auto_off_delay || 0 : 0; } catch (e) {} startTimer(); return { restart_required: false }; }
    if (p === `/rpc/${ns}.GetStatus`) return status();
    if (p === `/rpc/${ns}.Set`) {
      const was = sh.on;
      if (q('on') != null) sh.on = q('on') === 'true';
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
      if (sh) { const r = shellyApi(sh, u); return r && r.__err ? reply(id, false, r.__err) : reply(id, true, JSON.stringify(r)); }
      const d = devices[u.hostname];
      if (!d) return reply(id, false, `java.net.SocketTimeoutException: failed to connect to /${u.hostname}`, 1500);
      if (d.protected && u.searchParams.get('password') !== 'demo')
        return reply(id, false, 'HTTP 401 {"WARNING":"Need user=<username>&password=<password>"}');
      reply(id, true, JSON.stringify(command(d, u.searchParams.get('cmnd') || '')));
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
      '<br><br><b>Demo</b> — simulated switches, nothing is sent to a real network. <br>' +                               
      '<b>Some options only show in the app with real Tasmota devices. <br>' +
      'in App Tap <i>Scan network</i> to find Tasmota Devices. <br>The protected one uses password <code>demo</code>. <br>' +
      '<a href="https://github.com/ldijkman/Tasmota-android-test/releases/latest">Get the Android app</a>' +
      '<br><button type="button" class="b-tog" style="margin-top:10px" onclick="demoReset()">Reset demo</button></div>');
    // also in the hamburger menu
    const menu = document.getElementById('menu');
    if (menu) menu.insertAdjacentHTML('beforeend',
      '<button role="menuitem" onclick="demoReset()">' +
      '<svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" d="M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5"/></svg>' +
      'Reset demo</button>');
  });

  // Forget everything the demo saved in this browser (only this app's own settings, keys starting with "tas_")
  window.demoReset = function () {
    if (!confirm('Reset the demo?\n\nThis forgets all switches, groups, scenes, icons and settings you changed in this demo.')) return;
    clearDemo();
    try { sessionStorage.clear(); sessionStorage.setItem('demo_reset', '1'); } catch (e) {}
    location.reload();
  };
})();

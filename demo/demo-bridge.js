// Demo bridge for GitHub Pages.
// The Android app gives the page a `window.Tasmota` object (see MainActivity.java -> Bridge).
// This file fakes that object with a few simulated Tasmota switches, so the real
// index.html runs unchanged in any browser. Nothing here talks to a real network.
(function () {
  'use strict';

  const SUBNET = '10.10.100.';
  const devices = {
    '10.10.100.114': dev('plafond-led', 'Plafond LED', true),
    '10.10.100.115': dev('keuken', 'Kitchen', false),
    '10.10.100.120': dev('tuin', 'Garden lights', false),
    '10.10.100.131': dev('bureau', 'Desk lamp', true),
    '10.10.100.140': { protected: true }            // switch with a web password
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
  function command(d, cmnd) {
    const m = cmnd.match(/^(\S+)\s*(.*)$/s) || [];
    const key = (m[1] || '').toLowerCase(), arg = (m[2] || '').trim();
    const power = () => ({ POWER: d.on ? 'ON' : 'OFF' });

    if (key === 'power' || key === 'power1') {
      const a = arg.toLowerCase();
      if (a === 'on' || a === '1') d.on = true;
      else if (a === 'off' || a === '0') d.on = false;
      else if (a === 'toggle' || a === '2') d.on = !d.on;
      return power();
    }
    if (key === 'status') {
      if (arg === '7') {
        const s = sunTimes(d.lat, d.lon);
        return { StatusTIM: { UTC: new Date().toISOString().slice(0, 19), Local: localIso(), Timezone: 99, Sunrise: s.rise, Sunset: s.set } };
      }
      return status(d);
    }
    if (key === 'time') return { Time: localIso() };
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

  window.Tasmota = {
    get(url, id) {
      let u; try { u = new URL(url, 'http://' + SUBNET + '114/'); } catch (e) { return reply(id, false, 'java.net.MalformedURLException'); }
      const d = devices[u.hostname];
      if (!d) return reply(id, false, `java.net.SocketTimeoutException: failed to connect to /${u.hostname}`, 1500);
      if (d.protected && u.searchParams.get('password') !== 'demo')
        return reply(id, false, 'HTTP 401 {"WARNING":"Need user=<username>&password=<password>"}');
      if (d.protected) return reply(id, true, JSON.stringify({ POWER: 'OFF' }));
      reply(id, true, JSON.stringify(command(d, u.searchParams.get('cmnd') || '')));
    },
    scan(id) { this.scanRange('', id); },
    scanRange(range, id) {
      const ips = Object.keys(devices);
      if (ips.some(ip => inRange(range, ip) === null)) return reply(id, false, 'Invalid range: ' + range, 50);
      const found = ips.filter(ip => inRange(range, ip)).map(ip => {
        const d = devices[ip];
        return d.protected ? { ip, protected: true } : { ip, name: d.name, topic: d.topic, friendly: d.friendly, power: d.on ? '1' : '0' };
      });
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
      '<b>Demo</b> — simulated switches, nothing is sent to a real network. ' +
      'Tap <i>Scan network</i> to find them. The protected one uses password <code>demo</code>. ' +
      '<a href="https://github.com/ldijkman/Tasmota-android-test/releases/latest">Get the Android app</a></div>');
  });
})();

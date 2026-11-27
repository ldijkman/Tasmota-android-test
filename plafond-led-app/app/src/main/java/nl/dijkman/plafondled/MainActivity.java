package nl.dijkman.plafondled;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Toont de bedieningspagina (assets/index.html) en doet de HTTP-verzoeken naar
 * Tasmota zelf, buiten de browser om. Daardoor speelt CORS geen rol.
 */
public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        // standaard laat de WebView geen tekst kleiner dan 8px toe; de IP op de kaarten is bewust heel klein
        s.setMinimumFontSize(1);
        s.setMinimumLogicalFontSize(1);

        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Tasmota");
        web.loadUrl("file:///android_asset/index.html");
        handleSetupIntent(getIntent());

        // widgets laten meeluisteren met wifi-wijzigingen (ook na een app-update)
        android.appwidget.AppWidgetManager awm = android.appwidget.AppWidgetManager.getInstance(this);
        if (awm.getAppWidgetIds(new android.content.ComponentName(this, WifiWidget.class)).length > 0)
            WifiWidget.listenForNetworkChanges(this);
        if (awm.getAppWidgetIds(new android.content.ComponentName(this, DeviceWidget.class)).length > 0)
            DeviceWidget.listenForNetworkChanges(this);

        // Eenmalig: locatietoestemming, zodat de wifi-widget de netwerknaam kan tonen
        if (android.os.Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                   != android.content.pm.PackageManager.PERMISSION_GRANTED
                && !getPreferences(MODE_PRIVATE).getBoolean("askedLoc", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("askedLoc", true).apply();
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    // ---- Setup delen / importeren (groepen, namen, scènes, pictogrammen) ----
    private static final int PICK_SETUP = 42, SAVE_SETUP = 43;
    private volatile String pendingSetup = null, setupToSave = null, pendingLink = null;

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSetupIntent(intent);
    }

    /** App geopend met een setup-bestand (Bestanden, WhatsApp, Quick Share, delen naar de app) */
    private void handleSetupIntent(android.content.Intent i) {
        if (i == null) return;
        android.net.Uri u = null;
        if (android.content.Intent.ACTION_VIEW.equals(i.getAction())) u = i.getData();
        // link vanaf de website: tasmotascan://open, tasmotascan://scenes, tasmotascan://tiles
        if (u != null && "tasmotascan".equals(u.getScheme())) {
            String h = String.valueOf(u.getHost()).toLowerCase(java.util.Locale.ROOT);
            if (!h.matches("[a-z]{1,20}")) h = "open";
            pendingLink = h;
            final String js = "window.__openLink && window.__openLink()";
            if (web != null) web.post(() -> web.evaluateJavascript(js, null));
            return;
        }
        else if (android.content.Intent.ACTION_SEND.equals(i.getAction())) u = i.getParcelableExtra(android.content.Intent.EXTRA_STREAM);
        if (u != null) readSetup(u);
    }

    private void readSetup(final android.net.Uri u) {
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(u)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192]; int n; long total = 0;
                while ((n = in.read(buf)) > 0) { out.write(buf, 0, n); total += n; if (total > 8_000_000) throw new Exception("too big"); }
                pendingSetup = out.toString("UTF-8");
                // als de pagina al geladen is direct doorgeven, anders vraagt de pagina het zelf op bij het starten
                if (web != null) web.post(() -> web.evaluateJavascript("window.__setupFile && window.__setupFile()", null));
            } catch (Exception e) {
                if (web != null) web.post(() -> web.evaluateJavascript("window.showToast && showToast('Could not read the file')", null));
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int req, int res, android.content.Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_SETUP && res == RESULT_OK && data != null && data.getData() != null) readSetup(data.getData());
        if (req == SAVE_SETUP) {
            final String json = setupToSave; setupToSave = null;
            if (res != RESULT_OK || data == null || data.getData() == null || json == null) return;
            final android.net.Uri u = data.getData();
            new Thread(() -> {
                String msg;
                try (java.io.OutputStream o = getContentResolver().openOutputStream(u, "wt")) {
                    o.write(json.getBytes(StandardCharsets.UTF_8)); msg = "✓ Saved";
                } catch (Exception e) { msg = "Could not save the file"; }
                final String js = "window.showToast && showToast('" + msg + "')";
                if (web != null) web.post(() -> web.evaluateJavascript(js, null));
            }).start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        WifiWidget.updateAll(this);
        notifyWifi();
    }

    /** Laat de pagina de wifi-kaart verversen (na terugkomen uit het wifi-paneel) */
    private void notifyWifi() {
        if (web != null) web.post(() -> web.evaluateJavascript("window.__wifiChanged && window.__wifiChanged()", null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        WifiWidget.updateAll(this);
        DeviceWidget.updateAll(this);
        TailscaleWidget.updateAll(this);
        notifyWifi();
        // pagina laten weten dat de app weer in beeld is (bijv. na het Android-installatiescherm)
        if (web != null) web.evaluateJavascript("window.__appResume && window.__appResume()", null);
    }

    /** Laatst geopende app-scherm, zodat de installatie-status terug kan naar de pagina */
    static volatile MainActivity instance;
    /** Laatste bevestigingsscherm van Android (voor "Open the install screen again") */
    volatile android.content.Intent pendingConfirm;

    /**
     * Installeren via de PackageInstaller-sessie van Android (betrouwbaarder dan het APK "openen").
     * Android stuurt de status naar UpdateReceiver; die opent zo nodig het bevestigingsscherm.
     * Lukt de sessie niet, dan valt het terug op het oude "APK openen".
     */
    private void installApk(java.io.File f, String id) {
        try {
            android.content.pm.PackageInstaller pi = getPackageManager().getPackageInstaller();
            android.content.pm.PackageInstaller.SessionParams params =
                    new android.content.pm.PackageInstaller.SessionParams(android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName(getPackageName());
            // Android 12+: is deze app zelf de installateur (na de eerste update via de app), dan zonder extra vragen
            if (android.os.Build.VERSION.SDK_INT >= 31)
                params.setRequireUserAction(android.content.pm.PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
            int sid = pi.createSession(params);
            try (android.content.pm.PackageInstaller.Session session = pi.openSession(sid)) {
                try (java.io.OutputStream out = session.openWrite("update.apk", 0, f.length());
                     java.io.InputStream in = new java.io.FileInputStream(f)) {
                    byte[] buf = new byte[65536]; int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                    session.fsync(out);
                }
                android.content.Intent cb = new android.content.Intent(this, UpdateReceiver.class).setAction(UpdateReceiver.ACTION);
                int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT
                        | (android.os.Build.VERSION.SDK_INT >= 31 ? android.app.PendingIntent.FLAG_MUTABLE : 0);
                android.app.PendingIntent p = android.app.PendingIntent.getBroadcast(this, sid, cb, flags);
                getSharedPreferences("update", MODE_PRIVATE).edit().putBoolean("pending", true).apply();
                session.commit(p.getIntentSender());
            }
            callback(id, true, "\"ok\"");
        } catch (Exception e) {
            // terugvaloptie: APK laten openen door het installatiescherm
            runOnUiThread(() -> {
                try {
                    android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW)
                            .setDataAndType(ApkProvider.uri(), ApkProvider.MIME)
                            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    callback(id, true, "\"ok\"");
                } catch (Exception e2) { callback(id, false, e2.toString()); }
            });
        }
    }

    /** Status van de installatie naar de pagina: window.__updStatus(code, tekst) */
    void updateStatus(int status, String msg) {
        if (web == null) return;
        String m = msg == null ? "" : msg.replace("\\", " ").replace("'", " ").replace("\n", " ");
        web.post(() -> web.evaluateJavascript("window.__updStatus && window.__updStatus(" + status + ",'" + m + "')", null));
    }

    /** Stuurt een resultaat terug naar JavaScript: window.__tasCb(id, ok, base64) */
    private void callback(String id, boolean ok, String body) {
        final String b64 = Base64.encodeToString(body.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        final String js = "window.__tasCb && window.__tasCb('" + id.replace("'", "") + "'," + ok + ",'" + b64 + "')";
        web.post(() -> web.evaluateJavascript(js, null));
    }

    private static final String TS_PKG = "com.tailscale.ipn";

    // ---- Nieuw Tasmota-apparaat instellen: verbinden met zijn eigen wifi (tasmota-XXXX) en thuis-wifi doorgeven ----
    private android.net.ConnectivityManager.NetworkCallback apCb;
    private void apRelease() {
        try {
            if (apCb != null) getSystemService(android.net.ConnectivityManager.class).unregisterNetworkCallback(apCb);
        } catch (Exception ignored) { }
        apCb = null;
    }
    /** GET over het netwerk van het apparaat (niet over de thuis-wifi); true bij 2xx/3xx */
    private static boolean apGet(android.net.Network net, String url) {
        java.net.HttpURLConnection c = null;
        try {
            c = (java.net.HttpURLConnection) net.openConnection(new java.net.URL(url));
            c.setConnectTimeout(6000); c.setReadTimeout(6000);
            int code = c.getResponseCode();
            try (java.io.InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream()) {
                if (in != null) { byte[] b = new byte[2048]; while (in.read(b) > 0) { } }
            } catch (Exception ignored) { }
            return code >= 200 && code < 400;
        } catch (Exception e) {
            return false;
        } finally { if (c != null) c.disconnect(); }
    }
    /** zelfde als apGet, maar geeft de tekst terug (of null) */
    private static String apGetText(android.net.Network net, String url) {
        java.net.HttpURLConnection c = null;
        try {
            c = (java.net.HttpURLConnection) net.openConnection(new java.net.URL(url));
            c.setConnectTimeout(6000); c.setReadTimeout(6000);
            if (c.getResponseCode() != 200) return null;
            try (java.io.InputStream in = c.getInputStream()) {
                java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(); byte[] b = new byte[2048]; int n;
                while ((n = in.read(b)) > 0 && o.size() < 65536) o.write(b, 0, n);
                return o.toString("UTF-8");
            }
        } catch (Exception e) {
            return null;
        } finally { if (c != null) c.disconnect(); }
    }
    private static String enc(String s) {
        try { return java.net.URLEncoder.encode(s == null ? "" : s, "UTF-8"); } catch (Exception e) { return ""; }
    }

    private class Bridge {
        /**
         * Tasmota.sendWifiDirect(ssid, pass, id): de gebruiker is zelf met tasmota-XXXX verbonden;
         * stuur de thuis-wifi via elk wifi-netwerk van de telefoon (niet via mobiele data). "sent" of fout "nosend".
         */
        @JavascriptInterface
        public void sendWifiDirect(final String ssid, final String pass, final String id) {
            new Thread(() -> {
                boolean ok = false;
                try {
                    android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
                    for (android.net.Network n : cm.getAllNetworks()) {
                        android.net.NetworkCapabilities c = cm.getNetworkCapabilities(n);
                        if (c == null || !c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)) continue;
                        ok = apGet(n, "http://192.168.4.1/wi?s1=" + enc(ssid) + "&p1=" + enc(pass) + "&save=");
                        if (!ok) ok = apGet(n, "http://192.168.4.1/cm?cmnd=" + enc("Backlog SSID1 " + ssid + "; Password1 " + pass));
                        if (ok) break;
                    }
                } catch (Exception ignored) { }
                callback(id, ok, ok ? "sent" : "nosend");
            }).start();
        }

        /** Tasmota.wifiScan(id): wifi-netwerken in de buurt -> {"nets":[{"ssid","level","freq"}]}; fout: noperm, off, wifioff */
        @JavascriptInterface
        public void wifiScan(final String id) {
            new Thread(() -> {
                try {
                    if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) { callback(id, false, "noperm"); return; }
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        android.location.LocationManager lm = getSystemService(android.location.LocationManager.class);
                        if (lm != null && !lm.isLocationEnabled()) { callback(id, false, "off"); return; }
                    }
                    android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    if (wm == null || !wm.isWifiEnabled()) { callback(id, false, "wifioff"); return; }
                    try { wm.startScan(); } catch (Exception ignored) { }   // Android beperkt dit; anders de laatst bekende lijst
                    Thread.sleep(3500);
                    org.json.JSONArray all = new org.json.JSONArray();
                    java.util.Map<String, JSONObject> seen = new java.util.HashMap<>();
                    for (android.net.wifi.ScanResult r : wm.getScanResults()) {
                        String ss = r.SSID;
                        if (ss == null || ss.isEmpty()) continue;
                        JSONObject o = seen.get(ss);
                        if (o == null) { o = new JSONObject(); o.put("ssid", ss); o.put("level", r.level); o.put("freq", r.frequency); o.put("g24", false); seen.put(ss, o); all.put(o); }
                        else if (r.level > o.getInt("level")) o.put("level", r.level);
                        if (r.frequency < 3000) o.put("g24", true);   // er is (ook) een 2,4 GHz-zender
                    }
                    callback(id, true, new JSONObject().put("nets", all).toString());
                } catch (Exception e) { callback(id, false, e.toString()); }
            }).start();
        }

        /**
         * Tasmota.setupDevice(apSsid, ssid, pass, id): verbindt even met de wifi van een nieuw Tasmota-apparaat
         * (Android vraagt de gebruiker om te bevestigen), stuurt de thuis-wifi en laat die verbinding weer los.
         * Antwoord: "sent" (bevestigd) of fout "nosend" (verbonden, maar niet bevestigd), "notfound", "old" (Android 9 of ouder).
         */
        @JavascriptInterface
        public void setupDevice(final String ap, final String ssid, final String pass, final String id) {
            if (android.os.Build.VERSION.SDK_INT < 29) { callback(id, false, "old"); return; }
            runOnUiThread(() -> {
                try {
                    apRelease();
                    final android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
                    android.net.wifi.WifiNetworkSpecifier spec = new android.net.wifi.WifiNetworkSpecifier.Builder().setSsid(ap).build();
                    android.net.NetworkRequest req = new android.net.NetworkRequest.Builder()
                            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                            .removeCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .setNetworkSpecifier(spec).build();
                    final boolean[] done = {false};
                    apCb = new android.net.ConnectivityManager.NetworkCallback() {
                        @Override public void onAvailable(final android.net.Network net) {
                            synchronized (done) { if (done[0]) return; done[0] = true; }
                            new Thread(() -> {
                                boolean ok = false;
                                try {
                                    Thread.sleep(1500);
                                    // de wifi-pagina van Tasmota (werkt in de instelmodus); anders het console-commando
                                    ok = apGet(net, "http://192.168.4.1/wi?s1=" + enc(ssid) + "&p1=" + enc(pass) + "&save=");
                                    if (!ok) ok = apGet(net, "http://192.168.4.1/cm?cmnd=" + enc("Backlog SSID1 " + ssid + "; Password1 " + pass));
                                    Thread.sleep(1500);
                                } catch (Exception ignored) { }
                                runOnUiThread(MainActivity.this::apRelease);
                                callback(id, ok, ok ? "sent" : "nosend");
                            }).start();
                        }
                        @Override public void onUnavailable() {
                            synchronized (done) { if (done[0]) return; done[0] = true; }
                            runOnUiThread(MainActivity.this::apRelease);
                            callback(id, false, "notfound");
                        }
                    };
                    cm.requestNetwork(req, apCb, 60000);
                } catch (Exception e) { apRelease(); callback(id, false, "err: " + e); }
            });
        }
        /** Tasmota.setupShelly(ap, ssid, pass, id): met de eigen wifi van een nieuwe Shelly verbinden (192.168.33.1)
         *  en de thuis-wifi instellen. Gen1: /settings/sta, Gen2/3/4: WiFi.SetConfig. Antwoord: "sent:<gen>" */
        @JavascriptInterface
        public void setupShelly(final String ap, final String ssid, final String pass, final String id) {
            if (android.os.Build.VERSION.SDK_INT < 29) { callback(id, false, "old"); return; }
            runOnUiThread(() -> {
                try {
                    apRelease();
                    final android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
                    android.net.wifi.WifiNetworkSpecifier spec = new android.net.wifi.WifiNetworkSpecifier.Builder().setSsid(ap).build();
                    android.net.NetworkRequest req = new android.net.NetworkRequest.Builder()
                            .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                            .removeCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            .setNetworkSpecifier(spec).build();
                    final boolean[] done = {false};
                    apCb = new android.net.ConnectivityManager.NetworkCallback() {
                        @Override public void onAvailable(final android.net.Network net) {
                            synchronized (done) { if (done[0]) return; done[0] = true; }
                            new Thread(() -> {
                                boolean ok = false; int gen = 0;
                                try {
                                    Thread.sleep(1500);
                                    String info = apGetText(net, "http://192.168.33.1/shelly");
                                    if (info != null) { try { JSONObject j = new JSONObject(info); gen = j.has("gen") ? j.optInt("gen", 2) : 1; } catch (Exception ignored) { } }
                                    if (gen >= 2) {
                                        JSONObject sta = new JSONObject().put("ssid", ssid).put("pass", pass).put("enable", true);
                                        ok = apGet(net, "http://192.168.33.1/rpc/WiFi.SetConfig?config=" + enc(new JSONObject().put("sta", sta).toString()));
                                    } else {
                                        ok = apGet(net, "http://192.168.33.1/settings/sta?enabled=1&ssid=" + enc(ssid) + "&key=" + enc(pass));
                                        if (ok && gen == 0) gen = 1;
                                    }
                                    Thread.sleep(1500);
                                } catch (Exception ignored) { }
                                runOnUiThread(MainActivity.this::apRelease);
                                callback(id, ok, ok ? "sent:" + gen : (gen == 0 ? "noshelly" : "nosend"));
                            }).start();
                        }
                        @Override public void onUnavailable() {
                            synchronized (done) { if (done[0]) return; done[0] = true; }
                            runOnUiThread(MainActivity.this::apRelease);
                            callback(id, false, "notfound");
                        }
                    };
                    cm.requestNetwork(req, apCb, 60000);
                } catch (Exception e) { apRelease(); callback(id, false, "err: " + e); }
            });
        }
        /** Tasmota.isHomeApp(): is deze app de standaard Home-app van Android? */
        @JavascriptInterface
        public boolean isHomeApp() {
            try {
                android.content.Intent h = new android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME);
                android.content.pm.ResolveInfo r = getPackageManager().resolveActivity(h, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
                return r != null && r.activityInfo != null && getPackageName().equals(r.activityInfo.packageName);
            } catch (Exception e) { return false; }
        }
        /** Tasmota.setHomeApp(true/false): app aan/af melden als Home-app en Android's keuzescherm openen */
        @JavascriptInterface
        public void setHomeApp(final boolean on) {
            runOnUiThread(() -> {
                try {
                    getPackageManager().setComponentEnabledSetting(
                            new android.content.ComponentName(MainActivity.this, getPackageName() + ".HomeAlias"),
                            on ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                               : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            android.content.pm.PackageManager.DONT_KILL_APP);
                    startActivity(new android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS));
                } catch (Exception e) {
                    try { startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS)); } catch (Exception ignored) { }
                }
            });
        }
        /** Tasmota.fullscreen(true/false): status- en navigatiebalk verbergen; vegen vanaf de rand toont ze even */
        @JavascriptInterface
        public void fullscreen(final boolean on) {
            runOnUiThread(() -> {
                try {
                    android.view.Window w = getWindow();
                    if (android.os.Build.VERSION.SDK_INT >= 30) {
                        android.view.WindowInsetsController ic = w.getInsetsController();
                        if (ic == null) return;
                        int bars = android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars();
                        if (on) {
                            ic.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                            ic.hide(bars);
                        } else ic.show(bars);
                    } else {
                        android.view.View d = w.getDecorView();
                        int m = android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                              | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                        int f = d.getSystemUiVisibility();
                        d.setSystemUiVisibility(on ? (f | m) : (f & ~m));
                    }
                } catch (Exception ignored) { }
            });
        }
        /** Tasmota.setBars("#rrggbb", light): statusbalk en navigatiebalk (3 knoppen) in de kleur van het thema */
        @JavascriptInterface
        public void setBars(final String color, final boolean light) {
            runOnUiThread(() -> {
                try {
                    int c = android.graphics.Color.parseColor(color);
                    android.view.Window w = getWindow();
                    w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                    w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    w.setStatusBarColor(c);
                    // donkere knoppen op een lichte balk kan pas vanaf Android 8; daarvoor de balk zwart laten
                    w.setNavigationBarColor(light && android.os.Build.VERSION.SDK_INT < 26 ? android.graphics.Color.BLACK : c);
                    if (android.os.Build.VERSION.SDK_INT >= 30) {
                        android.view.WindowInsetsController ic = w.getInsetsController();
                        int m = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                        if (ic != null) ic.setSystemBarsAppearance(light ? m : 0, m);
                    } else {
                        android.view.View d = w.getDecorView();
                        int m = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                              | (android.os.Build.VERSION.SDK_INT >= 26 ? android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
                        int f = d.getSystemUiVisibility();
                        d.setSystemUiVisibility(light ? (f | m) : (f & ~m));
                    }
                } catch (Exception ignored) { }
            });
        }
        /** Tasmota.keepScreenOn(true/false): scherm niet uit laten gaan zolang de app open is */
        @JavascriptInterface
        public void keepScreenOn(final boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }
        /** Tasmota.screenBrightness(0..1): helderheid van dit venster; -1 = instelling van de telefoon */
        @JavascriptInterface
        public void screenBrightness(final float v) {
            runOnUiThread(() -> {
                android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.screenBrightness = v < 0 ? android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE : Math.max(0.01f, Math.min(1f, v));
                getWindow().setAttributes(lp);
            });
        }
        /** Tasmota.shareSetup(json, naam): bestand maken en het Android-deelscherm openen */
        @JavascriptInterface
        public void shareSetup(final String json, final String name) {
            try {
                java.io.File f = ApkProvider.shareFile(MainActivity.this, name);
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) { o.write(json.getBytes(StandardCharsets.UTF_8)); }
                final android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND)
                        .setType("application/json")
                        .putExtra(android.content.Intent.EXTRA_STREAM, ApkProvider.shareUri(f.getName()))
                        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Tasmota Scan & Control setup")
                        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(android.content.Intent.createChooser(send, "Share setup")));
            } catch (Exception e) {
                web.post(() -> web.evaluateJavascript("showToast('Could not share: " + String.valueOf(e.getMessage()).replace("'", "") + "')", null));
            }
        }

        /** Tasmota.saveSetup(json, naam): "Opslaan als" (Downloads, Drive, OneDrive…) */
        @JavascriptInterface
        public void saveSetup(final String json, final String name) {
            setupToSave = json;
            final android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(android.content.Intent.CATEGORY_OPENABLE).setType("application/json")
                    .putExtra(android.content.Intent.EXTRA_TITLE, name);
            runOnUiThread(() -> { try { startActivityForResult(i, SAVE_SETUP); } catch (Exception e) {
                setupToSave = null; web.evaluateJavascript("showToast('No file manager found')", null); } });
        }

        /** Tasmota.pickSetup(): bestandskiezer voor een setup-bestand */
        @JavascriptInterface
        public void pickSetup() {
            final android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(android.content.Intent.CATEGORY_OPENABLE).setType("*/*")
                    .putExtra(android.content.Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "application/octet-stream"});
            runOnUiThread(() -> { try { startActivityForResult(i, PICK_SETUP); } catch (Exception e) {
                web.evaluateJavascript("showToast('No file picker found')", null); } });
        }

        /** Tasmota.pendingSetup(): inhoud van een geopend setup-bestand (één keer) */
        @JavascriptInterface
        public String pendingSetup() { String t = pendingSetup; pendingSetup = null; return t == null ? "" : t; }

        /** Tasmota.pendingLink(): scherm uit een website-link (open, scenes, tiles), eenmalig */
        @JavascriptInterface
        public String pendingLink() { String t = pendingLink; pendingLink = null; return t == null ? "" : t; }

        /** Tasmota.renameDevices(json): {oudAdres: nieuwAdres}; widgets volgen een schakelaar naar zijn nieuwe adres */
        @JavascriptInterface
        public void renameDevices(String json) {
            try { DeviceWidget.renameIps(getApplicationContext(), new JSONObject(json)); } catch (Exception ignored) { }
        }

        /** Tasmota.syncWidgets(json): lijst met schakelaars voor de homescreen-widgets */
        @JavascriptInterface
        public void syncWidgets(String json) {
            DeviceWidget.saveDevices(getApplicationContext(), json);
        }

        /**
         * Tasmota.installUpdate(url, id): APK downloaden en het Android-installatiescherm openen.
         * Voortgang: window.__updProg(procent). Resultaat via de callback: "ok", of fout "noperm" / tekst.
         */
        @JavascriptInterface
        public void installUpdate(final String url, final String id) {
            if (url == null || !url.startsWith("https://")) { callback(id, false, "bad url"); return; }
            // Android 8+: deze app moet apps mogen installeren; zo niet, de instelling openen
            if (android.os.Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
                runOnUiThread(() -> {
                    try {
                        startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                android.net.Uri.parse("package:" + getPackageName())));
                    } catch (Exception ignored) { }
                });
                callback(id, false, "noperm");
                return;
            }
            new Thread(() -> {
                java.io.File f = ApkProvider.apkFile(MainActivity.this);
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(30000);
                    c.setInstanceFollowRedirects(true);
                    c.setRequestProperty("User-Agent", "TasmotaScanControl/" + BuildConfig.VERSION_NAME + " (Android)");
                    int code = c.getResponseCode();
                    if (code >= 400) throw new Exception("HTTP " + code);
                    long total = c.getContentLengthLong(), done = 0; int last = -1;
                    try (InputStream in = c.getInputStream(); java.io.FileOutputStream out = new java.io.FileOutputStream(f)) {
                        byte[] buf = new byte[16384]; int n;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n); done += n;
                            int pct = total > 0 ? (int) (done * 100 / total) : -1;
                            if (pct != last && pct >= 0) { last = pct; final int p = pct;
                                web.post(() -> web.evaluateJavascript("window.__updProg && window.__updProg(" + p + ")", null)); }
                        }
                    } finally { c.disconnect(); }
                    if (f.length() < 10000) throw new Exception("download too small");
                    installApk(f, id);
                } catch (Exception e) {
                    callback(id, false, e.toString());
                }
            }).start();
        }

        /** Tasmota.widgetPulse(ip, seconden): resterende auto-off-tijd naar de schakelaar-widgets */
        @JavascriptInterface
        public void widgetPulse(String ip, double sec) {
            DeviceWidget.setPulse(getApplicationContext(), ip, sec);
        }

        /** Tasmota.installDownloaded(id): de al gedownloade update nog eens laten installeren */
        @JavascriptInterface
        public void installDownloaded(final String id) {
            java.io.File f = ApkProvider.apkFile(MainActivity.this);
            if (!f.exists() || f.length() < 10000) { callback(id, false, "no download"); return; }
            new Thread(() -> installApk(f, id)).start();
        }

        /** Tasmota.installOpen(id): het installatiescherm direct openen (zonder sessie), als het andere niet verschijnt */
        @JavascriptInterface
        public void installOpen(final String id) {
            runOnUiThread(() -> {
                try {
                    android.content.Intent c = pendingConfirm;
                    if (c != null) { pendingConfirm = null; startActivity(c); callback(id, true, "\"ok\""); return; }
                } catch (Exception ignored) { }
                try {
                    java.io.File f = ApkProvider.apkFile(MainActivity.this);
                    if (!f.exists() || f.length() < 10000) { callback(id, false, "no download"); return; }
                    android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW)
                            .setDataAndType(ApkProvider.uri(), ApkProvider.MIME)
                            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    callback(id, true, "\"ok\"");
                } catch (Exception e) { callback(id, false, e.toString()); }
            });
        }

        /** Tasmota.syncScenes(json): scènes voor de scène-widget */
        @JavascriptInterface
        public void syncScenes(String json) {
            SceneWidget.saveScenes(getApplicationContext(), json);
        }

        /** Tasmota.widgetState(ip, "on"|"off"): bekende toestand naar de widgets */
        @JavascriptInterface
        public void widgetState(String ip, String st) {
            DeviceWidget.setState(getApplicationContext(), ip, st);
        }

        /** Tasmota.tailscaleInfo(): {"installed":bool, "on":bool, "ip":"100.x.y.z"}.
         *  "on" = er is een VPN-tunnel met een Tailscale-adres (100.64.0.0/10) op dit toestel. */
        @JavascriptInterface
        public String tailscaleInfo() {
            JSONObject o = new JSONObject();
            try {
                boolean inst;
                try { getPackageManager().getPackageInfo(TS_PKG, 0); inst = true; } catch (Exception e) { inst = false; }
                o.put("installed", inst);
                String ip = "";
                java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
                while (en != null && en.hasMoreElements() && ip.isEmpty()) {
                    java.net.NetworkInterface ni = en.nextElement();
                    String n = ni.getName();
                    if (n == null || !(n.startsWith("tun") || n.startsWith("tailscale")) || !ni.isUp()) continue;
                    java.util.Enumeration<java.net.InetAddress> ad = ni.getInetAddresses();
                    while (ad.hasMoreElements()) {
                        java.net.InetAddress a = ad.nextElement();
                        byte[] b = a.getAddress();
                        // 100.64.0.0/10: het adresbereik van Tailscale
                        if (b.length == 4 && (b[0] & 0xff) == 100 && (b[1] & 0xc0) == 64) { ip = a.getHostAddress(); break; }
                    }
                }
                o.put("on", !ip.isEmpty());
                o.put("ip", ip);
            } catch (Exception ignored) { }
            return o.toString();
        }

        /** Tasmota.tailscale(on): Tailscale verbinden of verbreken (opdracht aan de Tailscale-app) */
        @JavascriptInterface
        public void tailscale(boolean on) {
            TailscaleWidget.send(MainActivity.this, on);
            final android.content.Context app = getApplicationContext();
            new Thread(() -> {   // widget op het startscherm laten volgen
                try { for (int i = 0; i < 5; i++) { Thread.sleep(2000); TailscaleWidget.updateAll(app); } }
                catch (InterruptedException ignored) { }
            }).start();
        }

        /** Tasmota.openTailscale(): de Tailscale-app openen */
        @JavascriptInterface
        public void openTailscale() {
            runOnUiThread(() -> {
                try {
                    android.content.Intent i = getPackageManager().getLaunchIntentForPackage(TS_PKG);
                    if (i != null) startActivity(i);
                } catch (Exception ignored) { }
            });
        }

        /** Tasmota.wifiInfo(): {"ssid":..., "ip":..., "status":"ok|off|none|noname", "perm":bool, "loc":bool} */
        @JavascriptInterface
        public String wifiInfo() {
            try {
                String[] c = WifiWidget.current(MainActivity.this);
                JSONObject o = new JSONObject();
                o.put("ssid", c[0] == null ? "" : c[0]);
                o.put("ip", c[1]);
                o.put("status", c[2]);
                o.put("perm", checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED);
                boolean loc = true;
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    android.location.LocationManager lm = getSystemService(android.location.LocationManager.class);
                    loc = lm != null && lm.isLocationEnabled();
                }
                o.put("loc", loc);
                return o.toString();
            } catch (Exception e) { return "{}"; }
        }

        /**
         * Tasmota.location(id): huidige locatie van de telefoon -> {"lat","lon","acc"}.
         * Fout: "noperm" (geen toestemming), "off" (locatie uit), "timeout".
         */
        @JavascriptInterface
        public void location(final String id) {
            web.post(() -> getLocation(id));
        }

        /** Tasmota.appInfo(): {"version","code","date","android","sdk","maker","model"} van deze build en telefoon */
        @JavascriptInterface
        public String appInfo() {
            try {
                JSONObject o = new JSONObject();
                o.put("version", BuildConfig.VERSION_NAME);
                o.put("code", BuildConfig.VERSION_CODE);
                o.put("date", BuildConfig.BUILD_DATE);
                // telefoon: Android-versie en model (handig bij het melden van problemen)
                o.put("android", android.os.Build.VERSION.RELEASE);
                o.put("sdk", android.os.Build.VERSION.SDK_INT);
                o.put("maker", android.os.Build.MANUFACTURER);
                o.put("model", android.os.Build.MODEL);
                return o.toString();
            } catch (Exception e) { return "{}"; }
        }

        /** Tasmota.openUrl(url): webpagina openen in de browser van de telefoon */
        @JavascriptInterface
        public void openUrl(final String url) {
            if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) return;
            web.post(() -> {
                try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))); }
                catch (Exception ignored) { }
            });
        }

        /** Tasmota.openWifi(): wifi-paneel van Android */
        @JavascriptInterface
        public void openWifi() {
            web.post(() -> {
                try { startActivity(WifiWidget.wifiPanelIntent()); }
                catch (Exception e) { startActivity(new android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)); }
            });
        }

        /** Tasmota.fixName(): toestemming vragen, of locatie-instellingen openen */
        @JavascriptInterface
        public void fixName() {
            web.post(() -> {
                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            || !getPreferences(MODE_PRIVATE).getBoolean("askedLoc", false)) {
                        getPreferences(MODE_PRIVATE).edit().putBoolean("askedLoc", true).apply();
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    } else {   // definitief geweigerd: app-instellingen openen
                        startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:" + getPackageName())));
                    }
                } else {
                    startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            });
        }

        /** Tasmota.get(url, id): één HTTP GET */
        @JavascriptInterface
        public void get(final String url, final String id) {
            new Thread(() -> {
                try {
                    boolean web = url.startsWith("https://");   // internet (pictogrammen): meer geduld
                    String[] r = web ? httpGet(url, 10000, 30000) : httpGet(url, 4000, 6000);
                    int code = Integer.parseInt(r[0]);
                    if (code < 400) callback(id, true, r[1]);
                    else callback(id, false, "HTTP " + code + " " + r[1]);
                } catch (Exception e) {
                    callback(id, false, e.toString());
                }
            }).start();
        }

        /** Tasmota.post(url, json, id): één HTTP POST met JSON (bijv. script-code naar een Shelly) */
        @JavascriptInterface
        public void post(final String url, final String body, final String id) {
            new Thread(() -> {
                try {
                    String[] r = httpPost(url, body, 4000, 15000);
                    int code = Integer.parseInt(r[0]);
                    if (code < 400) callback(id, true, r[1]);
                    else callback(id, false, "HTTP " + code + " " + r[1]);
                } catch (Exception e) {
                    callback(id, false, e.toString());
                }
            }).start();
        }

        /** Tasmota.scanHints("ip1,ip2"): opgeslagen apparaten, krijgen bij een scan meer geduld */
        @JavascriptInterface
        public void scanHints(String csv) {
            java.util.Set<String> h = new java.util.HashSet<>();
            if (csv != null) for (String ip : csv.split(",")) if (!ip.trim().isEmpty()) h.add(ip.trim());
            scanHints = h;
        }

        /** Tasmota.scan(id): zoekt Tasmota-apparaten in het eigen /24-netwerk */
        @JavascriptInterface
        public void scan(final String id) {
            scanRange("", id);
        }

        /**
         * Tasmota.scanRange(bereik, id): zoekt in een opgegeven bereik.
         * Leeg of "auto" = eigen wifi-netwerk. Verder bijv. "192.168.1", "192.168.1.x",
         * "192.168.1.0/24" of "192.168.1.50-120".
         */
        @JavascriptInterface
        public void scanRange(final String range, final String id) {
            new Thread(() -> {
                try {
                    String prefix; int from = 1, to = 254;
                    String r = range == null ? "" : range.trim().toLowerCase();
                    if (r.isEmpty() || r.equals("auto")) {
                        prefix = localPrefix();
                    } else {
                        java.util.regex.Matcher m = java.util.regex.Pattern
                                .compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:\\.(?:x|\\*|0/24|(\\d{1,3})(?:\\s*-\\s*(\\d{1,3}))?))?(?:/24)?$")
                                .matcher(r);
                        if (!m.matches()) { callback(id, false, "Invalid range: " + range); return; }
                        for (int g = 1; g <= 3; g++)
                            if (Integer.parseInt(m.group(g)) > 255) { callback(id, false, "Invalid range: " + range); return; }
                        prefix = m.group(1) + "." + m.group(2) + "." + m.group(3) + ".";
                        if (m.group(4) != null) {
                            from = Integer.parseInt(m.group(4));
                            to = m.group(5) != null ? Integer.parseInt(m.group(5)) : from;
                            if (from < 1 || to > 254 || from > to) { callback(id, false, "Invalid range: " + range); return; }
                        }
                    }
                    JSONObject res = new JSONObject();
                    JSONArray found = new JSONArray();
                    String label = prefix == null ? "" :
                            (from == 1 && to == 254) ? prefix + "0/24" :
                            from == to ? prefix + from : prefix + from + "–" + to;
                    res.put("subnet", label);
                    if (prefix != null) {
                        // Ronde 1: alle adressen tegelijk (64 threads). Adressen die niet op tijd
                        // antwoorden (Tasmota in wifi-slaapstand reageert soms traag) krijgen een
                        // tweede, rustigere ronde met meer geduld.
                        List<String> slow = new ArrayList<>();
                        for (int pass = 1; pass <= 2; pass++) {
                            List<String> ips = new ArrayList<>();
                            if (pass == 1) for (int i = from; i <= to; i++) ips.add(prefix + i);
                            else ips.addAll(slow);
                            if (ips.isEmpty()) break;
                            final int connectMs = pass == 1 ? 1500 : 3000, readMs = pass == 1 ? 3000 : 5000;
                            ExecutorService pool = Executors.newFixedThreadPool(pass == 1 ? 64 : 24);
                            List<Future<Object>> jobs = new ArrayList<>();
                            for (final String ip : ips) jobs.add(pool.submit(() -> probe(ip, connectMs, readMs)));
                            slow = new ArrayList<>();
                            for (int k = 0; k < jobs.size(); k++) {
                                try {
                                    Object o = jobs.get(k).get();
                                    if (o instanceof JSONObject) found.put(o);
                                    else if (o == SLOW) slow.add(ips.get(k));
                                } catch (Exception ignored) { }
                            }
                            pool.shutdown();
                            pool.awaitTermination(5, TimeUnit.SECONDS);
                        }
                    }
                    res.put("devices", found);
                    callback(id, true, res.toString());
                } catch (Exception e) {
                    callback(id, false, e.toString());
                }
            }).start();
        }
    }

    private void sendLocation(String id, android.location.Location l) {
        try {
            JSONObject o = new JSONObject();
            o.put("lat", l.getLatitude());
            o.put("lon", l.getLongitude());
            o.put("acc", l.hasAccuracy() ? Math.round(l.getAccuracy()) : -1);
            callback(id, true, o.toString());
        } catch (Exception e) { callback(id, false, e.toString()); }
    }

    private void getLocation(final String id) {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED
         && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            callback(id, false, "noperm"); return;
        }
        final android.location.LocationManager lm = getSystemService(android.location.LocationManager.class);
        if (lm == null) { callback(id, false, "off"); return; }
        boolean net = lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
        boolean gps = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        if (!net && !gps) { callback(id, false, "off"); return; }

        // Recente bekende locatie (< 30 min) is goed genoeg voor zonsopkomst/-ondergang
        android.location.Location best = null;
        for (String p : lm.getProviders(true)) {
            try {
                android.location.Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            } catch (SecurityException ignored) { }
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 30 * 60 * 1000L) {
            sendLocation(id, best); return;
        }

        // Anders één nieuwe meting afwachten (max. 20 s), daarna terugvallen op de oude
        final android.location.Location fallback = best;
        final boolean[] done = {false};
        final android.location.LocationListener ll = new android.location.LocationListener() {
            @Override public void onLocationChanged(android.location.Location l) {
                if (done[0]) return;
                done[0] = true; lm.removeUpdates(this); sendLocation(id, l);
            }
            @Override public void onStatusChanged(String p, int st, Bundle b) { }
            @Override public void onProviderEnabled(String p) { }
            @Override public void onProviderDisabled(String p) { }
        };
        try {
            lm.requestLocationUpdates(net ? android.location.LocationManager.NETWORK_PROVIDER
                    : android.location.LocationManager.GPS_PROVIDER, 0, 0f, ll, android.os.Looper.getMainLooper());
        } catch (SecurityException e) { callback(id, false, "noperm"); return; }
        web.postDelayed(() -> {
            if (done[0]) return;
            done[0] = true; lm.removeUpdates(ll);
            if (fallback != null) sendLocation(id, fallback); else callback(id, false, "timeout");
        }, 20000);
    }

    private static final Object SLOW = new Object();

    /** Vraagt "Status" op: JSONObject = Tasmota, SLOW = geen antwoord op tijd (nog eens proberen), null = niets/geen Tasmota */
    private static Object probe(String ip, int connectMs, int readMs) {
        try {
            String[] r = httpGet("http://" + ip + "/cm?cmnd=Status", connectMs, readMs);
            int code = Integer.parseInt(r[0]);
            JSONObject o = new JSONObject();
            o.put("ip", ip);
            o.put("kind", "tasmota");          // ook een Shelly die later Tasmota kreeg weer als Tasmota zien
            if (code == 401) {                 // Tasmota met webwachtwoord
                if (!r[1].contains("password")) return null;
                o.put("protected", true);
                return o;
            }
            JSONObject st = null;
            try { st = new JSONObject(r[1]).optJSONObject("Status"); } catch (Exception ignored) {}
            if (st == null) return shellyProbe(ip, connectMs, readMs);   // geen Tasmota: misschien een Shelly
            o.put("name", st.optString("DeviceName", ""));
            o.put("topic", st.optString("Topic", ""));
            JSONArray fn = st.optJSONArray("FriendlyName");
            // relais 1; namen van relais 2, 3 … haalt de app op als het meer relais herkent
            o.put("friendly", fn != null && fn.length() > 0 ? fn.optString(0) : "");
            o.put("power", st.opt("Power") == null ? "" : String.valueOf(st.opt("Power")));
            return o;
        } catch (java.net.SocketTimeoutException e) {
            // verbonden maar traag antwoord: zeker opnieuw; geen verbinding op tijd: alleen bij bekende apparaten
            String m = String.valueOf(e.getMessage()).toLowerCase();
            return m.contains("connect") ? (scanHints.contains(ip) ? SLOW : null) : SLOW;
        } catch (Exception e) {
            return null;
        }
    }

    /** Shelly herkennen via /shelly (Gen1: "type", Gen2/3/4: "gen") */
    private static Object shellyProbe(String ip, int connectMs, int readMs) {
        try {
            String[] r = httpGet("http://" + ip + "/shelly", connectMs, readMs);
            if (!"200".equals(r[0])) return null;
            JSONObject j = new JSONObject(r[1]);
            if (!j.has("mac") || !(j.has("type") || j.has("gen"))) return null;
            int gen = j.optInt("gen", 1);
            JSONObject o = new JSONObject();
            o.put("ip", ip);
            o.put("kind", "shelly");
            o.put("gen", gen);
            String model = gen >= 2 ? j.optString("app", j.optString("model", "Shelly")) : j.optString("type", "Shelly");
            o.put("model", model);
            o.put("name", gen >= 2 ? j.optString("id", model) : model);
            String nm = j.optString("name", "");
            o.put("friendly", "null".equals(nm) ? "" : nm);
            String mac = j.optString("mac", "").replaceAll("[^0-9A-Fa-f]", "").toUpperCase(java.util.Locale.ROOT);
            if (mac.length() == 12) o.put("mac", mac.replaceAll("(..)(?!$)", "$1:"));
            o.put("protected", j.optBoolean("auth", false) || j.optBoolean("auth_en", false));
            o.put("comp", shellyComp(ip, gen, model, connectMs, readMs));
            return o;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Welk onderdeel een Shelly schakelt: "switch" (relais), "cct" (wit, warm-koel), "rgbcct" (kleur + wit),
     * "rgb"/"rgbw" (kleur), "light" (dimmer) of "g1light" (Gen1 Duo/Vintage lamp). Onbekend: "switch".
     */
    private static String shellyComp(String ip, int gen, String model, int connectMs, int readMs) {
        if (gen < 2) {
            String t = model.toUpperCase(java.util.Locale.ROOT);
            return t.startsWith("SHBDUO") || t.startsWith("SHVIN") ? "g1light" : "switch";
        }
        try {
            String[] r = httpGet("http://" + ip + "/rpc/Shelly.GetStatus", connectMs, Math.max(readMs, 1500));
            if (!"200".equals(r[0])) return "switch";
            JSONObject s = new JSONObject(r[1]);
            for (String c : new String[]{"switch", "rgbcct", "cct", "rgbw", "rgb", "light"})
                if (s.has(c + ":0")) return c;
        } catch (Exception ignored) { }
        return "switch";
    }

    /** IP's van opgeslagen apparaten: die krijgen bij scannen een tweede kans */
    private static volatile java.util.Set<String> scanHints = new java.util.HashSet<>();

    /** Eerste drie delen van het wifi-IP van de telefoon, bv. "10.10.100." */
    private static String localPrefix() throws Exception {
        String fallback = null;
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp() || ni.isLoopback()) continue;
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (!(a instanceof Inet4Address) || !a.isSiteLocalAddress()) continue;
                String ip = a.getHostAddress();
                String p = ip.substring(0, ip.lastIndexOf('.') + 1);
                if (ni.getName().startsWith("wlan")) return p;
                if (fallback == null) fallback = p;
            }
        }
        return fallback;
    }

    /** Geeft {statuscode, body} terug */
    private static String[] httpGet(String url, int connectMs, int readMs) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(connectMs);
            c.setRequestProperty("User-Agent", "TasmotaScanControl/" + BuildConfig.VERSION_NAME + " (Android)");
            c.setInstanceFollowRedirects(true);
            c.setReadTimeout(readMs);
            int code = c.getResponseCode();
            InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream();
            return new String[]{String.valueOf(code), in == null ? "" : readAll(in)};
        } finally {
            c.disconnect();
        }
    }

    private static String[] httpPost(String url, String body, int connectMs, int readMs) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setConnectTimeout(connectMs);
            c.setReadTimeout(readMs);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("User-Agent", "TasmotaScanControl/" + BuildConfig.VERSION_NAME + " (Android)");
            byte[] b = body.getBytes("UTF-8");
            c.setFixedLengthStreamingMode(b.length);
            java.io.OutputStream o = c.getOutputStream();
            o.write(b);
            o.close();
            int code = c.getResponseCode();
            InputStream in = code < 400 ? c.getInputStream() : c.getErrorStream();
            return new String[]{String.valueOf(code), in == null ? "" : readAll(in)};
        } finally {
            c.disconnect();
        }
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }

    @Override
    public void onBackPressed() {
        // eerst de pagina een open popup of menu laten sluiten
        web.evaluateJavascript("(window.__back && window.__back()) ? 'y' : 'n'", v -> {
            if (v != null && v.contains("y")) return;
            if (web.canGoBack()) web.goBack();
            else finish();
        });
    }
}

package nl.dijkman.plafondled;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Homescreen-widget voor één Tasmota-schakelaar.
 * Bij het plaatsen kies je de schakelaar (WidgetConfigActivity); tik = aan/uit.
 * De lijst met schakelaars (naam, wachtwoord, pictogram) krijgt de widget van de app
 * via Tasmota.syncWidgets(); de toestand wordt hier zelf bij Tasmota opgevraagd.
 */
public class DeviceWidget extends AppWidgetProvider {

    static final String ACTION_TOGGLE = "nl.dijkman.plafondled.DEV_TOGGLE";
    static final String ACTION_REFRESH = "nl.dijkman.plafondled.DEV_REFRESH";
    static final String ACTION_PULSE_END = "nl.dijkman.plafondled.DEV_PULSE_END";
    static final String ACTION_NET = "nl.dijkman.plafondled.DEV_NET";
    private static volatile long lastNet = 0;
    static final String PREFS = "devwidgets";
    static final ExecutorService POOL = Executors.newCachedThreadPool();

    static final int YELLOW = 0xFFE8A317, BLUE = 0xFF3FA9F5, GREY = 0xFF9A958C, ERR = 0xFFE06A5F;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- gegevens ----------

    /** Opgeslagen lijst uit de app: [{ip, name, group, pw, water, png}] */
    static JSONArray devices(Context c) {
        try { return new JSONArray(prefs(c).getString("devs", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    static JSONObject device(Context c, String ip) {
        if (ip == null) return null;
        JSONArray a = devices(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && ip.equals(o.optString("ip"))) return o;
        }
        return null;
    }

    static void saveDevices(Context c, String json) {
        prefs(c).edit().putString("devs", json == null ? "[]" : json).apply();
        updateAll(c);
        GroupWidget.updateAll(c);
    }

    /** Schakelaar kreeg een ander adres: widgets (ook relais "ip#n") volgen. map = {oudAdres: nieuwAdres} */
    static void renameIps(Context c, JSONObject map) {
        SharedPreferences p = prefs(c);
        SharedPreferences.Editor e = p.edit();
        for (java.util.Map.Entry<String, ?> en : p.getAll().entrySet()) {
            String k = en.getKey();
            if (!k.matches("w\\d+") || !(en.getValue() instanceof String)) continue;
            String v = (String) en.getValue();
            int hash = v.indexOf('#');
            String host = hash < 0 ? v : v.substring(0, hash), to = map.optString(host, "");
            if (!to.isEmpty()) e.putString(k, to + (hash < 0 ? "" : v.substring(hash)));
        }
        e.apply();
        updateAll(c);
    }

    static String ipFor(Context c, int wid) { return prefs(c).getString("w" + wid, null); }

    static void setIp(Context c, int wid, String ip) { prefs(c).edit().putString("w" + wid, ip).apply(); }

    static String state(Context c, String ip) { return prefs(c).getString("st_" + ip, ""); }

    static void putState(Context c, String ip, String st) {
        SharedPreferences.Editor e = prefs(c).edit().putString("st_" + ip, st);
        if (!"on".equals(st) && !"busy".equals(st)) e.remove("pe_" + ip);   // uit: geen aftellen meer
        e.apply();
    }

    /** Auto-off: nog `sec` seconden aan (0 = geen). Eindtijd in elapsedRealtime-milliseconden. */
    static void putPulse(Context c, String ip, double sec) {
        SharedPreferences.Editor e = prefs(c).edit();
        if (sec > 0.5) e.putLong("pe_" + ip, android.os.SystemClock.elapsedRealtime() + (long) (sec * 1000));
        else e.remove("pe_" + ip);
        e.apply();
    }

    /** Vanuit de app: resterende auto-off-tijd doorgeven */
    static void setPulse(Context c, String ip, double sec) {
        putPulse(c, ip, sec);
        updateForIp(c, ip);
    }

    // ---------- Shelly (eigen firmware) ----------

    /** RPC-naam van het onderdeel: Switch, CCT, RGBCCT, RGB, RGBW, Light */
    static String shellyNs(String comp) {
        switch (comp) {
            case "cct": return "CCT";
            case "rgbcct": return "RGBCCT";
            case "rgb": return "RGB";
            case "rgbw": return "RGBW";
            case "light": return "Light";
            default: return "Switch";
        }
    }

    private static JSONObject getJson(String url) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        try {
            h.setConnectTimeout(3000); h.setReadTimeout(4000);
            int code = h.getResponseCode();
            if (code == 401) throw new SecurityException();
            if (code >= 400) throw new Exception("HTTP " + code);
            String t = readAll(h.getInputStream()).trim();
            return t.startsWith("{") ? new JSONObject(t) : new JSONObject();
        } finally { h.disconnect(); }
    }

    /** Aan/uit/wisselen/stand bij een Shelly; zelfde antwoorden als request() */
    static String shellyRequest(Context c, String ip, JSONObject d, String cmd) {
        String m = cmd.trim().toLowerCase(java.util.Locale.ROOT);
        String act = m.equals("power") ? "" : m.equals("power on") || m.equals("power 1") ? "on"
                   : m.equals("power off") || m.equals("power 0") ? "off" : m.equals("power toggle") || m.equals("power 2") ? "toggle" : null;
        if (act == null) return "err";   // Timers e.d. bestaan niet bij Shelly
        String h = "http://" + host(ip), comp = d.optString("sc", "switch");
        try {
            boolean on; double rem = 0;
            if (d.optInt("sg", 1) < 2) {
                JSONObject r = getJson(h + ("g1light".equals(comp) ? "/light/0" : "/relay/0") + (act.isEmpty() ? "" : "?turn=" + act));
                on = r.optBoolean("ison");
                if (on && r.optBoolean("has_timer")) rem = r.optDouble("timer_remaining", 0);
            } else {
                String ns = shellyNs(comp);
                if (act.equals("on") || act.equals("off")) getJson(h + "/rpc/" + ns + ".Set?id=0&on=" + act.equals("on"));
                else if (act.equals("toggle")) getJson(h + "/rpc/" + ns + ".Toggle?id=0");
                JSONObject st = getJson(h + "/rpc/" + ns + ".GetStatus?id=0");
                on = st.optBoolean("output");
                if (on && st.has("timer_started_at")) {
                    double now = System.currentTimeMillis() / 1000.0;   // liever de klok van de Shelly zelf
                    try { JSONObject sy = getJson(h + "/rpc/Sys.GetStatus"); if (sy.optLong("unixtime", 0) > 0) now = sy.optLong("unixtime"); } catch (Exception ignored) { }
                    double dur = st.optDouble("timer_duration", 0);
                    rem = Math.min(dur, st.optDouble("timer_started_at", 0) + dur - now);
                }
            }
            putPulse(c, ip, on ? Math.max(0, rem) : 0);
            return on ? "on" : "off";
        } catch (SecurityException e) { return "locked"; }
        catch (Exception e) { return "err"; }
    }

    /** Resterende PulseTime opvragen (Tasmota: 1–111 = tienden van seconden, daarboven seconden + 100) */
    static void fetchPulse(Context c, String ip) {
        try {
            JSONObject d = device(c, ip);
            if (d != null && d.optBoolean("sk")) { shellyRequest(c, ip, d, "Power"); return; }
            String pw = d == null ? "" : d.optString("pw", "");
            String url = "http://" + host(ip) + "/cm?" + (pw.isEmpty() ? "" : "user=admin&password=" + URLEncoder.encode(pw, "UTF-8") + "&")
                    + "cmnd=PulseTime" + relay(ip);
            HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
            try {
                h.setConnectTimeout(3000); h.setReadTimeout(4000);
                if (h.getResponseCode() >= 400) return;
                JSONObject r = new JSONObject(readAll(h.getInputStream()));
                Object v = r.opt("PulseTime" + relay(ip));
                int rem = v instanceof JSONObject ? ((JSONObject) v).optInt("Remaining", 0) : 0;
                putPulse(c, ip, rem > 111 ? rem - 100 : rem / 10.0);
            } finally { h.disconnect(); }
        } catch (Exception ignored) { }
    }

    /** Vanuit de app: bekende toestand doorgeven ("on" / "off") */
    static void setState(Context c, String ip, String st) {
        if (ip == null || st == null || st.equals(state(c, ip))) return;
        putState(c, ip, st);
        updateForIp(c, ip);
    }

    // ---------- widget-levenscyclus ----------

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        listenForNetworkChanges(ctx);
        for (int id : ids) render(ctx, mgr, id);
        // toestand ophalen bij de schakelaars (op de achtergrond)
        final PendingResult pr = goAsync();
        final Context app = ctx.getApplicationContext();
        POOL.execute(() -> {
            try { refreshAll(app); } finally { pr.finish(); }
        });
    }

    @Override
    public void onEnabled(Context ctx) { listenForNetworkChanges(ctx); }

    @Override
    public void onDisabled(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            try { ctx.getSystemService(ConnectivityManager.class).unregisterNetworkCallback(netIntent(ctx)); }
            catch (Exception ignored) { }
        }
    }

    private static PendingIntent netIntent(Context ctx) {
        Intent i = new Intent(ctx, DeviceWidget.class).setAction(ACTION_NET);
        return PendingIntent.getBroadcast(ctx, 7, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    /** Android seint de widget bij elke wifi-wijziging, ook als de app dicht is (Android 8+) */
    static void listenForNetworkChanges(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
            // elk netwerk (wifi én mobiel): als wifi wegvalt, wordt mobiel actief en horen we dat ook
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
            PendingIntent pi = netIntent(ctx);
            try { cm.unregisterNetworkCallback(pi); } catch (Exception ignored) { }
            cm.registerNetworkCallback(req, pi);
        } catch (Exception ignored) { }
    }

    @Override
    public void onDeleted(Context ctx, int[] ids) {
        SharedPreferences.Editor e = prefs(ctx).edit();
        for (int id : ids) e.remove("w" + id);
        e.apply();
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (ACTION_PULSE_END.equals(intent.getAction())) {
            // auto-off afgelopen: echte toestand ophalen
            final String ip = intent.getStringExtra("ip");
            if (ip == null) return;
            final Context app = ctx.getApplicationContext();
            final PendingResult pr = goAsync();
            POOL.execute(() -> {
                try {
                    String[] w = WifiWidget.current(app);
                    putState(app, ip, w[1] == null || w[1].isEmpty() ? "nowifi" : reach(app, ip, w[1]));
                } finally { updateForIp(app, ip); pr.finish(); }
            });
            return;
        }
        if (ACTION_REFRESH.equals(intent.getAction())) {
            final int wid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            final String ip = ipFor(ctx, wid);
            if (ip == null) return;
            final Context app = ctx.getApplicationContext();
            putState(app, ip, "busy");
            updateForIp(app, ip);
            final PendingResult pr = goAsync();
            POOL.execute(() -> {
                try {
                    String[] w = WifiWidget.current(app);
                    putState(app, ip, w[1] == null || w[1].isEmpty() ? "nowifi" : reach(app, ip, w[1]));
                } finally { updateForIp(app, ip); pr.finish(); }
            });
            return;
        }
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            final int wid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            final String ip = ipFor(ctx, wid);
            if (ip == null) { openApp(ctx); return; }
            final Context app = ctx.getApplicationContext();
            putState(app, ip, "busy");
            updateForIp(app, ip);
            final PendingResult pr = goAsync();
            POOL.execute(() -> {
                try {
                    String[] w = WifiWidget.current(app);
                    if (w[1] == null || w[1].isEmpty()) putState(app, ip, "nowifi");
                    else {
                        String st = request(app, ip, "Power TOGGLE");
                        if ("err".equals(st) && !prefix(ip).equals(prefix(w[1]))) st = "otherwifi";
                        putState(app, ip, st);
                    }
                }
                finally { updateForIp(app, ip); pr.finish(); }
            });
            return;
        }
        if (ACTION_NET.equals(intent.getAction())) {
            // wifi aan/uit/ander netwerk: kort wachten tot het IP er is, dan opnieuw kijken
            long now = System.currentTimeMillis();
            if (now - lastNet < 4000) return;   // Android stuurt er vaak meerdere tegelijk
            lastNet = now;
            final Context app = ctx.getApplicationContext();
            final PendingResult pr = goAsync();
            POOL.execute(() -> {
                try {
                    Thread.sleep(2500);
                    refreshAll(app);
                } catch (InterruptedException ignored) {
                } finally { pr.finish(); }
            });
            return;
        }
        super.onReceive(ctx, intent);
    }

    static void openApp(Context ctx) {
        Intent i = new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    /** Toestand van alle schakelaars met een widget opvragen */
    static void refreshAll(Context c) {
        Set<String> ips = new LinkedHashSet<>();
        for (int id : ids(c)) { String ip = ipFor(c, id); if (ip != null) ips.add(ip); }
        ips.addAll(GroupWidget.allIps(c));     // ook de schakelaars van groep-widgets
        String[] w = WifiWidget.current(c);   // {ssid, ip, status}
        boolean wifi = w[1] != null && !w[1].isEmpty();
        for (String ip : ips) {
            putState(c, ip, wifi ? reach(c, ip, w[1]) : "nowifi");
            updateForIp(c, ip);
        }
    }

    static void refreshOne(Context c, String ip) {
        POOL.execute(() -> {
            String[] w = WifiWidget.current(c);
            putState(c, ip, w[1] == null || w[1].isEmpty() ? "nowifi" : reach(c, ip, w[1]));
            updateForIp(c, ip);
        });
    }

    /** Toestand opvragen; lukt het niet en zit de telefoon in een ander subnet: "otherwifi" */
    static String reach(Context c, String ip, String phoneIp) {
        String st = request(c, ip, "Power");
        if ("err".equals(st) && !prefix(ip).equals(prefix(phoneIp))) return "otherwifi";
        return st;
    }

    static String prefix(String ip) {
        int i = ip == null ? -1 : ip.lastIndexOf('.');
        return i < 0 ? "" : ip.substring(0, i);
    }

    // ---------- tekenen ----------

    static int[] ids(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, DeviceWidget.class));
    }

    static void updateAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) render(c, mgr, id);
    }

    static void updateForIp(Context c, String ip) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) if (ip.equals(ipFor(c, id))) render(c, mgr, id);
        GroupWidget.updateAll(c);
    }

    static String label(JSONObject d, String ip) {
        String n = d == null ? "" : d.optString("name", "");
        return n.isEmpty() ? ip : n;
    }

    static Bitmap iconBitmap(JSONObject d) {
        try {
            String b64 = d == null ? "" : d.optString("png", "");
            if (b64.isEmpty()) return null;
            byte[] b = Base64.decode(b64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } catch (Exception e) { return null; }
    }

    static void render(Context c, AppWidgetManager mgr, int wid) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_device);
        String ip = ipFor(c, wid);
        JSONObject d = device(c, ip);
        String st = ip == null ? "" : state(c, ip);
        boolean on = "on".equals(st), water = d != null && d.optBoolean("water", false);
        int col = on ? (water ? BLUE : YELLOW) : ("err".equals(st) || "locked".equals(st)) ? ERR : GREY;
        // eigen aan-kleur uit de app (#rrggbb)
        int own = 0;
        String oc = d == null ? "" : d.optString("oc", "");
        if (oc.matches("#[0-9a-fA-F]{6}")) { try { own = android.graphics.Color.parseColor(oc); } catch (Exception ignored) {} }
        if (on && own != 0) col = own;

        Bitmap bm = iconBitmap(d);
        if (bm != null) v.setImageViewBitmap(R.id.wd_icon, bm);
        else v.setImageViewResource(R.id.wd_icon, R.drawable.ic_dev_bulb);
        v.setInt(R.id.wd_icon, "setColorFilter", on ? col : 0xFF8A857C);

        v.setTextViewText(R.id.wd_name, ip == null ? "Choose switch" : label(d, ip));
        String txt = on ? "On" : "off".equals(st) ? "Off" : "busy".equals(st) ? "…"
                : "locked".equals(st) ? "Password?" : "err".equals(st) ? "Not reachable"
                : "nowifi".equals(st) ? "No wifi" : "otherwifi".equals(st) ? "Other wifi" : "Tap to switch";
        v.setTextViewText(R.id.wd_state, txt);
        v.setTextColor(R.id.wd_state, col);
        // auto-off loopt: Android-chronometer telt zelf af tot de eindtijd
        long end = ip == null ? 0 : prefs(c).getLong("pe_" + ip, 0), now = android.os.SystemClock.elapsedRealtime();
        if (on && end > now + 500) {
            v.setViewVisibility(R.id.wd_state, android.view.View.GONE);
            v.setViewVisibility(R.id.wd_timer, android.view.View.VISIBLE);
            v.setChronometer(R.id.wd_timer, end, "off in %s", true);
            v.setChronometerCountDown(R.id.wd_timer, true);
            v.setTextColor(R.id.wd_timer, col);
            schedulePulseEnd(c, ip, end);
        } else {
            v.setChronometer(R.id.wd_timer, now, null, false);
            v.setViewVisibility(R.id.wd_timer, android.view.View.GONE);
            v.setViewVisibility(R.id.wd_state, android.view.View.VISIBLE);
        }
        v.setInt(R.id.wd_root, "setBackgroundResource",
                on ? (own != 0 ? R.drawable.widget_dev_on : water ? R.drawable.widget_dev_on_w : R.drawable.widget_dev_on) : R.drawable.widget_dev_off);

        Intent t = new Intent(c, DeviceWidget.class).setAction(ACTION_TOGGLE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
                .setData(Uri.parse("tasmota-widget://" + wid));   // uniek per widget
        v.setOnClickPendingIntent(R.id.wd_root, PendingIntent.getBroadcast(c, wid, t,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent r = new Intent(c, DeviceWidget.class).setAction(ACTION_REFRESH)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
                .setData(Uri.parse("tasmota-widget-refresh://" + wid));
        v.setOnClickPendingIntent(R.id.wd_refresh, PendingIntent.getBroadcast(c, wid, r,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        v.setViewVisibility(R.id.wd_refresh, ip == null ? android.view.View.GONE : android.view.View.VISIBLE);
        mgr.updateAppWidget(wid, v);
    }

    /** Vlak na de eindtijd de toestand ophalen (onnauwkeurig wekkertje, geen extra toestemming nodig) */
    static void schedulePulseEnd(Context c, String ip, long end) {
        try {
            android.app.AlarmManager am = (android.app.AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(c, DeviceWidget.class).setAction(ACTION_PULSE_END).putExtra("ip", ip)
                    .setData(Uri.parse("tasmota-pulse://" + ip));
            PendingIntent pi = PendingIntent.getBroadcast(c, ip.hashCode(), i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setWindow(android.app.AlarmManager.ELAPSED_REALTIME, end + 1500, 10000, pi);
        } catch (Exception ignored) { }
    }

    // ---------- Tasmota ----------

    // Meer relais: sleutel "ip#n" (relais 1 = gewoon het ip)
    static String host(String key) { int i = key.indexOf('#'); return i < 0 ? key : key.substring(0, i); }
    static int relay(String key) {
        int i = key.indexOf('#');
        try { return i < 0 ? 1 : Integer.parseInt(key.substring(i + 1)); } catch (Exception e) { return 1; }
    }
    static String relCmd(String key, String cmd) {
        int n = relay(key); if (n == 1) return cmd;
        if (cmd.equals("Power") || cmd.startsWith("Power ")) return "Power" + n + cmd.substring(5);
        if (cmd.startsWith("PulseTime1")) return "PulseTime" + n + cmd.substring(10);
        return cmd;
    }

    /** Stuurt een commando, geeft "on" | "off" | "locked" | "err" terug */
    static String request(Context c, String ip, String cmd) {
        try {
            JSONObject d = device(c, ip);
            if (d != null && d.optBoolean("sk")) return shellyRequest(c, ip, d, cmd);
            String pw = d == null ? "" : d.optString("pw", "");
            String url = "http://" + host(ip) + "/cm?"
                    + (pw.isEmpty() ? "" : "user=admin&password=" + URLEncoder.encode(pw, "UTF-8") + "&")
                    + "cmnd=" + URLEncoder.encode(relCmd(ip, cmd), "UTF-8").replace("+", "%20");
            HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
            try {
                h.setConnectTimeout(3000);
                h.setReadTimeout(4000);
                int code = h.getResponseCode();
                if (code == 401) return "locked";
                if (code >= 400) return "err";
                JSONObject r = new JSONObject(readAll(h.getInputStream()));
                if (r.has("WARNING") && !r.has("POWER") && !r.has("POWER1")) return "locked";
                int rn = relay(ip);
                String p = rn > 1 ? r.optString("POWER" + rn, "") : r.optString("POWER", r.optString("POWER1", ""));
                String st = "ON".equalsIgnoreCase(p) ? "on" : "OFF".equalsIgnoreCase(p) ? "off" : "err";
                if ("on".equals(st)) fetchPulse(c, ip);   // loopt er een auto-off? dan aftellen op de widget
                return st;
            } finally { h.disconnect(); }
        } catch (Exception e) { return "err"; }
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toString("UTF-8");
    }
}

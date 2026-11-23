package nl.dijkman.plafondled;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Homescreen-widget voor een hele groep. Tik: is er iets aan, dan alles uit; anders alles aan
 * (zoals de groepsschakelaar in de app). De groepen komen mee met Tasmota.syncWidgets().
 */
public class GroupWidget extends AppWidgetProvider {

    static final String ACTION_TOGGLE = "nl.dijkman.plafondled.GRP_TOGGLE";
    static final String ACTION_REFRESH = "nl.dijkman.plafondled.GRP_REFRESH";

    static SharedPreferences prefs(Context c) { return DeviceWidget.prefs(c); }

    static String gidFor(Context c, int wid) { return prefs(c).getString("g" + wid, null); }

    static void setGid(Context c, int wid, String gid) { prefs(c).edit().putString("g" + wid, gid).apply(); }

    static int[] ids(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, GroupWidget.class));
    }

    /** Schakelaars in deze groep (uit de lijst van de app) */
    static List<JSONObject> members(Context c, String gid) {
        List<JSONObject> out = new ArrayList<>();
        if (gid == null) return out;
        JSONArray a = DeviceWidget.devices(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && gid.equals(o.optString("gid"))) out.add(o);
        }
        return out;
    }

    static Set<String> allIps(Context c) {
        Set<String> ips = new LinkedHashSet<>();
        for (int id : ids(c)) for (JSONObject o : members(c, gidFor(c, id))) ips.add(o.optString("ip"));
        return ips;
    }

    // ---------- levenscyclus ----------

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] wids) {
        DeviceWidget.listenForNetworkChanges(ctx);
        // eventueel blijven hangende "bezig"-markering opruimen
        SharedPreferences.Editor ed = prefs(ctx).edit();
        for (int id : wids) { String g = gidFor(ctx, id); if (g != null) ed.remove("gb_" + g); }
        ed.apply();
        for (int id : wids) render(ctx, mgr, id);
        final Context app = ctx.getApplicationContext();
        final PendingResult pr = goAsync();
        DeviceWidget.POOL.execute(() -> {
            try { for (int id : wids) run(app, gidFor(app, id), "Power"); }
            finally { pr.finish(); }
        });
    }

    @Override
    public void onDeleted(Context ctx, int[] wids) {
        SharedPreferences.Editor e = prefs(ctx).edit();
        for (int id : wids) e.remove("g" + id);
        e.apply();
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String act = intent.getAction();
        if (ACTION_TOGGLE.equals(act) || ACTION_REFRESH.equals(act)) {
            int wid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            final String gid = gidFor(ctx, wid);
            final List<JSONObject> m = members(ctx, gid);
            if (m.isEmpty()) { DeviceWidget.openApp(ctx); return; }
            final Context app = ctx.getApplicationContext();
            String cmd = "Power";
            if (ACTION_TOGGLE.equals(act)) {
                boolean anyOn = false;
                for (JSONObject o : m) if ("on".equals(DeviceWidget.state(app, o.optString("ip")))) anyOn = true;
                cmd = anyOn ? "Power OFF" : "Power ON";
            }
            final String c = cmd;
            prefs(app).edit().putBoolean("gb_" + gid, true).apply();
            updateAll(app);
            final PendingResult pr = goAsync();
            DeviceWidget.POOL.execute(() -> {
                try { run(app, gid, c); }
                finally {
                    prefs(app).edit().remove("gb_" + gid).apply();
                    updateAll(app);
                    pr.finish();
                }
            });
            return;
        }
        super.onReceive(ctx, intent);
    }

    /** Commando naar alle schakelaars van de groep tegelijk, daarna de widgets bijwerken */
    static void run(Context c, String gid, String cmd) {
        List<JSONObject> m = members(c, gid);
        if (m.isEmpty()) return;
        String[] w = WifiWidget.current(c);
        boolean wifi = w[1] != null && !w[1].isEmpty();
        List<Future<?>> jobs = new ArrayList<>();
        for (JSONObject o : m) {
            final String ip = o.optString("ip");
            if (!wifi) { DeviceWidget.putState(c, ip, "nowifi"); continue; }
            jobs.add(DeviceWidget.POOL.submit(() -> {
                String st = DeviceWidget.request(c, ip, cmd);
                if ("err".equals(st) && !DeviceWidget.prefix(ip).equals(DeviceWidget.prefix(w[1]))) st = "otherwifi";
                DeviceWidget.putState(c, ip, st);
            }));
        }
        for (Future<?> f : jobs) { try { f.get(); } catch (Exception ignored) { } }
        for (JSONObject o : m) DeviceWidget.updateForIp(c, o.optString("ip"));   // ook losse widgets + groepen
    }

    // ---------- tekenen ----------

    static void updateAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) render(c, mgr, id);
    }

    static void render(Context c, AppWidgetManager mgr, int wid) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_device);
        String gid = gidFor(c, wid);
        List<JSONObject> m = members(c, gid);
        int n = m.size(), nOn = 0, nNoWifi = 0, nOther = 0, nErr = 0;
        boolean allWater = n > 0;
        for (JSONObject o : m) {
            String st = DeviceWidget.state(c, o.optString("ip"));
            if ("on".equals(st)) nOn++;
            else if ("nowifi".equals(st)) nNoWifi++;
            else if ("otherwifi".equals(st)) nOther++;
            else if ("err".equals(st) || "locked".equals(st)) nErr++;
            if (!o.optBoolean("water", false)) allWater = false;
        }
        boolean busy = gid != null && prefs(c).getBoolean("gb_" + gid, false);
        boolean on = nOn > 0;
        int col = on ? (allWater ? DeviceWidget.BLUE : DeviceWidget.YELLOW) : (n > 0 && nErr == n) ? DeviceWidget.ERR : DeviceWidget.GREY;

        String name = gid == null ? "Choose group" : n == 0 ? "Group" : m.get(0).optString("group", "Group");
        String txt = busy ? "…" : gid == null ? "Tap to set up" : n == 0 ? "No switches"
                : nNoWifi == n ? "No wifi" : nOther == n ? "Other wifi" : nErr == n ? "Not reachable"
                : nOn == 0 ? "All off" : nOn == n ? "All on" : nOn + " of " + n + " on";

        v.setImageViewResource(R.id.wd_icon, R.drawable.ic_group);
        v.setInt(R.id.wd_icon, "setColorFilter", on ? col : 0xFF8A857C);
        v.setTextViewText(R.id.wd_name, name);
        v.setTextViewText(R.id.wd_state, txt);
        v.setTextColor(R.id.wd_state, col);
        v.setInt(R.id.wd_root, "setBackgroundResource",
                on ? (allWater ? R.drawable.widget_dev_on_w : R.drawable.widget_dev_on) : R.drawable.widget_dev_off);

        Intent t = new Intent(c, GroupWidget.class).setAction(ACTION_TOGGLE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
                .setData(Uri.parse("tasmota-group://" + wid));
        v.setOnClickPendingIntent(R.id.wd_root, PendingIntent.getBroadcast(c, wid, t,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent r = new Intent(c, GroupWidget.class).setAction(ACTION_REFRESH)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
                .setData(Uri.parse("tasmota-group-refresh://" + wid));
        v.setOnClickPendingIntent(R.id.wd_refresh, PendingIntent.getBroadcast(c, wid, r,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        v.setViewVisibility(R.id.wd_refresh, n == 0 ? View.GONE : View.VISIBLE);
        mgr.updateAppWidget(wid, v);
    }
}

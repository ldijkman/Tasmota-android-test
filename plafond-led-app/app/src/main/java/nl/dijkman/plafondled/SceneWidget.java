package nl.dijkman.plafondled;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
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
 * Homescreen-widget voor een scène: tik = scène uitvoeren (zoals de scène-knop in de app).
 * De scènes komen van de app via Tasmota.syncScenes().
 */
public class SceneWidget extends AppWidgetProvider {

    static final String ACTION_RUN = "nl.dijkman.plafondled.SCENE_RUN";

    static SharedPreferences prefs(Context c) { return DeviceWidget.prefs(c); }

    static void saveScenes(Context c, String json) {
        prefs(c).edit().putString("scenes", json == null ? "[]" : json).apply();
        updateAll(c);
    }

    static JSONArray scenes(Context c) {
        try { return new JSONArray(prefs(c).getString("scenes", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    static JSONObject scene(Context c, String id) {
        if (id == null) return null;
        JSONArray a = scenes(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }

    static String sidFor(Context c, int wid) { return prefs(c).getString("s" + wid, null); }

    static void setSid(Context c, int wid, String sid) { prefs(c).edit().putString("s" + wid, sid).apply(); }

    static int[] ids(Context c) {
        return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, SceneWidget.class));
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] wids) {
        SharedPreferences.Editor e = prefs(ctx).edit();
        for (int id : wids) e.remove("sr_" + id);   // eventueel hangende resultaattekst weg
        e.apply();
        for (int id : wids) render(ctx, mgr, id);
    }

    @Override
    public void onDeleted(Context ctx, int[] wids) {
        SharedPreferences.Editor e = prefs(ctx).edit();
        for (int id : wids) { e.remove("s" + id); e.remove("sr_" + id); }
        e.apply();
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (ACTION_RUN.equals(intent.getAction())) {
            final int wid = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            final JSONObject sc = scene(ctx, sidFor(ctx, wid));
            if (sc == null) { DeviceWidget.openApp(ctx); return; }
            final Context app = ctx.getApplicationContext();
            if (prefs(app).getString("sr_" + wid, "").equals("…")) return;   // loopt al
            setResult(app, wid, "…");
            final PendingResult pr = goAsync();
            DeviceWidget.POOL.execute(() -> {
                try {
                    setResult(app, wid, run(app, sc));
                    Thread.sleep(3500);               // resultaat even laten zien
                } catch (InterruptedException ignored) {
                } finally {
                    prefs(app).edit().remove("sr_" + wid).apply();
                    render(app, AppWidgetManager.getInstance(app), wid);
                    pr.finish();
                }
            });
            return;
        }
        super.onReceive(ctx, intent);
    }

    private static void setResult(Context c, int wid, String txt) {
        prefs(c).edit().putString("sr_" + wid, txt).apply();
        render(c, AppWidgetManager.getInstance(c), wid);
    }

    /** Voert de scène uit; geeft de tekst voor de widget terug ("✓ Done", "4 of 5 done", "No wifi") */
    static String run(Context c, JSONObject sc) {
        String[] w = WifiWidget.current(c);
        if (w[1] == null || w[1].isEmpty()) return "No wifi";
        JSONObject acts = sc.optJSONObject("acts");
        boolean others = sc.optBoolean("others", false);
        String timers = sc.optString("timers", "");
        JSONArray devs = DeviceWidget.devices(c);
        Set<String> touched = new LinkedHashSet<>();
        List<Future<Boolean>> jobs = new ArrayList<>();
        for (int i = 0; i < devs.length(); i++) {
            JSONObject d = devs.optJSONObject(i);
            if (d == null) continue;
            final String ip = d.optString("ip");
            String act = acts != null ? acts.optString(ip, "") : "";
            if (act.isEmpty() && others) act = "off";
            final String a = act;
            if (a.isEmpty()) continue;
            touched.add(ip);
            jobs.add(DeviceWidget.POOL.submit(() -> {
                boolean ok = true;
                String st = DeviceWidget.request(c, ip, "on".equals(a) ? "Power ON" : "Power OFF");
                if ("err".equals(st) || "locked".equals(st)) ok = false;
                DeviceWidget.putState(c, ip, st);
                // schema's pauzeren/hervatten (antwoord bevat geen POWER, dus niet meetellen)
                if (ok && !timers.isEmpty()) DeviceWidget.request(c, ip, "pause".equals(timers) ? "Timers 0" : "Timers 1");
                return ok;
            }));
        }
        int n = jobs.size(), ok = 0;
        for (Future<Boolean> f : jobs) { try { if (f.get()) ok++; } catch (Exception ignored) { } }
        for (String ip : touched) DeviceWidget.updateForIp(c, ip);
        if (n == 0) return "Nothing to do";
        if (ok == n) return "✓ Done";
        if (ok == 0 && !DeviceWidget.prefix(touched.iterator().next()).equals(DeviceWidget.prefix(w[1]))) return "Other wifi";
        return ok + " of " + n + " done";
    }

    static void updateAll(Context c) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(c);
        for (int id : ids(c)) render(c, mgr, id);
    }

    static void render(Context c, AppWidgetManager mgr, int wid) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_device);
        String sid = sidFor(c, wid);
        JSONObject sc = scene(c, sid);
        String res = prefs(c).getString("sr_" + wid, "");
        boolean busy = "…".equals(res), done = res.startsWith("✓");
        boolean bad = !res.isEmpty() && !busy && !done;

        Bitmap bm = DeviceWidget.iconBitmap(sc);
        if (bm != null) v.setImageViewBitmap(R.id.wd_icon, bm);
        else v.setImageViewResource(R.id.wd_icon, R.drawable.ic_dev_bulb);
        // eigen kleur van de scène (pictogram en naam), anders geel
        int ic = DeviceWidget.YELLOW, tc = 0xFFEEEAE2;
        String oi = sc == null ? "" : sc.optString("icolor", sc.optString("color", "")), ot = sc == null ? "" : sc.optString("tcolor", sc.optString("color", ""));
        if (oi.matches("#[0-9a-fA-F]{6}")) { try { ic = android.graphics.Color.parseColor(oi); } catch (Exception ignored) {} }
        if (ot.matches("#[0-9a-fA-F]{6}")) { try { tc = android.graphics.Color.parseColor(ot); } catch (Exception ignored) {} }
        v.setInt(R.id.wd_icon, "setColorFilter", ic);
        v.setTextColor(R.id.wd_name, tc);
        v.setTextViewText(R.id.wd_name, sc == null ? (sid == null ? "Choose scene" : "Scene deleted") : sc.optString("name", "Scene"));
        v.setTextViewText(R.id.wd_state, res.isEmpty() ? (sc == null ? "Tap to set up" : "Tap to run") : res);
        v.setTextColor(R.id.wd_state, done ? 0xFF3CCF6E : bad ? DeviceWidget.ERR : DeviceWidget.GREY);
        v.setInt(R.id.wd_root, "setBackgroundResource", busy || done ? R.drawable.widget_dev_on : R.drawable.widget_dev_off);
        v.setViewVisibility(R.id.wd_refresh, View.GONE);

        Intent t = new Intent(c, SceneWidget.class).setAction(ACTION_RUN)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid)
                .setData(Uri.parse("tasmota-scene://" + wid));
        v.setOnClickPendingIntent(R.id.wd_root, PendingIntent.getBroadcast(c, wid, t,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        mgr.updateAppWidget(wid, v);
    }
}

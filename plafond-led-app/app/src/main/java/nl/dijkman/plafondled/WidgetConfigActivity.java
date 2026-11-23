package nl.dijkman.plafondled;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

/** Opent bij het plaatsen (of opnieuw instellen) van een schakelaar-widget: kies de schakelaar. */
public class WidgetConfigActivity extends Activity {

    private int wid = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean group = false, sceneMode = false;   // groep-widget: groepen tonen i.p.v. schakelaars

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setResult(RESULT_CANCELED);   // terug/annuleren = geen widget
        Bundle ex = getIntent().getExtras();
        if (ex != null) wid = ex.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (wid == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return; }
        android.appwidget.AppWidgetProviderInfo info = AppWidgetManager.getInstance(this).getAppWidgetInfo(wid);
        group = info != null && info.provider != null && info.provider.getClassName().endsWith("GroupWidget");
        sceneMode = info != null && info.provider != null && info.provider.getClassName().endsWith("SceneWidget");

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(12), dp(16), dp(12), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1F1F1F);
        bg.setCornerRadius(dp(18));
        col.setBackground(bg);

        TextView title = text(sceneMode ? "Choose a scene for this widget" : group ? "Choose a group for this widget" : "Choose a switch for this widget", 17, 0xFFEEEAE2, true);
        title.setPadding(dp(8), 0, dp(8), dp(8));
        col.addView(title);

        JSONArray devs = DeviceWidget.devices(this);
        String cur = DeviceWidget.ipFor(this, wid);
        // groepen (volgorde zoals in de app) met het aantal schakelaars
        java.util.LinkedHashMap<String, String> gName = new java.util.LinkedHashMap<>();
        java.util.HashMap<String, Integer> gCount = new java.util.HashMap<>();
        for (int i = 0; i < devs.length(); i++) {
            JSONObject d = devs.optJSONObject(i);
            String gid = d == null ? "" : d.optString("gid", "");
            if (gid.isEmpty()) continue;
            gName.put(gid, d.optString("group", "Group"));
            gCount.put(gid, (gCount.containsKey(gid) ? gCount.get(gid) : 0) + 1);
        }
        JSONArray scs = SceneWidget.scenes(this);
        if (sceneMode && scs.length() == 0) {
            TextView t = text("No scenes yet.\n\nOpen Tasmota Scan & Control, tap \"+ Scene\" on the main page, then add the widget again.", 14, 0xFF9A958C, false);
            t.setPadding(dp(8), dp(4), dp(8), dp(12));
            col.addView(t);
            col.addView(openButton());
        } else if (sceneMode) {
            String curS = SceneWidget.sidFor(this, wid);
            for (int i = 0; i < scs.length(); i++) {
                JSONObject sc = scs.optJSONObject(i);
                if (sc != null) col.addView(sceneRow(sc, sc.optString("id").equals(curS)));
            }
        } else if (group && gName.isEmpty()) {
            TextView t = text(devs.length() == 0
                    ? "No switches saved yet.\n\nOpen Tasmota Scan & Control, scan the network, make a group, then add the widget again."
                    : "No groups yet.\n\nOpen Tasmota Scan & Control, tap \"+ New group\" and drag switches into it, then add the widget again.", 14, 0xFF9A958C, false);
            t.setPadding(dp(8), dp(4), dp(8), dp(12));
            col.addView(t);
            col.addView(openButton());
        } else if (group) {
            String curG = GroupWidget.gidFor(this, wid);
            for (java.util.Map.Entry<String, String> e : gName.entrySet())
                col.addView(groupRow(e.getKey(), e.getValue(), gCount.get(e.getKey()), e.getKey().equals(curG)));
        } else if (devs.length() == 0) {
            TextView t = text("No switches saved yet.\n\nOpen Tasmota Scan & Control, scan the network, then add the widget again.", 14, 0xFF9A958C, false);
            t.setPadding(dp(8), dp(4), dp(8), dp(12));
            col.addView(t);
            col.addView(openButton());
        } else {
            String lastGroup = null;
            for (int i = 0; i < devs.length(); i++) {
                JSONObject d = devs.optJSONObject(i);
                if (d == null) continue;
                String g = d.optString("group", "");
                if (!g.equals(lastGroup)) {
                    lastGroup = g;
                    TextView h = text((g.isEmpty() ? "Not in a group" : g).toUpperCase(), 11, 0xFF9A958C, true);
                    h.setPadding(dp(8), dp(12), dp(8), dp(4));
                    h.setLetterSpacing(0.06f);
                    col.addView(h);
                }
                col.addView(row(d, d.optString("ip").equals(cur)));
            }
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(col);
        setContentView(sv);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(420)),
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private View openButton() {
        TextView open = text("Open the app", 15, 0xFF3A2700, true);
        open.setGravity(Gravity.CENTER);
        open.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable ob = new GradientDrawable();
        ob.setColor(0xFFE8A317);
        ob.setCornerRadius(dp(12));
        open.setBackground(ob);
        open.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
        return open;
    }

    private View sceneRow(JSONObject sc, boolean selected) {
        final String sid = sc.optString("id");
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(10), dp(12), dp(10), dp(12));
        GradientDrawable rb = new GradientDrawable();
        rb.setColor(selected ? 0x22E8A317 : 0x00000000);
        rb.setCornerRadius(dp(10));
        if (selected) rb.setStroke(dp(1.5f), 0xFFE8A317);
        r.setBackground(rb);
        ImageView ic = new ImageView(this);
        Bitmap bm = DeviceWidget.iconBitmap(sc);
        if (bm != null) ic.setImageBitmap(bm); else ic.setImageResource(R.drawable.ic_dev_bulb);
        ic.setColorFilter(0xFFE8A317);
        r.addView(ic, new LinearLayout.LayoutParams(dp(26), dp(26)));
        int n = sc.optJSONObject("acts") == null ? 0 : sc.optJSONObject("acts").length();
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(14), 0, 0, 0);
        tx.addView(text(sc.optString("name", "Scene"), 15, 0xFFEEEAE2, true));
        String sub = n + (n == 1 ? " switch" : " switches") + (sc.optBoolean("others") ? " · others off" : "")
                + ("pause".equals(sc.optString("timers")) ? " · pauses schedules" : "resume".equals(sc.optString("timers")) ? " · resumes schedules" : "");
        tx.addView(text(sub, 11, 0xFF9A958C, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.setOnClickListener(v -> {
            SceneWidget.setSid(this, wid, sid);
            SceneWidget.render(this, AppWidgetManager.getInstance(this), wid);
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid));
            finish();
        });
        return r;
    }

    private View groupRow(String gid, String name, int count, boolean selected) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(10), dp(12), dp(10), dp(12));
        GradientDrawable rb = new GradientDrawable();
        rb.setColor(selected ? 0x22E8A317 : 0x00000000);
        rb.setCornerRadius(dp(10));
        if (selected) rb.setStroke(dp(1.5f), 0xFFE8A317);
        r.setBackground(rb);
        ImageView ic = new ImageView(this);
        ic.setImageResource(R.drawable.ic_group);
        ic.setColorFilter(0xFFEEEAE2);
        r.addView(ic, new LinearLayout.LayoutParams(dp(26), dp(26)));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(14), 0, 0, 0);
        tx.addView(text(name, 15, 0xFFEEEAE2, true));
        tx.addView(text(count + (count == 1 ? " switch" : " switches"), 11, 0xFF9A958C, false));
        r.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.setOnClickListener(v -> {
            GroupWidget.setGid(this, wid, gid);
            GroupWidget.render(this, AppWidgetManager.getInstance(this), wid);
            final android.content.Context app = getApplicationContext();
            DeviceWidget.POOL.execute(() -> GroupWidget.run(app, gid, "Power"));
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid));
            finish();
        });
        return r;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private View row(JSONObject d, boolean selected) {
        final String ip = d.optString("ip");
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(10), dp(10), dp(10), dp(10));
        GradientDrawable rb = new GradientDrawable();
        rb.setColor(selected ? 0x22E8A317 : 0x00000000);
        rb.setCornerRadius(dp(10));
        if (selected) rb.setStroke(dp(1.5f), 0xFFE8A317);
        r.setBackground(rb);

        ImageView ic = new ImageView(this);
        Bitmap bm = DeviceWidget.iconBitmap(d);
        if (bm != null) ic.setImageBitmap(bm); else ic.setImageResource(R.drawable.ic_dev_bulb);
        ic.setColorFilter(d.optBoolean("water", false) ? DeviceWidget.BLUE : 0xFFEEEAE2);
        r.addView(ic, new LinearLayout.LayoutParams(dp(26), dp(26)));

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(14), 0, 0, 0);
        tx.addView(text(DeviceWidget.label(d, ip), 15, 0xFFEEEAE2, true));
        TextView ipT = text(ip.replace("#", " · relay "), 11, 0xFF9A958C, false);
        ipT.setTypeface(Typeface.MONOSPACE);
        tx.addView(ipT);
        r.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        r.setOnClickListener(v -> choose(ip));
        return r;
    }

    private void choose(String ip) {
        DeviceWidget.setIp(this, wid, ip);
        DeviceWidget.render(this, AppWidgetManager.getInstance(this), wid);
        DeviceWidget.refreshOne(getApplicationContext(), ip);
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, wid));
        finish();
    }
}

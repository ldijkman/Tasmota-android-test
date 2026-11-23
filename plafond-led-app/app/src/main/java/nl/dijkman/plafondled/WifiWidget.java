package nl.dijkman.plafondled;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.widget.RemoteViews;

/**
 * Homescreen-widget: toont het huidige wifi-netwerk + IP.
 * "Wissel" opent het wifi-paneel van Android (Android 10+) of de wifi-instellingen.
 * Tik op de naam om te verversen.
 */
public class WifiWidget extends AppWidgetProvider {

    static final String ACTION_REFRESH = "nl.dijkman.plafondled.WIFI_REFRESH";
    static final String ACTION_NET = "nl.dijkman.plafondled.WIFI_NET";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        listenForNetworkChanges(ctx);
        RemoteViews v = build(ctx);
        for (int id : ids) mgr.updateAppWidget(id, v);
    }

    @Override
    public void onEnabled(Context ctx) {
        listenForNetworkChanges(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
                cm.unregisterNetworkCallback(selfIntent(ctx));
            } catch (Exception ignored) { }
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_NET.equals(intent.getAction())) {
            // Netwerkwijziging: naam en IP zijn er vaak pas een paar seconden later,
            // dus nu, na 2 s en na 6 s bijwerken
            final Context app = ctx.getApplicationContext();
            final PendingResult pr = goAsync();
            new Thread(() -> {
                try {
                    updateAll(app);
                    Thread.sleep(2000); updateAll(app);
                    Thread.sleep(4000); updateAll(app);
                } catch (InterruptedException ignored) {
                } finally { pr.finish(); }
            }).start();
        } else if (ACTION_REFRESH.equals(intent.getAction())) {
            updateAll(ctx);   // tik op naam/IP
        }
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, WifiWidget.class));
        if (ids.length == 0) return;
        RemoteViews v = build(ctx);
        for (int id : ids) mgr.updateAppWidget(id, v);
    }

    private static PendingIntent selfIntent(Context ctx) {
        Intent i = new Intent(ctx, WifiWidget.class).setAction(ACTION_NET);
        return PendingIntent.getBroadcast(ctx, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    /** Laat Android de widget seinen bij elke wifi-wijziging, ook als de app dicht is (Android 8+) */
    static void listenForNetworkChanges(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
            // elk netwerk: valt wifi weg, dan wordt mobiel actief en horen we dat ook
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
            PendingIntent pi = selfIntent(ctx);
            try { cm.unregisterNetworkCallback(pi); } catch (Exception ignored) { }
            try {   // registratie van oudere app-versies opruimen
                cm.unregisterNetworkCallback(PendingIntent.getBroadcast(ctx, 1,
                        new Intent(ctx, WifiWidget.class).setAction(ACTION_REFRESH),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));
            } catch (Exception ignored) { }
            cm.registerNetworkCallback(req, pi);
        } catch (Exception ignored) { }
    }

    /** {ssid (of null), ip, status}: status = "ok" | "off" | "none" | "noname" */
    static String[] current(Context ctx) {
        String ssid = null, ip = "", st = "none";
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null || !wm.isWifiEnabled()) return new String[]{null, "", "off"};
            WifiInfo wi = wm.getConnectionInfo();
            int a = wi == null ? 0 : wi.getIpAddress();
            if (a == 0) return new String[]{null, "", "none"};
            ip = (a & 255) + "." + ((a >> 8) & 255) + "." + ((a >> 16) & 255) + "." + ((a >> 24) & 255);
            String s = wi.getSSID();
            if (s != null && !s.contains("unknown ssid") && !s.equals("0x") && !s.isEmpty()) {
                ssid = s.replace("\"", ""); st = "ok";
            } else st = "noname";
        } catch (Exception ignored) { }
        return new String[]{ssid, ip, st};
    }

    static Intent wifiPanelIntent() {
        return new Intent(Build.VERSION.SDK_INT >= 29
                ? Settings.Panel.ACTION_WIFI : Settings.ACTION_WIFI_SETTINGS);
    }

    private static RemoteViews build(Context ctx) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_wifi);

        String[] c = current(ctx);
        String ssid = "off".equals(c[2]) ? "Wifi is off" : "none".equals(c[2]) ? "Not connected"
                : c[0] != null ? c[0] : "Wifi connected";
        String ip = c[1];
        v.setTextViewText(R.id.w_ssid, ssid);
        v.setTextViewText(R.id.w_ip, ip);

        // "Wissel": wifi-paneel (Android 10+), anders wifi-instellingen
        Intent panel = wifiPanelIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        v.setOnClickPendingIntent(R.id.w_switch, PendingIntent.getActivity(ctx, 2, panel,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        // Tik op naam/IP: verversen
        Intent refresh = new Intent(ctx, WifiWidget.class).setAction(ACTION_REFRESH);
        v.setOnClickPendingIntent(R.id.w_info, PendingIntent.getBroadcast(ctx, 3, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
}

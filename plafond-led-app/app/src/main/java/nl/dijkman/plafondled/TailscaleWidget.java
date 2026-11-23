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
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Homescreen-widget voor Tailscale: toont aan/uit (+ Tailscale-adres) en schakelt met één tik.
 * Aan/uit gaat met een opdracht aan de Tailscale-app (CONNECT_VPN / DISCONNECT_VPN).
 * De status komt van Android: is er een netwerk met een Tailscale-adres (100.64.0.0/10)?
 */
public class TailscaleWidget extends AppWidgetProvider {

    static final String PKG = "com.tailscale.ipn";
    static final String ACTION_TOGGLE = "nl.dijkman.plafondled.TS_TOGGLE";
    static final String ACTION_NET = "nl.dijkman.plafondled.TS_NET";
    private static final int AMBER = 0xFFE8A317, GREY = 0xFF8A857C;

    /** Bezig met schakelen: true = verbinden, false = verbreken, null = niet bezig */
    private static volatile Boolean busy = null;

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        listen(ctx);
        RemoteViews v = build(ctx);
        for (int id : ids) mgr.updateAppWidget(id, v);
    }

    @Override
    public void onEnabled(Context ctx) { listen(ctx); }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        final Context app = ctx.getApplicationContext();
        String a = intent.getAction();
        if (ACTION_TOGGLE.equals(a)) {
            if (!installed(app)) {   // nog niet geïnstalleerd: Play Store openen
                try {
                    app.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PKG))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (Exception ignored) { }
                return;
            }
            // altijd uitgaan van de echte toestand, niet van wat de widget toonde
            final boolean want = ip().isEmpty();
            send(app, want);
            busy = want; updateAll(app);
            final PendingResult pr = goAsync();
            new Thread(() -> {
                try {
                    for (int i = 0; i < 14; i++) {      // max. ~10 s wachten op de nieuwe toestand
                        Thread.sleep(700);
                        if (ip().isEmpty() != want) break;
                    }
                } catch (InterruptedException ignored) {
                } finally { busy = null; updateAll(app); pr.finish(); }
            }).start();
        } else if (ACTION_NET.equals(a)) {
            final PendingResult pr = goAsync();
            new Thread(() -> {
                try { updateAll(app); Thread.sleep(1500); updateAll(app); }
                catch (InterruptedException ignored) { }
                finally { pr.finish(); }
            }).start();
        }
    }

    static boolean installed(Context c) {
        try { c.getPackageManager().getPackageInfo(PKG, 0); return true; } catch (Exception e) { return false; }
    }

    /** Tailscale-adres van dit toestel, of "" als Tailscale niet verbonden is */
    static String ip() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
            while (en != null && en.hasMoreElements()) {
                java.net.NetworkInterface ni = en.nextElement();
                String n = ni.getName();
                if (n == null || !(n.startsWith("tun") || n.startsWith("tailscale")) || !ni.isUp()) continue;
                java.util.Enumeration<java.net.InetAddress> ad = ni.getInetAddresses();
                while (ad.hasMoreElements()) {
                    java.net.InetAddress x = ad.nextElement();
                    byte[] b = x.getAddress();
                    if (b.length == 4 && (b[0] & 0xff) == 100 && (b[1] & 0xc0) == 64) return x.getHostAddress();
                }
            }
        } catch (Exception ignored) { }
        return "";
    }

    /** Opdracht aan de Tailscale-app: verbinden of verbreken */
    static void send(Context c, boolean on) {
        try {
            Intent i = new Intent(on ? PKG + ".CONNECT_VPN" : PKG + ".DISCONNECT_VPN");
            i.setClassName(PKG, PKG + ".IPNReceiver");
            c.sendBroadcast(i);
        } catch (Exception ignored) { }
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, TailscaleWidget.class));
        if (ids.length == 0) return;
        RemoteViews v = build(ctx);
        for (int id : ids) mgr.updateAppWidget(id, v);
    }

    /** Laat Android de widget seinen als er een VPN-verbinding bijkomt (ook als de app dicht is) */
    static void listen(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
            NetworkRequest req = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build();
            PendingIntent pi = PendingIntent.getBroadcast(ctx, 41,
                    new Intent(ctx, TailscaleWidget.class).setAction(ACTION_NET),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            try { cm.unregisterNetworkCallback(pi); } catch (Exception ignored) { }
            cm.registerNetworkCallback(req, pi);
        } catch (Exception ignored) { }
    }

    private static RemoteViews build(Context ctx) {
        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_device);
        boolean inst = installed(ctx);
        String ip = inst ? ip() : "";
        boolean on = !ip.isEmpty();
        Boolean b = busy;

        v.setImageViewResource(R.id.wd_icon, R.drawable.ic_vpn);
        v.setInt(R.id.wd_icon, "setColorFilter", on || b != null ? AMBER : GREY);
        v.setTextViewText(R.id.wd_name, "Tailscale");
        String st = !inst ? "Not installed" : b != null ? (b ? "Connecting…" : "Disconnecting…") : on ? "On" : "Off";
        v.setTextViewText(R.id.wd_state, st);
        v.setTextColor(R.id.wd_state, on && b == null ? AMBER : 0xFF9A958C);
        v.setInt(R.id.wd_root, "setBackgroundResource", on ? R.drawable.widget_dev_on : R.drawable.widget_dev_off);
        v.setViewVisibility(R.id.wd_refresh, View.GONE);
        // Tailscale-adres klein onderaan (het tijdsveld wordt hier als gewone tekst gebruikt)
        if (on && b == null) {
            v.setViewVisibility(R.id.wd_timer, View.VISIBLE);
            v.setChronometer(R.id.wd_timer, android.os.SystemClock.elapsedRealtime(), ip, false);
            v.setTextColor(R.id.wd_timer, 0xFF9A958C);
        } else v.setViewVisibility(R.id.wd_timer, View.GONE);

        Intent t = new Intent(ctx, TailscaleWidget.class).setAction(ACTION_TOGGLE);
        v.setOnClickPendingIntent(R.id.wd_root, PendingIntent.getBroadcast(ctx, 40, t,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return v;
    }
}

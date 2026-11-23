package nl.dijkman.plafondled;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Na een update van de app zelf (MY_PACKAGE_REPLACED): de app weer openen.
 * Lukt dat niet (sommige Android-versies staan dat niet toe), dan opent de gebruiker hem zelf;
 * de app laat bij de volgende start zien dat de update gelukt is.
 */
public class UpdatedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
        // alleen als de update vanuit de app gestart is (niet bij een handmatige installatie)
        if (!ctx.getSharedPreferences("update", Context.MODE_PRIVATE).getBoolean("pending", false)) return;
        ctx.getSharedPreferences("update", Context.MODE_PRIVATE).edit().putBoolean("pending", false).apply();
        try {
            Intent i = new Intent(ctx, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ctx.startActivity(i);
        } catch (Exception ignored) { }
    }
}

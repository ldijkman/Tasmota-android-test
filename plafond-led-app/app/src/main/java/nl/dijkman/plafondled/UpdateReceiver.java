package nl.dijkman.plafondled;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

/**
 * Ontvangt de status van de update-installatie (PackageInstaller-sessie).
 * STATUS_PENDING_USER_ACTION: Android wil dat de gebruiker bevestigt -> het bevestigingsscherm openen.
 */
public class UpdateReceiver extends BroadcastReceiver {

    static final String ACTION = "nl.dijkman.plafondled.UPDATE_STATUS";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm;
            if (android.os.Build.VERSION.SDK_INT >= 33) confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
            else confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { ctx.startActivity(confirm); } catch (Exception e) { msg = e.toString(); status = -998; }
            }
        }
        MainActivity a = MainActivity.instance;
        if (a != null) a.updateStatus(status, msg);
    }
}

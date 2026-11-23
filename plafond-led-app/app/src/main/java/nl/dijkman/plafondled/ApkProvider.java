package nl.dijkman.plafondled;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Minimale provider: geeft alleen de gedownloade update (cache/update/update.apk) door aan het
 * Android-installatiescherm. Alleen-lezen, niet van buiten de app te openen (exported=false).
 */
public class ApkProvider extends ContentProvider {

    static final String AUTHORITY = "nl.dijkman.plafondled.apk";
    static final String MIME = "application/vnd.android.package-archive";

    static File apkFile(android.content.Context c) {
        File dir = new File(c.getCacheDir(), "update");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "update.apk");
    }

    static Uri uri() { return Uri.parse("content://" + AUTHORITY + "/update.apk"); }

    // Ook: het gedeelde setup-bestand (cache/share/tasmota-setup-….json)
    static File shareFile(android.content.Context c, String name) {
        File dir = new File(c.getCacheDir(), "share");
        if (dir.exists()) { File[] old = dir.listFiles(); if (old != null) for (File o : old) o.delete(); }
        else dir.mkdirs();
        return new File(dir, safe(name));
    }
    static Uri shareUri(String name) { return Uri.parse("content://" + AUTHORITY + "/share/" + safe(name)); }
    private static String safe(String n) {
        n = n == null ? "" : n.replaceAll("[^A-Za-z0-9._-]", "");
        return n.endsWith(".json") && n.length() > 5 ? n : "tasmota-setup.json";
    }
    private File fileFor(Uri uri) throws FileNotFoundException {
        String last = uri.getLastPathSegment();
        if ("update.apk".equals(last)) return apkFile(getContext());
        java.util.List<String> seg = uri.getPathSegments();
        if (seg.size() == 2 && "share".equals(seg.get(0)) && safe(last).equals(last))
            return new File(new File(getContext().getCacheDir(), "share"), last);
        throw new FileNotFoundException();
    }

    @Override public boolean onCreate() { return true; }

    @Override public String getType(Uri uri) {
        return "update.apk".equals(uri.getLastPathSegment()) ? MIME : "application/json";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String sel, String[] args, String sort) {
        File f;
        try { f = fileFor(uri); } catch (FileNotFoundException e) { return null; }
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
        return c;
    }

    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}

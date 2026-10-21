package com.ldd.ai;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Tải và giải nén mô hình nhận dạng giọng nói offline Vosk tiếng Việt. */
public final class ModelManager {
    private static final String URL_ZIP =
            "https://alphacephei.com/vosk/models/vosk-model-small-vn-0.4.zip";
    private static final String NAME = "vosk-model-small-vn-0.4";

    private ModelManager() {}

    public static File modelDir(Context c) {
        return new File(c.getFilesDir(), NAME);
    }

    public static boolean isReady(Context c) {
        return new File(modelDir(c), "am/final.mdl").exists();
    }

    public static void download(Context c, Consumer<String> progress) throws Exception {
        File zip = new File(c.getCacheDir(), "model.zip");
        HttpURLConnection conn = (HttpURLConnection) new URL(URL_ZIP).openConnection();
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(30000);
        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);
        long total = conn.getContentLengthLong();
        try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(zip)) {
            byte[] buf = new byte[64 * 1024];
            long done = 0;
            int n;
            int lastPct = -1;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                done += n;
                if (total > 0) {
                    int pct = (int) (done * 100 / total);
                    if (pct != lastPct) {
                        lastPct = pct;
                        progress.accept("Đang tải mô hình: " + pct + "%");
                    }
                }
            }
        }
        progress.accept("Đang giải nén mô hình...");
        File base = c.getFilesDir();
        String basePath = base.getCanonicalPath() + File.separator;
        try (ZipInputStream zin = new ZipInputStream(new java.io.FileInputStream(zip))) {
            ZipEntry e;
            byte[] buf = new byte[64 * 1024];
            while ((e = zin.getNextEntry()) != null) {
                File f = new File(base, e.getName());
                if (!f.getCanonicalPath().startsWith(basePath)) continue; // chặn zip-slip
                if (e.isDirectory()) {
                    f.mkdirs();
                    continue;
                }
                f.getParentFile().mkdirs();
                try (OutputStream out = new FileOutputStream(f)) {
                    int n;
                    while ((n = zin.read(buf)) > 0) out.write(buf, 0, n);
                }
            }
        }
        zip.delete();
        if (!isReady(c)) throw new Exception("Mô hình giải nén không đầy đủ");
    }
}

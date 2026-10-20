package com.ldd.ai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

/**
 * Không giao diện. Mỗi lần mở: chạy
 *   input keyevent --duration 2000 KEYCODE_POWER
 * qua Shizuku (quyền shell), rồi tự đóng.
 */
public class MainActivity extends Activity {
    private static final int REQ_CODE = 1001;
    private static boolean listenerAdded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        run(this);
        finishAndRemoveTask();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        run(this);
        finishAndRemoveTask();
    }

    private static void run(Context activityContext) {
        final Context ctx = activityContext.getApplicationContext();
        GeminiLiveAccessibilityService.armedUntil = System.currentTimeMillis() + 10000;
        try {
            if (!Shizuku.pingBinder()) {
                toast(ctx, "Shizuku chưa chạy. Hãy mở Shizuku và bấm Start.");
                return;
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                sendLongPressPower(ctx);
                return;
            }
            if (Shizuku.shouldShowRequestPermissionRationale()) {
                toast(ctx, "Bạn đã từ chối quyền Shizuku. Hãy cấp lại trong app Shizuku.");
                return;
            }
            if (!listenerAdded) {
                listenerAdded = true;
                Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                    if (requestCode == REQ_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                        sendLongPressPower(ctx);
                    } else {
                        toast(ctx, "Chưa được cấp quyền Shizuku.");
                    }
                });
            }
            Shizuku.requestPermission(REQ_CODE);
        } catch (Throwable t) {
            toast(ctx, "Lỗi: " + t.getMessage());
        }
    }

    private static void sendLongPressPower(final Context ctx) {
        new Thread(() -> {
            try {
                String[] cmd = {"input", "keyevent", "--duration", "2000", "KEYCODE_POWER"};
                // Shizuku.newProcess là private từ bản 13, gọi bằng reflection.
                Method m = Shizuku.class.getDeclaredMethod(
                        "newProcess", String[].class, String[].class, String.class);
                m.setAccessible(true);
                Process p = (Process) m.invoke(null, cmd, null, null);
                p.waitFor();
            } catch (Throwable t) {
                toast(ctx, "Không chạy được lệnh: " + t);
            }
        }).start();
    }

    private static void toast(final Context ctx, final String msg) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(
                () -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
    }
}

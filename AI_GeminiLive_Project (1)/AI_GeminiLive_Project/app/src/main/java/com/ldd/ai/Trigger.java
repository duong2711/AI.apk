package com.ldd.ai;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

/** Chạy lệnh giữ nút nguồn qua Shizuku và bật canh cho dịch vụ Trợ năng. */
public final class Trigger {
    private static final int REQ_CODE = 1001;
    private static boolean listenerAdded = false;

    private Trigger() {}

    public static void fire(Context context, boolean allowRequest) {
        final Context ctx = context.getApplicationContext();
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
            if (!allowRequest) {
                toast(ctx, "Chưa cấp quyền Shizuku. Hãy mở app AI một lần để cấp.");
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

    static void toast(final Context ctx, final String msg) {
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
    }
}

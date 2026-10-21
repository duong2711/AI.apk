package com.ldd.ai;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
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

    private static final String ENSURE_A11Y =
            "cur=$(settings get secure enabled_accessibility_services); "
          + "svc=com.ldd.ai/com.ldd.ai.GeminiLiveAccessibilityService; "
          + "case \"$cur\" in "
          + "*\"$svc\"*) ;; "
          + "null|\"\") settings put secure enabled_accessibility_services $svc;; "
          + "*) settings put secure enabled_accessibility_services \"$cur:$svc\";; "
          + "esac; "
          + "settings put secure accessibility_enabled 1";

    private static Process shell(String... cmd) throws Exception {
        Method m = Shizuku.class.getDeclaredMethod(
                "newProcess", String[].class, String[].class, String.class);
        m.setAccessible(true);
        return (Process) m.invoke(null, cmd, null, null);
    }

    private static void sendLongPressPower(final Context ctx) {
        new Thread(() -> {
            PowerManager.WakeLock screenWakeLock = null;
            try {
                // Đánh thức màn hình trước khi mô phỏng thao tác giữ nút nguồn.
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                screenWakeLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                                | PowerManager.ON_AFTER_RELEASE,
                        "AI:GeminiWake");
                screenWakeLock.acquire(6000);

                // Bật lại quyền Trợ năng nếu hệ thống đã tắt nó.
                try {
                    shell("sh", "-c", ENSURE_A11Y).waitFor();
                } catch (Throwable ignored) {
                }
                // Giữ 2000 ms vì đây là thời lượng đã xác nhận kích hoạt Gemini Overlay trên máy.
                shell("input", "keyevent", "--duration", "2000", "KEYCODE_POWER").waitFor();
            } catch (Throwable t) {
                toast(ctx, "Không chạy được lệnh: " + t);
            } finally {
                try {
                    if (screenWakeLock != null && screenWakeLock.isHeld()) screenWakeLock.release();
                } catch (Throwable ignored) { }
            }
        }).start();
    }

    static void toast(final Context ctx, final String msg) {
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
    }
}

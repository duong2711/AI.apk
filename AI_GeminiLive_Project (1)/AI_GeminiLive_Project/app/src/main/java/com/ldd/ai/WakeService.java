package com.ldd.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

/**
 * Dịch vụ nền nghe lệnh đánh thức tiếng Việt (offline).
 * Chỉ kích hoạt khi câu nói CHỈ gồm đúng lệnh đó:
 *  - không có tiếng nói nào ngay trước đó (khoảng lặng >= MIN_GAP_BEFORE_MS),
 *  - câu đã chốt đúng bằng một trong các câu đánh thức được cho phép,
 *  - sau đó yên lặng thêm QUIET_AFTER_MS mà không có tiếng nói mới.
 */
public class WakeService extends Service {
    private static final String CHANNEL = "wake";
    private static final String PHRASE = "ây ai";
    private static final String[] WAKE_PHRASES = {"ây ai", "mở ai", "mở a i"};
    private static final long COOLDOWN_MS = 6000;
    private static final long MIN_GAP_BEFORE_MS = 1200;
    private static final long QUIET_AFTER_MS = 1000;
    /** Điểm tin cậy tối thiểu (0..1) cho từng từ. */
    private static final double MIN_CONF = 0.85;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Model model;
    private SpeechService speech;
    private PowerManager.WakeLock cpuWakeLock;

    // Trạng thái (chỉ đụng tới trên luồng chính)
    private boolean inUtterance = false;
    private long gapBefore = Long.MAX_VALUE;
    private long lastActivity = 0L;
    private long lastFire = 0L;
    private Runnable pending = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification("AI đang nghe \"" + PHRASE + "\""));
        // Giữ CPU thức khi màn hình tắt để Vosk tiếp tục nhận diện câu lệnh.
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (cpuWakeLock == null) {
                cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AI:WakeService");
                cpuWakeLock.setReferenceCounted(false);
            }
            if (!cpuWakeLock.isHeld()) cpuWakeLock.acquire();
        } catch (Throwable ignored) { }
        if (speech == null) startEngine();
        return START_STICKY;
    }

    private Notification buildNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(
                new NotificationChannel(CHANNEL, "AI – nghe lệnh tiếng Việt", NotificationManager.IMPORTANCE_LOW));
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("AI – nghe lệnh tiếng Việt")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, buildNotification(text));
    }

    private void startEngine() {
        new Thread(() -> {
            try {
                if (!ModelManager.isReady(this)) {
                    Trigger.toast(getApplicationContext(), "Chưa có mô hình giọng nói. Mở \"AI Cài đặt\" để tải.");
                    stopSelf();
                    return;
                }
                model = new Model(ModelManager.modelDir(this).getAbsolutePath());
                Recognizer rec = new Recognizer(model, 16000.0f,
                        "[\"ây ai\", \"mở ai\", \"mở a i\", \"[unk]\"]");
                rec.setWords(true);
                speech = new SpeechService(rec, 16000.0f);
                speech.startListening(listener);
            } catch (Throwable t) {
                Trigger.toast(getApplicationContext(), "Không khởi động được nhận dạng: " + t);
                stopSelf();
            }
        }).start();
    }

    private final RecognitionListener listener = new RecognitionListener() {
        @Override public void onPartialResult(String hypothesis) {
            try {
                String p = new JSONObject(hypothesis).optString("partial", "").trim();
                if (!p.isEmpty()) handler.post(() -> onSpeech());
            } catch (Throwable ignored) { }
        }

        @Override public void onResult(String hypothesis) {
            try {
                JSONObject o = new JSONObject(hypothesis);
                final String text = o.optString("text", "").trim().toLowerCase();
                double minConf = 1.0;
                JSONArray words = o.optJSONArray("result");
                if (words != null) {
                    for (int i = 0; i < words.length(); i++) {
                        minConf = Math.min(minConf, words.getJSONObject(i).optDouble("conf", 1.0));
                    }
                }
                final double conf = minConf;
                handler.post(() -> onFinal(text, conf));
            } catch (Throwable ignored) { }
        }

        @Override public void onFinalResult(String hypothesis) { }
        @Override public void onError(Exception exception) { }
        @Override public void onTimeout() { }
    };

    /** Có tiếng nói (kết quả tạm không rỗng). */
    private void onSpeech() {
        long now = System.currentTimeMillis();
        cancelPending(); // có tiếng mới sau lệnh -> hủy
        if (!inUtterance) {
            inUtterance = true;
            gapBefore = lastActivity == 0L ? Long.MAX_VALUE : now - lastActivity;
        }
        lastActivity = now;
    }

    /** Một câu đã chốt. */
    private void onFinal(String text, double minConf) {
        inUtterance = false;
        if (text.isEmpty()) return;
        long now = System.currentTimeMillis();
        lastActivity = now;
        cancelPending();
        updateNotification("Nghe: \"" + text + "\" (" + String.format("%.2f", minConf) + ")");

        if (!isWakePhrase(text)) return;                  // chỉ nhận đúng câu đánh thức
        if (minConf < MIN_CONF) return;                   // nghe không chắc -> bỏ
        if (gapBefore < MIN_GAP_BEFORE_MS) return;        // vừa có tiếng nói trước đó -> bỏ
        if (now - lastFire < COOLDOWN_MS) return;

        // Đợi yên lặng thêm 1 giây; nếu có tiếng nói tiếp thì hủy.
        pending = () -> {
            pending = null;
            lastFire = System.currentTimeMillis();
            Trigger.fire(getApplicationContext(), false);
        };
        handler.postDelayed(pending, QUIET_AFTER_MS);
    }

    private boolean isWakePhrase(String text) {
        for (String phrase : WAKE_PHRASES) {
            if (phrase.equals(text)) return true;
        }
        return false;
    }

    private void cancelPending() {
        if (pending != null) {
            handler.removeCallbacks(pending);
            pending = null;
        }
    }

    /** Vuốt màn hình Cài đặt khỏi đa nhiệm không đồng nghĩa với yêu cầu tắt nghe.
     * Dịch vụ foreground tiếp tục chạy; Android/OEM vẫn có thể áp dụng giới hạn nền riêng. */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try {
            if (speech != null) {
                speech.stop();
                speech.shutdown();
            }
            if (model != null) model.close();
        } catch (Throwable ignored) {
        }
        speech = null;
        try {
            if (cpuWakeLock != null && cpuWakeLock.isHeld()) cpuWakeLock.release();
        } catch (Throwable ignored) { }
        cpuWakeLock = null;
        super.onDestroy();
    }
}

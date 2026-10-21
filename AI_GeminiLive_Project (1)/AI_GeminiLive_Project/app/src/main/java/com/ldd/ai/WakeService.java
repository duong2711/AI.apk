package com.ldd.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

/** Dịch vụ nền nghe cụm "Hey Gemini" (offline) rồi gọi Trigger. */
public class WakeService extends Service {
    private static final String CHANNEL = "wake";
    private static final long COOLDOWN_MS = 6000;
    /** Điểm tin cậy tối thiểu (0..1) cho từng từ. Tăng nếu còn nhận nhầm, giảm nếu khó kích hoạt. */
    private static final double MIN_CONF = 0.85;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Model model;
    private SpeechService speech;
    private long lastFire = 0L;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification("AI đang nghe \"Hey Gemini\""));
        if (speech == null) startEngine();
        return START_STICKY;
    }

    private Notification buildNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(
                new NotificationChannel(CHANNEL, "Hey Gemini", NotificationManager.IMPORTANCE_LOW));
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("AI – Hey Gemini")
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
                Recognizer rec = new Recognizer(model, 16000.0f, "[\"hey gemini\", \"[unk]\"]");
                rec.setWords(true); // để có điểm tin cậy từng từ
                speech = new SpeechService(rec, 16000.0f);
                speech.startListening(listener);
            } catch (Throwable t) {
                Trigger.toast(getApplicationContext(), "Không khởi động được nhận dạng: " + t);
                stopSelf();
            }
        }).start();
    }

    private final RecognitionListener listener = new RecognitionListener() {
        @Override public void onPartialResult(String hypothesis) { /* bỏ qua kết quả tạm */ }
        @Override public void onResult(String hypothesis) { handleFinal(hypothesis); }
        @Override public void onFinalResult(String hypothesis) { handleFinal(hypothesis); }
        @Override public void onError(Exception exception) { }
        @Override public void onTimeout() { }
    };

    private void handleFinal(String hypothesis) {
        if (hypothesis == null) return;
        try {
            JSONObject o = new JSONObject(hypothesis);
            String text = o.optString("text", "").trim().toLowerCase();
            if (text.isEmpty()) return;

            double minConf = 1.0;
            JSONArray words = o.optJSONArray("result");
            if (words != null) {
                for (int i = 0; i < words.length(); i++) {
                    minConf = Math.min(minConf, words.getJSONObject(i).optDouble("conf", 1.0));
                }
            }
            final String shown = "Nghe: \"" + text + "\" (" + String.format("%.2f", minConf) + ")";
            handler.post(() -> updateNotification(shown));

            if (!text.equals("hey gemini")) return;
            if (minConf < MIN_CONF) return;

            long now = System.currentTimeMillis();
            if (now - lastFire < COOLDOWN_MS) return;
            lastFire = now;
            handler.post(() -> Trigger.fire(getApplicationContext(), false));
        } catch (Throwable ignored) {
        }
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
        super.onDestroy();
    }
}

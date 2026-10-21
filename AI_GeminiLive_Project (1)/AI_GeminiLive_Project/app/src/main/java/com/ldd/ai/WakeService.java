package com.ldd.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

/** Dịch vụ nền nghe cụm "Hey Gemini" (offline) rồi gọi Trigger. */
public class WakeService extends Service {
    private static final String CHANNEL = "wake";
    private static final long COOLDOWN_MS = 6000;
    private static final long PAUSE_MS = 45000; // nhường micro cho Gemini

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
        startForeground(1, buildNotification());
        if (speech == null) startEngine();
        return START_STICKY;
    }

    private Notification buildNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(
                new NotificationChannel(CHANNEL, "Hey Gemini", NotificationManager.IMPORTANCE_LOW));
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("AI đang nghe \"Hey Gemini\"")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
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
                speech = new SpeechService(rec, 16000.0f);
                speech.startListening(listener);
            } catch (Throwable t) {
                Trigger.toast(getApplicationContext(), "Không khởi động được nhận dạng: " + t);
                stopSelf();
            }
        }).start();
    }

    private final RecognitionListener listener = new RecognitionListener() {
        @Override public void onPartialResult(String hypothesis) { check(hypothesis); }
        @Override public void onResult(String hypothesis) { check(hypothesis); }
        @Override public void onFinalResult(String hypothesis) { }
        @Override public void onError(Exception exception) { }
        @Override public void onTimeout() { }
    };

    private void check(String hypothesis) {
        if (hypothesis == null) return;
        if (!hypothesis.toLowerCase().contains("gemini")) return;
        long now = System.currentTimeMillis();
        if (now - lastFire < COOLDOWN_MS) return;
        lastFire = now;
        handler.post(() -> {
            Trigger.fire(getApplicationContext(), false);
            if (speech != null) {
                speech.setPause(true);
                handler.postDelayed(() -> {
                    if (speech != null) speech.setPause(false);
                }, PAUSE_MS);
            }
        });
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

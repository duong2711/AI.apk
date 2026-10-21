package com.ldd.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Màn hình cài đặt: bật/tắt nghe lệnh đánh thức tiếng Việt. */
public class SetupActivity extends Activity {
    private static final int REQ_MIC = 7;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("AI – nghe lệnh tiếng Việt");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, pad, 0, pad);
        status.setText("Bấm \"Bật nghe Ây Ai\". Lần đầu sẽ tải mô hình giọng nói (~40MB).");
        layout.addView(status);

        Button start = new Button(this);
        start.setText("Bật nghe Ây Ai");
        start.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            } else {
                startAll();
            }
        });
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("Tắt nghe");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, WakeService.class));
            setStatus("Đã tắt.");
        });
        layout.addView(stop);

        Button test = new Button(this);
        test.setText("Thử mở Gemini ngay");
        test.setOnClickListener(v -> Trigger.fire(this, true));
        layout.addView(test);

        setContentView(layout);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAll();
        } else {
            setStatus("Cần cấp quyền micro để nghe lệnh \"Ây Ai\".");
        }
    }

    private void startAll() {
        new Thread(() -> {
            if (!ModelManager.isReady(this)) {
                setStatus("Đang tải mô hình giọng nói...");
                try {
                    ModelManager.download(this, this::setStatus);
                } catch (Throwable t) {
                    setStatus("Tải mô hình lỗi: " + t.getMessage());
                    return;
                }
            }
            runOnUiThread(() -> {
                startForegroundService(new Intent(this, WakeService.class));
                status.setText("Đang nghe \"Ây Ai\" / \"Mở AI\". Có thể vuốt màn hình Cài đặt khỏi đa nhiệm; dịch vụ nghe nền tiếp tục nếu Android cho phép.");
            });
        }).start();
    }

    private void setStatus(final String s) {
        runOnUiThread(() -> status.setText(s));
    }
}

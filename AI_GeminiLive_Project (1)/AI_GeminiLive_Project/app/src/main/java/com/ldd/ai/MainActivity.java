package com.ldd.ai;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String GEMINI_PACKAGE = "com.google.android.googlequicksearchbox";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("AI");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView instructions = new TextView(this);
        instructions.setText("Lần đầu dùng: bật quyền Trợ năng cho “AI – mở Gemini Live”.\n\nSau đó, khi Xiaomi AI mở ứng dụng AI này, ứng dụng sẽ mở Gemini và tự bấm nút Open Gemini Live nếu nút đó xuất hiện.");
        instructions.setTextSize(16);
        instructions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(0, pad, 0, pad);
        layout.addView(instructions, textParams);

        Button accessibility = new Button(this);
        accessibility.setText("Mở cài đặt Trợ năng");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(accessibility);

        Button run = new Button(this);
        run.setText("Mở Gemini Live");
        run.setOnClickListener(v -> launchGemini());
        layout.addView(run);

        setContentView(layout);

        // Khi được mở bằng Xiaomi AI, tự khởi chạy Gemini.
        layout.postDelayed(this::launchGemini, 500);
    }

    private void launchGemini() {
        try {
            PackageManager pm = getPackageManager();
            Intent launch = pm.getLaunchIntentForPackage(GEMINI_PACKAGE);
            if (launch == null) {
                Toast.makeText(this, "Không tìm thấy ứng dụng Google/Gemini. Hãy cài hoặc cập nhật Google app.", Toast.LENGTH_LONG).show();
                return;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (Exception e) {
            Toast.makeText(this, "Không mở được Gemini: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}

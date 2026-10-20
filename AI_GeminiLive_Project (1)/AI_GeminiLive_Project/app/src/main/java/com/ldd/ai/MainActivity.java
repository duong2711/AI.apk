package com.ldd.ai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Không có giao diện: mỗi lần được mở (kể cả khi đã nằm trong đa nhiệm)
 * sẽ gọi trợ lý mặc định (Gemini) giống như giữ nút nguồn, rồi tự đóng.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callAssistant();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        callAssistant();
    }

    private void callAssistant() {
        boolean ok = tryStart(Intent.ACTION_ASSIST) || tryStart(Intent.ACTION_VOICE_COMMAND);
        if (!ok) {
            Toast.makeText(this,
                    "Không gọi được trợ lý. Hãy đặt Gemini làm trợ lý kỹ thuật số mặc định.",
                    Toast.LENGTH_LONG).show();
        }
        finishAndRemoveTask();
    }

    private boolean tryStart(String action) {
        try {
            Intent i = new Intent(action);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

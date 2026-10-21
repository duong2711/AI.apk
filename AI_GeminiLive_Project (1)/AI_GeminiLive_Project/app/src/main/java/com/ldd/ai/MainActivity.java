package com.ldd.ai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Không giao diện: mỗi lần mở sẽ gọi Gemini rồi tự đóng. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Trigger.fire(this, true);
        finishAndRemoveTask();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Trigger.fire(this, true);
        finishAndRemoveTask();
    }
}

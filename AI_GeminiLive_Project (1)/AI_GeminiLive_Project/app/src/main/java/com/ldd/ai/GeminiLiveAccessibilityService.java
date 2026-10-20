package com.ldd.ai;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class GeminiLiveAccessibilityService extends AccessibilityService {
    private static final String GEMINI_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String LIVE_BUTTON_ID =
            "com.google.android.googlequicksearchbox:id/assistant_robin_input_voice_chat_button_compose";

    /** Chỉ được tự bấm trước thời điểm này (do MainActivity đặt). */
    public static volatile long armedUntil = 0L;

    private long lastClickAt = 0L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        long now = System.currentTimeMillis();
        if (now > armedUntil) return;
        if (now - lastClickAt < 1500) return;
        if (event == null || event.getPackageName() == null) return;
        if (!GEMINI_PACKAGE.contentEquals(event.getPackageName())) return;

        // Thử cửa sổ đang hoạt động trước, rồi tới mọi cửa sổ (overlay).
        if (tryRoot(getRootInActiveWindow(), now)) return;
        List<android.view.accessibility.AccessibilityWindowInfo> windows = getWindows();
        if (windows != null) {
            for (android.view.accessibility.AccessibilityWindowInfo w : windows) {
                if (tryRoot(w.getRoot(), now)) return;
            }
        }
    }

    private boolean tryRoot(AccessibilityNodeInfo root, long now) {
        if (root == null) return false;
        if (root.getPackageName() == null
                || !GEMINI_PACKAGE.contentEquals(root.getPackageName())) return false;

        List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByViewId(LIVE_BUTTON_ID);
        if (matches != null) {
            for (AccessibilityNodeInfo node : matches) {
                if (node != null && node.isVisibleToUser() && node.isEnabled()
                        && clickNodeOrParent(node)) {
                    done(now);
                    return true;
                }
            }
        }
        return clickByDescription(root, "Open Gemini Live", now);
    }

    private void done(long now) {
        lastClickAt = now;
        armedUntil = 0L; // chỉ bấm một lần cho mỗi lần gọi
    }

    private boolean clickByDescription(AccessibilityNodeInfo node, String description, long now) {
        if (node == null) return false;
        CharSequence desc = node.getContentDescription();
        if (desc != null && description.contentEquals(desc)
                && node.isVisibleToUser() && node.isEnabled() && clickNodeOrParent(node)) {
            done(now);
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (clickByDescription(node.getChild(i), description, now)) return true;
        }
        return false;
    }

    private boolean clickNodeOrParent(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        for (int i = 0; i < 4 && current != null; i++) {
            if (current.isClickable() && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
            current = current.getParent();
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    @Override
    public void onInterrupt() {
    }
}

package com.ldd.ai;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class GeminiLiveAccessibilityService extends AccessibilityService {
    private static final String GEMINI_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String LIVE_BUTTON_ID =
            "com.google.android.googlequicksearchbox:id/assistant_robin_input_voice_chat_button_compose";
    private long lastClickAt = 0L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!GEMINI_PACKAGE.contentEquals(event.getPackageName())) return;

        long now = System.currentTimeMillis();
        if (now - lastClickAt < 2500) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        // Ưu tiên tìm đúng nút theo resource-id đã xác nhận trên máy người dùng.
        List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByViewId(LIVE_BUTTON_ID);
        if (matches != null) {
            for (AccessibilityNodeInfo node : matches) {
                if (node != null && node.isVisibleToUser() && node.isEnabled()) {
                    if (clickNodeOrParent(node)) {
                        lastClickAt = now;
                        return;
                    }
                }
            }
        }

        // Dự phòng: tìm theo mô tả trợ năng "Open Gemini Live".
        clickByDescription(root, "Open Gemini Live", now);
    }

    private boolean clickByDescription(AccessibilityNodeInfo node, String description, long now) {
        if (node == null) return false;
        CharSequence desc = node.getContentDescription();
        if (desc != null && description.contentEquals(desc) && node.isVisibleToUser() && node.isEnabled()) {
            if (clickNodeOrParent(node)) {
                lastClickAt = now;
                return true;
            }
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
        // Không cần xử lý.
    }
}

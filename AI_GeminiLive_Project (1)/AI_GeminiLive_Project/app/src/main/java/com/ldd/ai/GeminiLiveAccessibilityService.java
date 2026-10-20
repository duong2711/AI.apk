package com.ldd.ai;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeminiLiveAccessibilityService extends AccessibilityService {
    private static final String GEMINI_PACKAGE = "com.google.android.googlequicksearchbox";
    private static final String LIVE_BUTTON_ID =
            "com.google.android.googlequicksearchbox:id/assistant_robin_input_voice_chat_button_compose";

    /** Chỉ được tự bấm trước thời điểm này (do MainActivity đặt). */
    public static volatile long armedUntil = 0L;

    private long lastClickAt = 0L;
    private long lastDumpFor = -1L;
    private boolean sawGemini = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        long now = System.currentTimeMillis();
        long armed = armedUntil;
        if (armed == 0L || now > armed) return;
        if (event == null || event.getPackageName() == null) return;
        if (!GEMINI_PACKAGE.contentEquals(event.getPackageName())) return;
        sawGemini = true;

        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        AccessibilityNodeInfo active = getRootInActiveWindow();
        if (active != null) roots.add(active);
        List<AccessibilityWindowInfo> windows = getWindows();
        if (windows != null) {
            for (AccessibilityWindowInfo w : windows) {
                AccessibilityNodeInfo r = w.getRoot();
                if (r != null) roots.add(r);
            }
        }

        if (now - lastClickAt >= 1200) {
            for (AccessibilityNodeInfo root : roots) {
                if (tryRoot(root, now)) return;
            }
        }

        // Chẩn đoán: sau 3 giây mà chưa bấm được thì chép danh sách nút vào bộ nhớ tạm.
        long armedAt = armed - 10000;
        if (now - armedAt > 3000 && lastDumpFor != armed) {
            lastDumpFor = armed;
            StringBuilder sb = new StringBuilder("Gemini nodes (" + roots.size() + " windows):\n");
            for (AccessibilityNodeInfo root : roots) {
                if (root.getPackageName() != null
                        && GEMINI_PACKAGE.contentEquals(root.getPackageName())) {
                    sb.append("-- window --\n");
                    dump(root, sb, new int[]{0});
                }
            }
            copy(sb.toString());
            Toast.makeText(this, "AI: đã chép danh sách nút Gemini vào bộ nhớ tạm. Hãy dán gửi cho mình.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean tryRoot(AccessibilityNodeInfo root, long now) {
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
        return clickFuzzy(root, now);
    }

    private boolean clickFuzzy(AccessibilityNodeInfo node, long now) {
        if (node == null) return false;
        String id = node.getViewIdResourceName();
        CharSequence d = node.getContentDescription();
        String desc = d == null ? "" : d.toString().toLowerCase(Locale.ROOT);
        boolean idHit = id != null && id.contains("voice_chat");
        boolean descHit = desc.contains("gemini live") || desc.equals("live")
                || desc.contains("trực tiếp") || desc.contains("实时") || desc.contains("實時");
        if ((idHit || descHit) && node.isVisibleToUser() && node.isEnabled() && clickNodeOrParent(node)) {
            done(now);
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (clickFuzzy(node.getChild(i), now)) return true;
        }
        return false;
    }

    private void dump(AccessibilityNodeInfo node, StringBuilder sb, int[] count) {
        if (node == null || count[0] > 120 || sb.length() > 6000) return;
        String id = node.getViewIdResourceName();
        CharSequence d = node.getContentDescription();
        CharSequence t = node.getText();
        if (id != null || (d != null && d.length() > 0) || (t != null && t.length() > 0)) {
            count[0]++;
            sb.append(id == null ? "-" : id.replace(GEMINI_PACKAGE + ":id/", ""))
              .append(" | desc=").append(d == null ? "" : d)
              .append(" | text=").append(t == null ? "" : t)
              .append(" | click=").append(node.isClickable())
              .append(" | vis=").append(node.isVisibleToUser())
              .append("\n");
        }
        for (int i = 0; i < node.getChildCount(); i++) dump(node.getChild(i), sb, count);
    }

    private void copy(String s) {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("gemini-nodes", s));
        } catch (Throwable ignored) {
        }
    }

    private void done(long now) {
        lastClickAt = now;
        armedUntil = 0L;
        Toast.makeText(this, "AI: đã bấm nút Gemini Live", Toast.LENGTH_SHORT).show();
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

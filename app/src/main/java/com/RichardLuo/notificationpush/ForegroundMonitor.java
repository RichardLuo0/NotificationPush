package com.RichardLuo.notificationpush;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class ForegroundMonitor extends AccessibilityService {
    static String packageName = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.getContentChangeTypes() == 0) {
            if (event.getPackageName() == null || event.getPackageName().toString().contains("inputmethod") || event.getPackageName().toString().contains("system"))
                return;
            packageName = event.getPackageName().toString();
        }
    }

    @Override
    public void onInterrupt() {

    }
}

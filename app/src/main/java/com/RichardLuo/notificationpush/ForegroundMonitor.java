package com.RichardLuo.notificationpush;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class ForegroundMonitor extends AccessibilityService {
    static String packageName = "";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (event.getPackageName() == null)
                return;
            packageName = event.getPackageName().toString();
        }
    }

    @Override
    public void onInterrupt() {

    }
}

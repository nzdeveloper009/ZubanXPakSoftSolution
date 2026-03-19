package com.android.zubanx.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

// Full implementation in Plan 7: Visual Features
class AccessibilityTranslateService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}

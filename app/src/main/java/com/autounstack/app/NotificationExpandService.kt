package com.autounstack.app

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class NotificationExpandService : AccessibilityService() {
    private var lastGlobalClickTime = 0L
    private lateinit var preferencesManager: PreferencesManager

    private companion object {
        private const val TAG = "NotificationExpandService"
        private const val GLOBAL_CLICK_COOLDOWN_MS = 450L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        preferencesManager = PreferencesManager(this)
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!::preferencesManager.isInitialized) {
            preferencesManager = PreferencesManager(this)
        }

        if (!preferencesManager.isServiceEnabled()) {
            Log.d(TAG, "Service disabled; ignoring event")
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val now = SystemClock.uptimeMillis()
        if (now - lastGlobalClickTime < GLOBAL_CLICK_COOLDOWN_MS) {
            Log.d(TAG, "Global cooldown active")
            return
        }

        // Only handle events coming from SystemUI
        val evPkg = event.packageName?.toString()
        if (evPkg == null || evPkg != "com.android.systemui") {
            Log.d(TAG, "Ignoring event from package=$evPkg")
            return
        }

        val root = rootInActiveWindow
        if (root == null) {
            Log.d(TAG, "No active window root available for event type ${event.eventType}")
            return
        }

        try {
            val screenBounds = Rect()
            root.getBoundsInScreen(screenBounds)
            val screenWidth = screenBounds.width().takeIf { it > 0 } ?: resources.displayMetrics.widthPixels

            Log.d(TAG, "SystemUI event; scanning node tree; eventType=${event.eventType}, screenWidth=$screenWidth")
            scanNodeRecursive(root, screenWidth)
        } finally {
            root.recycle()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    private fun scanNodeRecursive(node: AccessibilityNodeInfo, screenWidth: Int): Boolean {
        if (processNode(node, screenWidth)) {
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (scanNodeRecursive(child, screenWidth)) {
                    return true
                }
            } finally {
                child.recycle()
            }
        }

        return false
    }


    private fun processNode(node: AccessibilityNodeInfo, screenWidth: Int): Boolean {
        val text = node.text?.toString()
        if (!NotificationHeuristics.isNumericBadgeText(text)) {
            return false
        }

        if (!node.isVisibleToUser) {
            Log.d(TAG, "Skipping numeric node because it is not visible: text=$text")
            return false
        }

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty || !NotificationHeuristics.isNearRightSide(bounds.right, screenWidth)) {
            Log.d(TAG, "Skipping numeric node because it is not near right side: text=$text bounds=$bounds")
            return false
        }

        Log.d(TAG, "Detected numeric badge node: text=$text bounds=$bounds")

        val clickableParent = findClickableParent(node)
        if (clickableParent == null) {
            Log.d(TAG, "No clickable parent found for numeric badge: text=$text bounds=$bounds")
            return false
        }

        try {
            Log.d(TAG, "Attempting click on clickable parent for numeric badge: text=$text")
            val clicked = clickableParent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) {
                lastGlobalClickTime = SystemClock.uptimeMillis()
                Log.d(TAG, "Click performed")
                return true
            }

            Log.d(TAG, "Click failed for numeric badge: text=$text")
        } finally {
            if (clickableParent != node) {
                clickableParent.recycle()
            }
        }

        return false
    }

    private fun findClickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                Log.d(TAG, "Found clickable parent: class=${current.className} clickable=true")
                return current
            }
            val parent = current.parent
            if (current != node) {
                current.recycle()
            }
            current = parent
        }
        return null
    }
}

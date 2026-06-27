package com.autounstack.app

internal object NotificationHeuristics {
    private val numericBadgePattern = Regex("^[0-9]+$")

    fun isNumericBadgeText(text: String?): Boolean {
        if (text.isNullOrBlank()) {
            return false
        }

        return text.trim().matches(numericBadgePattern)
    }

    fun isNearRightSide(rightEdge: Int, screenWidth: Int): Boolean {
        if (rightEdge <= 0 || screenWidth <= 0) {
            return false
        }

        val threshold = (screenWidth * 0.75f).toInt()
        return rightEdge >= threshold
    }
}

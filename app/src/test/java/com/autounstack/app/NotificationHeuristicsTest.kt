package com.autounstack.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationHeuristicsTest {
    @Test
    fun `numeric badge text accepts trimmed digits`() {
        assertTrue(NotificationHeuristics.isNumericBadgeText(" 12 "))
    }

    @Test
    fun `numeric badge text rejects blanks and mixed text`() {
        assertFalse(NotificationHeuristics.isNumericBadgeText(null))
        assertFalse(NotificationHeuristics.isNumericBadgeText(""))
        assertFalse(NotificationHeuristics.isNumericBadgeText("2 new"))
        assertFalse(NotificationHeuristics.isNumericBadgeText("1,024"))
    }

    @Test
    fun `right side detection uses final quarter of screen`() {
        assertTrue(NotificationHeuristics.isNearRightSide(rightEdge = 750, screenWidth = 1000))
        assertTrue(NotificationHeuristics.isNearRightSide(rightEdge = 900, screenWidth = 1000))
        assertFalse(NotificationHeuristics.isNearRightSide(rightEdge = 749, screenWidth = 1000))
    }

    @Test
    fun `right side detection rejects invalid bounds`() {
        assertFalse(NotificationHeuristics.isNearRightSide(rightEdge = 0, screenWidth = 1000))
        assertFalse(NotificationHeuristics.isNearRightSide(rightEdge = 750, screenWidth = 0))
    }
}

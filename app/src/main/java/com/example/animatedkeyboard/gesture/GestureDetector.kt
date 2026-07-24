package com.example.animatedkeyboard.gesture

import android.graphics.PointF
import kotlin.math.sqrt

/**
 * Detects gesture patterns from touch points.
 * Used for swipe-to-type and cursor control.
 */
class GestureDetector {

    private val pathPoints = mutableListOf<PointF>()
    private var lastPoint: PointF? = null
    private var isActive = false

    fun startGesture(x: Float, y: Float) {
        pathPoints.clear()
        pathPoints.add(PointF(x, y))
        lastPoint = PointF(x, y)
        isActive = true
    }

    fun addPoint(x: Float, y: Float) {
        if (!isActive) return
        val last = lastPoint ?: return
        val distance = sqrt((x - last.x) * (x - last.x) + (y - last.y) * (y - last.y))
        if (distance > 10f) { // Only add if moved enough
            pathPoints.add(PointF(x, y))
            lastPoint = PointF(x, y)
        }
    }

    fun endGesture(): List<PointF> {
        isActive = false
        return pathPoints.toList()
    }

    fun isSwiping(): Boolean = isActive && pathPoints.size > 2

    fun getPath(): List<PointF> = pathPoints

    fun clear() {
        pathPoints.clear()
        lastPoint = null
        isActive = false
    }

    /**
     * Simplified gesture to word mapping: uses the bounding box of the path
     * and computes a rough sequence of keys; in practice, we'd use a more
     * sophisticated algorithm (e.g., dynamic time warping).
     * For now, we'll just use the first and last keys to determine the word.
     */
    fun getGesturePath(): String {
        if (pathPoints.size < 3) return ""
        // Simplified: return the path as a string of coordinates (for debug)
        // In a real implementation, we'd map each point to a key and build a word.
        // For now, we'll just return the path length as a placeholder.
        return "swipe"
    }
}

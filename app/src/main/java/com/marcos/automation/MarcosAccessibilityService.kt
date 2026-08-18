package com.marcos.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MarcosAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MarcosAccessibility"
        private const val HTTP_PORT = 8482
        var instance: MarcosAccessibilityService? = null
    }

    private var httpServer: CommandServer? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "MARCOS Accessibility Service connected")

        httpServer = CommandServer(HTTP_PORT, this)
        try {
            httpServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.i(TAG, "Command server listening on http://localhost:$HTTP_PORT")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start command server", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally minimal — screen content is pulled on-demand via
        // readScreen() rather than logging every event.
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        httpServer?.stop()
        instance = null
    }

    fun readScreen(): JSONArray {
        val results = JSONArray()
        val root = rootInActiveWindow ?: return results
        collectNodes(root, results)
        return results
    }

    private fun collectNodes(node: AccessibilityNodeInfo, out: JSONArray) {
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()

        if (!text.isNullOrBlank() || !desc.isNullOrBlank()) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)

            val obj = JSONObject()
            obj.put("text", text ?: "")
            obj.put("description", desc ?: "")
            obj.put("clickable", node.isClickable)
            obj.put("className", node.className?.toString() ?: "")

            val boundsObj = JSONObject()
            boundsObj.put("left", bounds.left)
            boundsObj.put("top", bounds.top)
            boundsObj.put("right", bounds.right)
            boundsObj.put("bottom", bounds.bottom)

            obj.put("bounds", boundsObj)
            out.put(obj)
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, out) }
        }
    }

    fun tapAt(x: Float, y: Float, callback: (Boolean) -> Unit) {
        val path = Path()
        path.moveTo(x, y)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(
            GestureDescription.StrokeDescription(path, 0, 100)
        )

        dispatchGesture(
            gestureBuilder.build(),
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    callback(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    callback(false)
                }
            },
            null
        )
    }

    fun tapNodeContainingText(query: String, callback: (Boolean) -> Unit) {
        val root = rootInActiveWindow

        if (root == null) {
            callback(false)
            return
        }

        val match = findNodeByText(root, query.lowercase())

        if (match == null) {
            callback(false)
            return
        }

        val bounds = android.graphics.Rect()
        match.getBoundsInScreen(bounds)

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        tapAt(centerX, centerY, callback)
    }

    private fun findNodeByText(
        node: AccessibilityNodeInfo,
        query: String
    ): AccessibilityNodeInfo? {

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        if ((text.contains(query) || desc.contains(query)) && node.isClickable) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue

            val result = findNodeByText(child, query)
            if (result != null) return result
        }

        return null
    }
}

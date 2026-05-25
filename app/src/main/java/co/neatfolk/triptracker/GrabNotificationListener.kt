package co.neatfolk.triptracker

import android.content.ComponentName
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Listens for Grab booking notifications and fires a broadcast
 * so FloatingOverlayService can auto-start a trip.
 *
 * Setup required (one-time):
 *   Settings → Notifications → Notification access → Trip Tracker → Enable
 *
 * Grab package: com.grabtaxi.driver2
 * Booking accepted keywords: "booking", "accepted", "passenger", "pickup"
 */
class GrabNotificationListener : NotificationListenerService() {

    companion object {
        const val ACTION_GRAB_BOOKING = "co.neatfolk.triptracker.GRAB_BOOKING_DETECTED"
        const val EXTRA_NOTIF_TEXT    = "notif_text"
        private const val TAG         = "GrabNotifListener"

        // Grab driver app package name
        private const val GRAB_PACKAGE = "com.grabtaxi.driver2"

        // Keywords that indicate a new booking was accepted
        // Grab sends notifications like "New booking" or "Booking accepted"
        private val BOOKING_KEYWORDS = listOf(
            "booking", "accepted", "new trip", "passenger",
            "picked up", "pick up", "new job"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName != GRAB_PACKAGE) return

        val notif     = sbn.notification ?: return
        val extras    = notif.extras ?: return
        val title     = extras.getString("android.title", "") ?: ""
        val text      = extras.getString("android.text",  "") ?: ""
        val combined  = "$title $text".lowercase()

        Log.d(TAG, "Grab notification: title='$title' text='$text'")

        // Check if this looks like a new booking notification
        val isBooking = BOOKING_KEYWORDS.any { combined.contains(it) }
        if (!isBooking) return

        Log.i(TAG, "Grab booking detected — broadcasting auto-start")

        // Broadcast to FloatingOverlayService
        val intent = Intent(ACTION_GRAB_BOOKING).apply {
            putExtra(EXTRA_NOTIF_TEXT, "$title — $text")
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not used
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        Log.w(TAG, "Notification listener disconnected")
    }
}

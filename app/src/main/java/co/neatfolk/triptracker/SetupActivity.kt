package co.neatfolk.triptracker

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * One-time setup screen shown on first launch.
 * Guides Roy to enable the Accessibility Service.
 * Never shown again once AS is confirmed enabled.
 */
class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        findViewById<Button>(R.id.btnEnableAS)?.setOnClickListener {
            // Open Android Accessibility settings directly
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnSetupDone)?.setOnClickListener {
            if (isAccessibilityServiceEnabled()) {
                // AS confirmed — go to main app
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                findViewById<TextView>(R.id.tvSetupStatus)?.apply {
                    text = "Trip Tracker is not enabled yet.\nGo to Accessibility → Installed Apps → Trip Tracker → Enable"
                    setTextColor(0xFFE53E3E.toInt())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Update status when Roy returns from Settings
        if (isAccessibilityServiceEnabled()) {
            findViewById<TextView>(R.id.tvSetupStatus)?.apply {
                text = "✓ Auto capture enabled — you're all set!"
                setTextColor(0xFF38A169.toInt())
            }
            findViewById<Button>(R.id.btnSetupDone)?.text = "Continue to Trip Tracker"
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(packageName, ignoreCase = true)
    }

    companion object {
        fun isEnabled(context: android.content.Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(context.packageName, ignoreCase = true)
        }
    }
}

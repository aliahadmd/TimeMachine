package me.aliahad.timemanager.focus

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.Condition
import android.util.Log
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.R

object FocusZenManager {
    private const val TAG = "FocusZenManager"
    private const val PREFS_NAME = "tm_focus_zen_prefs"
    private const val KEY_RULE_ID = "automatic_zen_rule_id"
    private val CONDITION_URI: Uri = Uri.parse("condition://me.aliahad.timemanager/focus")

    fun hasDndPolicyAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        return nm.isNotificationPolicyAccessGranted
    }

    fun openDndPolicySettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Throwable) {
            }
        }
    }

    fun setFocusZenActive(context: Context, active: Boolean, label: String = "Focus Session"): Boolean {
        if (!hasDndPolicyAccess(context)) return false
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return false
        return try {
            val ruleId = ensureZenRule(context, nm) ?: return false
            val state = if (active) Condition.STATE_TRUE else Condition.STATE_FALSE
            val summary = if (active) "Active: $label" else "Inactive"
            nm.setAutomaticZenRuleState(
                ruleId,
                Condition(CONDITION_URI, summary, state)
            )
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to toggle AutomaticZenRule (active=$active): ${t.message}")
            false
        }
    }

    private fun ensureZenRule(context: Context, nm: NotificationManager): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingId = prefs.getString(KEY_RULE_ID, null)
        if (!existingId.isNullOrBlank() && nm.getAutomaticZenRule(existingId) != null) {
            return existingId
        }

        val rule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            AutomaticZenRule.Builder("TimeMachine Focus", CONDITION_URI)
                .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                .setEnabled(true)
                .setManualInvocationAllowed(true)
                .setType(AutomaticZenRule.TYPE_IMMERSIVE)
                .setIconResId(R.drawable.ic_stat_timemachine)
                .setConfigurationActivity(ComponentName(context, MainActivity::class.java))
                .build()
        } else {
            @Suppress("DEPRECATION")
            AutomaticZenRule(
                "TimeMachine Focus",
                null,
                ComponentName(context, MainActivity::class.java),
                CONDITION_URI,
                null,
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                true
            )
        }

        val newId = nm.addAutomaticZenRule(rule)
        if (!newId.isNullOrBlank()) {
            prefs.edit().putString(KEY_RULE_ID, newId).apply()
        }
        return newId
    }
}

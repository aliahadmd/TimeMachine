package me.aliahad.timemanager.system

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import me.aliahad.timemanager.MainActivity
import me.aliahad.timemanager.R
import me.aliahad.timemanager.data.ActivityCategory
import me.aliahad.timemanager.notifications.NotificationChannelRegistry

object AppShortcutsManager {

    fun syncDynamicShortcuts(context: Context, categories: List<ActivityCategory>) {
        val maxSlots = (ShortcutManagerCompat.getMaxShortcutCountPerActivity(context) - 3)
            .coerceIn(0, 2)
        if (maxSlots == 0) {
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)
            return
        }

        val shortcuts = categories
            .filter { it.isActive }
            .take(maxSlots)
            .map { category ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(
                        NotificationChannelRegistry.EXTRA_ROUTE_TARGET,
                        NotificationChannelRegistry.ROUTE_FOCUS
                    )
                    putExtra(NotificationChannelRegistry.EXTRA_CATEGORY_ID, category.id)
                    putExtra(NotificationChannelRegistry.EXTRA_AUTO_START_FOCUS, true)
                }
                ShortcutInfoCompat.Builder(context, "focus_cat_${category.id}")
                    .setShortLabel("${category.icon} ${category.name}".take(18))
                    .setLongLabel("Start ${category.name} Focus")
                    .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(intent)
                    .build()
            }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}

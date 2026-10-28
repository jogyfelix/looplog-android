package com.jolabs.looplog.habit.widgets.addHabit

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.jolabs.looplog.habit.R

class CreateShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Create the target intent triggered when the shortcut is tapped
        val shortcutIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("looplog://habit/create?habitId=0")
            // Specify the target package and class explicitly to open the app correctly
            setClassName("com.jolabs.looplog", "com.jolabs.looplog.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        // 2. Wrap it in a shortcut result intent
        val resultIntent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.add_habit))
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, 
                Intent.ShortcutIconResource.fromContext(this@CreateShortcutActivity, R.drawable.ic_shortcut_add_habit))
        }

        // 3. Return successfully to the launcher and close
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}

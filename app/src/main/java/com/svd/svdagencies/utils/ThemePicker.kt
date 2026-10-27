package com.svd.svdagencies.utils

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.svd.svdagencies.R

/** Theme selection is independent of the retired admin App Tools entry. */
object ThemePicker {
    @JvmStatic
    fun show(activity: AppCompatActivity): AlertDialog {
        val choices = arrayOf(activity.getString(R.string.theme_ruby), activity.getString(R.string.theme_ocean), activity.getString(R.string.theme_forest))
        val current = Appearance.selected(activity)
        return WhiteMaterialAlertDialogBuilder(activity)
            .setTitle("Change theme")
            .setSingleChoiceItems(choices, current) { dialog, choice ->
                dialog.dismiss()
                if (choice != current) {
                    Appearance.save(activity, choice)
                    activity.recreate()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

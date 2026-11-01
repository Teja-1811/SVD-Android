package com.svd.svdagencies.utils

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

object ThemePicker {
    fun choices(activity:AppCompatActivity, selected:()->Unit = {}):View {
        val density=activity.resources.displayMetrics.density
        fun dp(v:Int)=(v*density).toInt()
        val list=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL }
        val colors=intArrayOf(Color.rgb(185,28,28),Color.rgb(2,132,199),Color.rgb(21,128,61))
        listOf("Portal default","Ocean","Forest").forEachIndexed { index,name ->
            val row=LinearLayout(activity).apply { orientation=LinearLayout.HORIZONTAL;gravity=android.view.Gravity.CENTER_VERTICAL;minimumHeight=dp(56);setPadding(dp(12),dp(8),dp(12),dp(8));isClickable=true;isFocusable=true;contentDescription=name+if(Appearance.selected(activity)==index) ", selected" else "" }
            val circle=View(activity).apply { background=GradientDrawable().apply { shape=GradientDrawable.OVAL;setColor(colors[index]) } }
            row.addView(circle,LinearLayout.LayoutParams(dp(24),dp(24)).apply { marginEnd=dp(16) })
            row.addView(TextView(activity).apply { text=name;textSize=16f;setTextColor(Color.rgb(24,59,71)) },LinearLayout.LayoutParams(0,-2,1f))
            if(Appearance.selected(activity)==index)row.addView(TextView(activity).apply { text="✓";textSize=20f;setTextColor(colors[index]) })
            row.setOnClickListener { selected();if(Appearance.selected(activity)!=index){Appearance.save(activity,index);activity.recreate()} }
            list.addView(row)
        }
        return list
    }
    @JvmStatic fun show(activity:AppCompatActivity):AlertDialog {
        val dialog=WhiteMaterialAlertDialogBuilder(activity).setTitle("Appearance").setNegativeButton("Cancel",null).create()
        dialog.setView(choices(activity){dialog.dismiss()});dialog.show();return dialog
    }
}

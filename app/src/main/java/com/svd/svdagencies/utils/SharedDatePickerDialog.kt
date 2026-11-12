package com.svd.svdagencies.utils

import android.app.DatePickerDialog.OnDateSetListener
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.DatePicker
import com.google.android.material.button.MaterialButton
import com.svd.svdagencies.R

/** One reusable calendar and fixed action bar for every date selector. */
class SharedDatePickerDialog(context: Context, private val listener: OnDateSetListener?, year: Int, month: Int, day: Int) : Dialog(context, R.style.Theme_Svd_SharedDatePicker) {
    private val content = LayoutInflater.from(this.context).inflate(R.layout.svd_date_picker_dialog, null)
    val datePicker: DatePicker = content.findViewById(R.id.sharedDatePicker)
    constructor(context: Context, @Suppress("UNUSED_PARAMETER") theme: Int, listener: OnDateSetListener?, year: Int, month: Int, day: Int) : this(context, listener, year, month, day)
    init { datePicker.init(year, month, day, null) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(content)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        content.findViewById<MaterialButton>(R.id.btnCancelSharedDate).setOnClickListener { cancel() }
        content.findViewById<MaterialButton>(R.id.btnSelectSharedDate).setOnClickListener {
            datePicker.clearFocus()
            listener?.onDateSet(datePicker, datePicker.year, datePicker.month, datePicker.dayOfMonth)
            dismiss()
        }
    }
    override fun onStart() {
        super.onStart()
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val insets = window?.decorView?.rootWindowInsets
        val bars = (insets?.systemWindowInsetTop ?: 0) + (insets?.systemWindowInsetBottom ?: 0)
        val width = minOf((360 * density).toInt(), metrics.widthPixels - (24 * density).toInt()).coerceAtLeast(1)
        val height = minOf((520 * density).toInt(), metrics.heightPixels - bars - (32 * density).toInt()).coerceAtLeast(1)
        window?.setLayout(width, height)
    }
}

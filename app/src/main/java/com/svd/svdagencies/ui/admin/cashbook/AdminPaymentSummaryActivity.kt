package com.svd.svdagencies.ui.admin.cashbook

import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.svd.svdagencies.R
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.PaymentConfig
import com.svd.svdagencies.utils.SharedDatePickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

open class AdminPaymentSummaryActivity : AdminBaseActivity() {

    private lateinit var receiptContainer: LinearLayout
    private lateinit var cardReceiptMain: MaterialCardView
    private lateinit var tvReceiptStatus: TextView
    private lateinit var tvReceiptDateTime: TextView
    private lateinit var tvReceiptPaidToLabel: TextView
    private lateinit var tvReceiptRecipientName: TextView
    private lateinit var tvReceiptRecipientAccount: TextView
    private lateinit var tvReceiptRecipientBank: TextView
    private lateinit var tvReceiptAmount: TextView
    private lateinit var tvReceiptBankingNameLabel: TextView
    private lateinit var tvReceiptBankingNameColon: TextView
    private lateinit var tvReceiptBankingName: TextView
    private lateinit var tvReceiptTransferDetailsLabel: TextView
    private lateinit var ivExpandChevron: ImageView
    private lateinit var tvReceiptTxnIdLabel: TextView
    private lateinit var tvReceiptTxnId: TextView
    private lateinit var tvReceiptDebitedFromLabel: TextView
    private lateinit var tvReceiptDebitedAccount: TextView
    private lateinit var tvReceiptUtr: TextView
    private lateinit var tvReceiptDebitedAmount: TextView
    private lateinit var tvReceiptPoweredBy: TextView
    private lateinit var divider1: View
    private lateinit var divider2: View

    private lateinit var btnToggleTheme: MaterialButton
    private lateinit var btnPickDateTime: MaterialButton
    private lateinit var etAmount: EditText
    private lateinit var etTxnId: EditText
    private lateinit var etUtr: EditText

    private lateinit var btnShareWhatsApp: MaterialButton

    private var isDarkMode = true
    private val selectedCalendar = Calendar.getInstance()
    private val dateTimeFormat = SimpleDateFormat("hh:mm a 'on' dd MMM yyyy", Locale.ENGLISH)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_simple_dashboard)
        setupAdminLayout("Payment Summary")

        findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh).isEnabled = false

        val content = findViewById<LinearLayout>(R.id.contentContainer)
        content.setPadding(0, 0, 0, 0)
        content.removeAllViews()

        val view = LayoutInflater.from(this).inflate(R.layout.view_phone_pe_receipt_content, content, false)
        content.addView(view)

        // Bind Views
        receiptContainer = view.findViewById(R.id.receiptContainer)
        cardReceiptMain = view.findViewById(R.id.cardReceiptMain)
        tvReceiptStatus = view.findViewById(R.id.tvReceiptStatus)
        tvReceiptDateTime = view.findViewById(R.id.tvReceiptDateTime)
        tvReceiptPaidToLabel = view.findViewById(R.id.tvReceiptPaidToLabel)
        tvReceiptRecipientName = view.findViewById(R.id.tvReceiptRecipientName)
        tvReceiptRecipientAccount = view.findViewById(R.id.tvReceiptRecipientAccount)
        tvReceiptRecipientBank = view.findViewById(R.id.tvReceiptRecipientBank)
        tvReceiptAmount = view.findViewById(R.id.tvReceiptAmount)
        tvReceiptBankingNameLabel = view.findViewById(R.id.tvReceiptBankingNameLabel)
        tvReceiptBankingNameColon = view.findViewById(R.id.tvReceiptBankingNameColon)
        tvReceiptBankingName = view.findViewById(R.id.tvReceiptBankingName)
        tvReceiptTransferDetailsLabel = view.findViewById(R.id.tvReceiptTransferDetailsLabel)
        ivExpandChevron = view.findViewById(R.id.ivExpandChevron)
        tvReceiptTxnIdLabel = view.findViewById(R.id.tvReceiptTxnIdLabel)
        tvReceiptTxnId = view.findViewById(R.id.tvReceiptTxnId)
        tvReceiptDebitedFromLabel = view.findViewById(R.id.tvReceiptDebitedFromLabel)
        tvReceiptDebitedAccount = view.findViewById(R.id.tvReceiptDebitedAccount)
        tvReceiptUtr = view.findViewById(R.id.tvReceiptUtr)
        tvReceiptDebitedAmount = view.findViewById(R.id.tvReceiptDebitedAmount)
        tvReceiptPoweredBy = view.findViewById(R.id.tvReceiptPoweredBy)
        divider1 = view.findViewById(R.id.divider1)
        divider2 = view.findViewById(R.id.divider2)

        btnToggleTheme = view.findViewById(R.id.btnToggleTheme)
        btnPickDateTime = view.findViewById(R.id.btnPickDateTime)
        etAmount = view.findViewById(R.id.etAmount)
        etTxnId = view.findViewById(R.id.etTxnId)
        etUtr = view.findViewById(R.id.etUtr)

        btnShareWhatsApp = view.findViewById(R.id.btnShareWhatsApp)

        // Set Static Values
        tvReceiptRecipientName.text = PaymentConfig.PAYEE_NAME
        tvReceiptRecipientAccount.text = "XXXXXXXX7295"
        tvReceiptRecipientBank.text = "ICICI Bank"
        tvReceiptBankingName.text = "Bank Account Xxxxxxxx7295"
        tvReceiptDebitedAccount.text = "XXXX0047"

        if (savedInstanceState != null) {
            isDarkMode = savedInstanceState.getBoolean("is_dark_mode", true)
            selectedCalendar.timeInMillis = savedInstanceState.getLong("summary_date", selectedCalendar.timeInMillis)
            etAmount.setText(savedInstanceState.getString("amount", "7485.4"))
            etTxnId.setText(savedInstanceState.getString("transaction", "T2611011915246257837177"))
            etUtr.setText(savedInstanceState.getString("utr", "347690216802"))
        } else {
            val passedAmount = intent.getStringExtra(EXTRA_AMOUNT) ?: "7485.4"
            val passedTxnId = intent.getStringExtra(EXTRA_TXN_ID) ?: "T2611011915246257837177"
            val passedUtr = intent.getStringExtra(EXTRA_UTR) ?: "347690216802"

            etAmount.setText(passedAmount)
            etTxnId.setText(passedTxnId)
            etUtr.setText(passedUtr)
        }

        applyReceiptTheme()
        updateDateTimeLabel()
        updateReceiptPreview()

        // Listeners
        btnToggleTheme.setOnClickListener {
            isDarkMode = !isDarkMode
            applyReceiptTheme()
        }

        btnPickDateTime.setOnClickListener { showDateTimePicker() }

        listOf(etAmount, etTxnId, etUtr).forEach { field ->
            field.doAfterTextChanged { updateReceiptPreview() }
        }

        btnShareWhatsApp.setOnClickListener { shareToWhatsApp() }
    }

    private fun applyReceiptTheme() {
        if (isDarkMode) {
            btnToggleTheme.text = "Dark Mode"
            receiptContainer.setBackgroundColor(Color.parseColor("#0D1010"))
            cardReceiptMain.setCardBackgroundColor(Color.parseColor("#1C1E20"))

            val white = Color.parseColor("#FFFFFF")
            val muted = Color.parseColor("#9EA5A5")
            val divider = Color.parseColor("#2A2F30")

            tvReceiptStatus.setTextColor(white)
            tvReceiptDateTime.setTextColor(muted)
            tvReceiptPaidToLabel.setTextColor(white)
            tvReceiptRecipientName.setTextColor(white)
            tvReceiptRecipientAccount.setTextColor(muted)
            tvReceiptRecipientBank.setTextColor(muted)
            tvReceiptAmount.setTextColor(white)
            tvReceiptBankingNameLabel.setTextColor(muted)
            tvReceiptBankingNameColon.setTextColor(muted)
            tvReceiptBankingName.setTextColor(Color.parseColor("#E0E0E0"))
            tvReceiptTransferDetailsLabel.setTextColor(white)
            ivExpandChevron.setColorFilter(white)
            tvReceiptTxnIdLabel.setTextColor(muted)
            tvReceiptTxnId.setTextColor(white)
            tvReceiptDebitedFromLabel.setTextColor(muted)
            tvReceiptDebitedAccount.setTextColor(white)
            tvReceiptUtr.setTextColor(muted)
            tvReceiptDebitedAmount.setTextColor(white)
            tvReceiptPoweredBy.setTextColor(Color.parseColor("#888888"))

            divider1.setBackgroundColor(divider)
            divider2.setBackgroundColor(divider)
        } else {
            btnToggleTheme.text = "Light Mode"
            receiptContainer.setBackgroundColor(Color.parseColor("#F4F4F6"))
            cardReceiptMain.setCardBackgroundColor(Color.parseColor("#FFFFFF"))

            val dark = Color.parseColor("#1F1F1F")
            val muted = Color.parseColor("#666666")
            val divider = Color.parseColor("#EBEBEB")

            tvReceiptStatus.setTextColor(dark)
            tvReceiptDateTime.setTextColor(Color.parseColor("#555555"))
            tvReceiptPaidToLabel.setTextColor(dark)
            tvReceiptRecipientName.setTextColor(dark)
            tvReceiptRecipientAccount.setTextColor(muted)
            tvReceiptRecipientBank.setTextColor(muted)
            tvReceiptAmount.setTextColor(dark)
            tvReceiptBankingNameLabel.setTextColor(muted)
            tvReceiptBankingNameColon.setTextColor(muted)
            tvReceiptBankingName.setTextColor(Color.parseColor("#333333"))
            tvReceiptTransferDetailsLabel.setTextColor(dark)
            ivExpandChevron.setColorFilter(Color.parseColor("#333333"))
            tvReceiptTxnIdLabel.setTextColor(Color.parseColor("#777777"))
            tvReceiptTxnId.setTextColor(Color.parseColor("#222222"))
            tvReceiptDebitedFromLabel.setTextColor(Color.parseColor("#777777"))
            tvReceiptDebitedAccount.setTextColor(Color.parseColor("#222222"))
            tvReceiptUtr.setTextColor(muted)
            tvReceiptDebitedAmount.setTextColor(Color.parseColor("#222222"))
            tvReceiptPoweredBy.setTextColor(Color.parseColor("#888888"))

            divider1.setBackgroundColor(divider)
            divider2.setBackgroundColor(divider)
        }
    }

    private fun showDateTimePicker() {
        hideKeyboard()
        SharedDatePickerDialog(this, { _, year, month, day ->
            selectedCalendar.set(Calendar.YEAR, year)
            selectedCalendar.set(Calendar.MONTH, month)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, day)

            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedCalendar.set(Calendar.HOUR_OF_DAY, hour)
                    selectedCalendar.set(Calendar.MINUTE, minute)
                    updateDateTimeLabel()
                },
                selectedCalendar.get(Calendar.HOUR_OF_DAY),
                selectedCalendar.get(Calendar.MINUTE),
                false
            ).show()
        }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateTimeLabel() {
        val formatted = dateTimeFormat.format(selectedCalendar.time)
        btnPickDateTime.text = formatted
        tvReceiptDateTime.text = formatted
    }

    private fun updateReceiptPreview() {
        val rawAmount = etAmount.text.toString().trim()
        val amountVal = rawAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val formattedAmount = formatCurrency(rawAmount, amountVal)

        tvReceiptAmount.text = formattedAmount
        tvReceiptDebitedAmount.text = formattedAmount

        tvReceiptTxnId.text = etTxnId.text.toString().ifBlank { "T2611011915246257837177" }

        val utrVal = etUtr.text.toString().trim()
        tvReceiptUtr.text = if (utrVal.isNotBlank()) "UTR: $utrVal" else "UTR: 347690216802"
    }

    private fun formatCurrency(raw: String, value: BigDecimal): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            var formatted = formatter.format(value)
            if (raw.contains(".")) {
                val decimals = raw.substringAfter(".")
                if (decimals.length == 1) {
                    val integerPart = NumberFormat.getNumberInstance(Locale("en", "IN")).format(value.toLong())
                    return "₹$integerPart.$decimals"
                }
            }
            if (formatted.endsWith(".00")) {
                formatted = formatted.substring(0, formatted.length - 3)
            }
            formatted
        } catch (e: Exception) {
            "₹$value"
        }
    }

    private fun renderReceiptBitmap(): Bitmap {
        receiptContainer.clearFocus()
        val width = receiptContainer.width.coerceAtLeast(720)
        val height = receiptContainer.height.coerceAtLeast(960)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        receiptContainer.draw(canvas)
        return bitmap
    }

    private fun shareToWhatsApp() {
        hideKeyboard()
        btnShareWhatsApp.isEnabled = false

        val bitmap = renderReceiptBitmap()

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val cacheFolder = File(cacheDir, "shared_images").apply { mkdirs() }
                    val cacheFile = File(cacheFolder, "phonepe_receipt_${System.currentTimeMillis()}.png")
                    cacheFile.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    val contentUri = FileProvider.getUriForFile(this@AdminPaymentSummaryActivity, "$packageName.provider", cacheFile)

                    val targetNumber = "919951696363"
                    val shareText = "Paid using PhonePe UPI.\nExplore the app now: https://phon.pe/download2025"

                    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        putExtra("jid", "$targetNumber@s.whatsapp.net")
                        setPackage("com.whatsapp")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    try {
                        startActivity(whatsappIntent)
                    } catch (e: Exception) {
                        val webUri = Uri.parse("https://wa.me/$targetNumber?text=" + Uri.encode(shareText))
                        val fallbackIntent = Intent(Intent.ACTION_VIEW, webUri)
                        startActivity(fallbackIntent)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminPaymentSummaryActivity, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                bitmap.recycle()
                btnShareWhatsApp.isEnabled = true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("is_dark_mode", isDarkMode)
        outState.putLong("summary_date", selectedCalendar.timeInMillis)
        outState.putString("amount", etAmount.text.toString())
        outState.putString("transaction", etTxnId.text.toString())
        outState.putString("utr", etUtr.text.toString())
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_TXN_ID = "extra_txn_id"
        const val EXTRA_UTR = "extra_utr"
    }
}

package com.svd.svdagencies.ui.admin.companies

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Company
import com.svd.svdagencies.databinding.AdminCompanyAddEditBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import com.svd.svdagencies.utils.NetworkMessageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class AddEditCompanyActivity : AdminBaseActivity() {

    private lateinit var binding: AdminCompanyAddEditBinding
    private var companyToUpdate: Company? = null
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                Glide.with(this).load(uri).into(binding.ivLogo)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminCompanyAddEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        companyToUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("COMPANY_TO_UPDATE", Company::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("COMPANY_TO_UPDATE")
        }

        if (companyToUpdate != null) {
            setupAdminLayout("Edit Company")
            binding.tvSubtitle.text = "Update company details"
            binding.btnSave.text = "Update Company"
            populateFields(companyToUpdate!!)
        } else {
            setupAdminLayout("Add Company")
            binding.tvSubtitle.text = "Fill the form to save company details"
            binding.btnSave.text = "Save Company"
        }

        setupListeners()
    }

    private fun populateFields(company: Company) {
        binding.etCompanyName.setText(company.name)
        binding.etWebsite.setText(company.websiteLink)

        if (!company.logo.isNullOrEmpty()) {
            val base = ApiClient.BASE_URL.removeSuffix("/")
            val fullUrl = if (company.logo.startsWith("http")) {
                company.logo
            } else {
                "$base/${company.logo.removePrefix("/")}"
            }
            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_milk_placeholder)
                .into(binding.ivLogo)
        }
    }

    private fun setupListeners() {
        binding.btnSelectLogo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener { saveCompany() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun saveCompany() {
        val name = binding.etCompanyName.text.toString().trim()
        val website = binding.etWebsite.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Company name is required", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSave.isEnabled = false
        showScreenLoading()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val websitePart = website.toRequestBody("text/plain".toMediaTypeOrNull())

                var logoPart: MultipartBody.Part? = null
                selectedImageUri?.let { uri ->
                    getFileFromUri(uri)?.let { file ->
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        logoPart = MultipartBody.Part.createFormData("logo", file.name, requestFile)
                    }
                }

                if (companyToUpdate == null) {
                    ApiClient.adminCompaniesApi.addCompany(namePart, websitePart, logoPart)
                } else {
                    ApiClient.adminCompaniesApi.editCompany(companyToUpdate!!.id, namePart, websitePart, logoPart)
                }

                withContext(Dispatchers.Main) {
                    hideScreenLoading()
                    Toast.makeText(this@AddEditCompanyActivity, "Saved successfully", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                    hideScreenLoading()
                    Toast.makeText(
                        this@AddEditCompanyActivity,
                        NetworkMessageUtils.friendlyMessage(e, "Failed to save company"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "temp_logo.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}

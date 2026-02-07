package com.svd.svdagencies.ui.admin.items

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Company
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.databinding.AdminItemAddBinding
import com.svd.svdagencies.ui.admin.AdminBaseActivity
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

class AddEditItemActivity : AdminBaseActivity() {

    private lateinit var binding: AdminItemAddBinding
    private var itemToUpdate: AdminItem? = null
    private var selectedImageUri: Uri? = null
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private lateinit var companyAdapter: ArrayAdapter<String>
    private var companies: List<Company> = emptyList()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                Glide.with(this).load(uri).into(binding.imgItemPreview)
                binding.btnRemoveImage.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AdminItemAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve item data
        itemToUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("ITEM_TO_UPDATE", AdminItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("ITEM_TO_UPDATE")
        }

        setupSpinners()

        if (itemToUpdate != null) {
            setupAdminLayout("Edit Item")
            binding.btnAddItem.text = "Update Item"
        } else {
            setupAdminLayout("Add Item")
            binding.btnAddItem.text = "Add Item"
        }

        loadCompanies()
        setupListeners()
    }

    private fun setupSpinners() {
        val categories = listOf("Milk", "Curd", "Butter Milk", "Paneer", "Other")
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
    }

    private fun loadCompanies() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.adminCompaniesApi.getCompanies()
                companies = response.companies
                val companyNames = companies.map { it.name }

                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        companyAdapter = ArrayAdapter(this@AddEditItemActivity, android.R.layout.simple_spinner_item, companyNames)
                        companyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.spinnerCompany.adapter = companyAdapter

                        // If editing, set the correct company selection
                        itemToUpdate?.let { populateFields(it) }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        Toast.makeText(this@AddEditItemActivity, "Failed to load companies", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun populateFields(item: AdminItem) {
        binding.etItemName.setText(item.name)
        binding.etItemCode.setText(item.code)
        binding.etBuyingPrice.setText(item.buying_price)
        binding.etSellingPrice.setText(item.selling_price)
        binding.etMrp.setText(item.mrp)
        binding.etStock.setText(item.stock_quantity?.toString())
        binding.etPcs.setText(item.pcs_count?.toString())

        // Set Category Selection
        val catPos = categoryAdapter.getPosition(item.category)
        if (catPos >= 0) binding.spinnerCategory.setSelection(catPos)

        // Set Company Selection
        val companyIndex = companies.indexOfFirst { it.name == item.company }
        if (companyIndex >= 0) binding.spinnerCompany.setSelection(companyIndex)

        // Load Image
        if (!item.image.isNullOrEmpty()) {
            val base = ApiClient.BASE_URL.removeSuffix("/")
            val fullUrl = if (item.image.startsWith("http")) item.image else "$base/${item.image.removePrefix("/")}"
            Glide.with(this).load(fullUrl).into(binding.imgItemPreview)
            binding.btnRemoveImage.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            binding.imgItemPreview.setImageResource(R.drawable.ic_milk_placeholder)
            binding.btnRemoveImage.visibility = View.GONE
        }

        binding.btnAddItem.setOnClickListener { saveItem() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun saveItem() {
        val name = binding.etItemName.text.toString().trim()
        val code = binding.etItemCode.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem?.toString() ?: ""
        val buyingPrice = binding.etBuyingPrice.text.toString().trim()
        val sellingPrice = binding.etSellingPrice.text.toString().trim()
        val mrp = binding.etMrp.text.toString().trim()
        val stock = binding.etStock.text.toString().trim()
        val pcs = binding.etPcs.text.toString().trim()

        // Get selected company ID
        val companyIndex = binding.spinnerCompany.selectedItemPosition
        val companyId = if (companyIndex >= 0) companies[companyIndex].id.toString() else ""

        if (name.isEmpty() || companyId.isEmpty()) {
            Toast.makeText(this, "Name and Company are required", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAddItem.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val codePart = code.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryPart = category.toRequestBody("text/plain".toMediaTypeOrNull())
                val buyingPart = buyingPrice.toRequestBody("text/plain".toMediaTypeOrNull())
                val sellingPart = sellingPrice.toRequestBody("text/plain".toMediaTypeOrNull())
                val mrpPart = mrp.toRequestBody("text/plain".toMediaTypeOrNull())
                val stockPart = stock.toRequestBody("text/plain".toMediaTypeOrNull())
                val pcsPart = pcs.toRequestBody("text/plain".toMediaTypeOrNull())
                val companyPart = companyId.toRequestBody("text/plain".toMediaTypeOrNull())

                var imagePart: MultipartBody.Part? = null
                selectedImageUri?.let { uri ->
                    getFileFromUri(uri)?.let { file ->
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                }

                if (itemToUpdate == null) {
                    ApiClient.adminItemsApi.addItem(codePart, namePart, companyPart, categoryPart, sellingPart, buyingPart, mrpPart, stockPart, pcsPart, imagePart)
                } else {
                    ApiClient.adminItemsApi.editItem(itemToUpdate!!.id, codePart, namePart, companyPart, categoryPart, sellingPart, buyingPart, mrpPart, stockPart, pcsPart, imagePart)
                }

                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        Toast.makeText(this@AddEditItemActivity, "Saved successfully", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.btnAddItem.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@AddEditItemActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, "upload_image.jpg")
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

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
import com.svd.svdagencies.data.model.admin.AdminItem
import com.svd.svdagencies.databinding.ActivityAddEditItemBinding
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

    private lateinit var binding: ActivityAddEditItemBinding
    private var itemToUpdate: AdminItem? = null
    private var selectedImageUri: Uri? = null

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
        binding = ActivityAddEditItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve item data
        itemToUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("ITEM_TO_UPDATE", AdminItem::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("ITEM_TO_UPDATE")
        }

        if (itemToUpdate != null) {
            setupAdminLayout("Edit Item")
            binding.btnAddItem.text = "Update Item"
            populateFields(itemToUpdate!!)
        } else {
            setupAdminLayout("Add Item")
            binding.btnAddItem.text = "Add Item"
        }

        setupSpinners()
        setupListeners()
    }

    private fun setupSpinners() {
        // Hardcoded categories for now, ideally fetch from API
        // Or if AdminItemsActivity fetched them, pass them via intent
        val categories = listOf("Milk", "Curd", "Butter Milk", "Paneer", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun populateFields(item: AdminItem) {
        binding.etItemName.setText(item.name)
        binding.etItemCode.setText(item.code)
        // binding.etCompany.setText(item.company) // Spinner setup needed if dynamic
        binding.etBuyingPrice.setText(item.buying_price)
        binding.etSellingPrice.setText(item.selling_price)
        binding.etMrp.setText(item.mrp)
        binding.etStock.setText(item.stock_quantity?.toString())
        binding.etPcs.setText(item.pcs_count?.toString())

        // Set Category Spinner
        val adapter = binding.spinnerCategory.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(item.category)
        if (position >= 0) binding.spinnerCategory.setSelection(position)

        // Load Image
        if (!item.image.isNullOrEmpty()) {
            val fullUrl = if (item.image.startsWith("http")) item.image else "http://ec2-18-235-222-205.compute-1.amazonaws.com${item.image}"
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
            binding.imgItemPreview.setImageResource(R.drawable.ic_milk_placeholder) // Use placeholder
            binding.btnRemoveImage.visibility = View.GONE
        }

        binding.btnAddItem.setOnClickListener {
            saveItem()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveItem() {
        val name = binding.etItemName.text.toString().trim()
        val code = binding.etItemCode.text.toString().trim()
        val category = binding.spinnerCategory.selectedItem.toString()
        val buyingPrice = binding.etBuyingPrice.text.toString().trim()
        val sellingPrice = binding.etSellingPrice.text.toString().trim()
        val mrp = binding.etMrp.text.toString().trim()
        val stock = binding.etStock.text.toString().trim()
        val pcs = binding.etPcs.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAddItem.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Prepare RequestBody parts
                val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val codePart = code.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryPart = category.toRequestBody("text/plain".toMediaTypeOrNull())
                val buyingPart = buyingPrice.toRequestBody("text/plain".toMediaTypeOrNull())
                val sellingPart = sellingPrice.toRequestBody("text/plain".toMediaTypeOrNull())
                val mrpPart = mrp.toRequestBody("text/plain".toMediaTypeOrNull())
                val stockPart = stock.toRequestBody("text/plain".toMediaTypeOrNull())
                val pcsPart = pcs.toRequestBody("text/plain".toMediaTypeOrNull())
                // Company ID hardcoded or retrieved
                val companyPart = "1".toRequestBody("text/plain".toMediaTypeOrNull()) // Default or selected

                // Prepare Image Part
                var imagePart: MultipartBody.Part? = null
                selectedImageUri?.let { uri ->
                    val file = getFileFromUri(uri)
                    if (file != null) {
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                }

                val response = if (itemToUpdate == null) {
                    ApiClient.adminItemsApi.addItem(
                        code = codePart, name = namePart, companyId = companyPart, category = categoryPart,
                        sellingPrice = sellingPart, buyingPrice = buyingPart, mrp = mrpPart,
                        stockQuantity = stockPart, pcsCount = pcsPart, image = imagePart
                    )
                } else {
                    ApiClient.adminItemsApi.editItem(
                        id = itemToUpdate!!.id,
                        code = codePart, name = namePart, companyId = companyPart, category = categoryPart,
                        sellingPrice = sellingPart, buyingPrice = buyingPart, mrp = mrpPart,
                        stockQuantity = stockPart, pcsCount = pcsPart, image = imagePart
                    )
                }

                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        binding.btnAddItem.isEnabled = true
                        binding.progressBar.visibility = View.GONE
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
            e.printStackTrace()
            null
        }
    }
}

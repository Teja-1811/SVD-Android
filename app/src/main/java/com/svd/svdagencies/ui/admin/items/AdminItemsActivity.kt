package com.svd.svdagencies.ui.admin.items

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.svd.svdagencies.R
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.ui.admin.adapter.AdminItemAdapter
import com.svd.svdagencies.ui.admin.AdminBaseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminItemsActivity : AdminBaseActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvItems: RecyclerView
    private lateinit var rvCompanyFilter: RecyclerView
    private lateinit var rvCategoryFilter: RecyclerView
    private lateinit var etSearchItems: EditText
    private lateinit var btnAddItem: FloatingActionButton
    private lateinit var btnResetFilter: MaterialCardView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var itemAdapter: AdminItemAdapter
    private lateinit var companyAdapter: CompanyFilterAdapter
    private lateinit var categoryAdapter: CategoryFilterAdapter

    private var allItems: List<AdminItem> = emptyList()
    private var currentCategory: String = "Milk"
    private var selectedCompany: String? = null

    private val categoryOrder = listOf("milk", "curd", "buckets", "cups", "ghee", "flavoured milk", "panner", "sweets", "others")

    private val addEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadItems(currentCategory)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.admin_items_dashboard)

        setupAdminLayout("Items")

        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvItems = findViewById(R.id.rvItems)
        rvCompanyFilter = findViewById(R.id.rvCompanyFilter)
        rvCategoryFilter = findViewById(R.id.rvCategoryFilter)
        etSearchItems = findViewById(R.id.etSearchItems)
        btnAddItem = findViewById(R.id.btnAddItem)
        btnResetFilter = findViewById(R.id.btnResetFilter)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)

        setupRecyclers()
        setupListeners()
        loadInitialData()
    }

    private fun setupRecyclers() {
        itemAdapter = AdminItemAdapter(
            emptyList(),
            onEditClick = { item -> 
                val intent = Intent(this, AddEditItemActivity::class.java)
                intent.putExtra("ITEM_TO_UPDATE", item)
                addEditLauncher.launch(intent)
            },
            onFreezeClick = { item -> toggleItemFreeze(item) }
        )
        rvItems.layoutManager = LinearLayoutManager(this)
        rvItems.adapter = itemAdapter

        companyAdapter = CompanyFilterAdapter { company ->
            selectedCompany = company
            applyFilters()
        }
        rvCompanyFilter.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCompanyFilter.adapter = companyAdapter

        categoryAdapter = CategoryFilterAdapter { category ->
            currentCategory = category
            tvCategoryTitle.text = category
            loadItems(category)
        }
        rvCategoryFilter.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCategoryFilter.adapter = categoryAdapter
    }

    private fun setupListeners() {
        btnAddItem.setOnClickListener {
            addEditLauncher.launch(Intent(this, AddEditItemActivity::class.java))
        }

        btnResetFilter.setOnClickListener {
            selectedCompany = null
            companyAdapter.clearSelection()
            etSearchItems.text.clear()
            applyFilters()
        }

        swipeRefresh.setOnRefreshListener {
            loadItems(currentCategory)
        }

        etSearchItems.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadInitialData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch all companies globally
                val companiesResponse = ApiClient.adminCompaniesApi.getCompanies()
                val companyDataList = companiesResponse.companies.map { CompanyData(it.name, it.logo) }
                
                // Fetch and sort categories
                val categoriesResponse = ApiClient.adminItemsApi.getCategories()
                val sortedCategories = sortCategories(categoriesResponse.categories)

                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        companyAdapter.submitList(companyDataList)
                        categoryAdapter.submitList(sortedCategories)
                        
                        val milkIndex = sortedCategories.indexOfFirst { it.equals("Milk", ignoreCase = true) }
                        if (milkIndex != -1) {
                            categoryAdapter.setSelection(milkIndex)
                            currentCategory = sortedCategories[milkIndex]
                            tvCategoryTitle.text = currentCategory
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        showToast("Failed to load filters")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadItems(currentCategory)
    }

    private fun sortCategories(categories: List<String>): List<String> {
        return categories.sortedBy { cat ->
            val index = categoryOrder.indexOf(cat.lowercase())
            if (index != -1) index else Int.MAX_VALUE
        }
    }

    private fun loadItems(category: String) {
        swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.adminItemsApi.getItemsByCategory(category)
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        swipeRefresh.isRefreshing = false
                        allItems = response.items
                        applyFilters()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        swipeRefresh.isRefreshing = false
                        showToast("Error loading items")
                    }
                }
            }
        }
    }

    private fun toggleItemFreeze(item: AdminItem) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.adminItemsApi.toggleFreezeItem(item.id)
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        showToast("Item status updated")
                        loadItems(currentCategory)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        showToast("Error updating status: ${e.message}")
                    }
                }
            }
        }
    }

    private fun applyFilters() {
        val searchQuery = etSearchItems.text.toString().trim()
        var filteredList = allItems

        if (selectedCompany != null) {
            filteredList = filteredList.filter { it.company == selectedCompany }
        }

        if (searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.code?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        itemAdapter.updateList(filteredList)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    data class CompanyData(val name: String, val logo: String?)

    inner class CompanyFilterAdapter(private val onCompanySelected: (String) -> Unit) : 
        RecyclerView.Adapter<CompanyFilterAdapter.ViewHolder>() {

        private var companies: List<CompanyData> = emptyList()
        private var selectedPosition: Int = -1

        fun submitList(list: List<CompanyData>) {
            companies = list
            notifyDataSetChanged()
        }

        fun clearSelection() {
            selectedPosition = -1
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_company_filter_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val company = companies[position]
            holder.bind(company, position == selectedPosition)
            
            holder.itemView.setOnClickListener {
                if (selectedPosition == holder.adapterPosition) {
                    selectedPosition = -1
                    onCompanySelected("") // Deselect
                } else {
                    val prev = selectedPosition
                    selectedPosition = holder.adapterPosition
                    notifyItemChanged(prev)
                    onCompanySelected(company.name)
                }
                notifyItemChanged(selectedPosition)
            }
        }

        override fun getItemCount() = companies.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imgLogo: ImageView = itemView.findViewById(R.id.imgCompanyLogo)
            private val card: MaterialCardView = itemView.findViewById(R.id.cardCompany)

            fun bind(company: CompanyData, isSelected: Boolean) {
                // For company logos, we use the logo helper
                val fullUrl = ApiClient.getLogoUrl(company.logo)
                
                Glide.with(itemView.context)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_milk_placeholder)
                    .into(imgLogo)

                card.strokeColor = if (isSelected) 0xFFD32F2F.toInt() else 0xFFE0E0E0.toInt()
                card.strokeWidth = if (isSelected) 6 else 2
            }
        }
    }

    inner class CategoryFilterAdapter(private val onCategorySelected: (String) -> Unit) : 
        RecyclerView.Adapter<CategoryFilterAdapter.ViewHolder>() {

        private var categories: List<String> = emptyList()
        private var selectedPosition: Int = -1

        fun submitList(list: List<String>) {
            categories = list
            notifyDataSetChanged()
        }

        fun setSelection(position: Int) {
            selectedPosition = position
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_category_filter_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            holder.bind(category, position == selectedPosition)
            
            holder.itemView.setOnClickListener {
                if (selectedPosition != holder.adapterPosition) {
                    val prev = selectedPosition
                    selectedPosition = holder.adapterPosition
                    notifyItemChanged(prev)
                    notifyItemChanged(selectedPosition)
                    onCategorySelected(category)
                }
            }
        }

        override fun getItemCount() = categories.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
            private val card: MaterialCardView = itemView.findViewById(R.id.cardCategory)

            fun bind(category: String, isSelected: Boolean) {
                tvName.text = category
                if (isSelected) {
                    card.setCardBackgroundColor(0xFFD32F2F.toInt())
                    tvName.setTextColor(0xFFFFFFFF.toInt())
                    card.strokeWidth = 0
                } else {
                    card.setCardBackgroundColor(0xFFFFFFFF.toInt())
                    tvName.setTextColor(0xFF000000.toInt())
                    card.strokeWidth = 2
                }
            }
        }
    }
}

package com.svd.svdagencies.ui.customer.viewmodel

import androidx.lifecycle.ViewModel
import com.svd.svdagencies.data.model.customer.ProductResponse

class PlaceOrderViewModel : ViewModel() {
    val cartQuantities = HashMap<Int, Double>()
    var currentCategoryProducts = mutableListOf<ProductResponse>()
    val allProducts = mutableMapOf<Int, ProductResponse>()
}

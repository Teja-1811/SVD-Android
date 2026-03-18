package com.svd.svdagencies.ui.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.ui.user.model.CartItem
import kotlin.math.min

class UserCartViewModel : ViewModel() {

    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _totalAmount = MediatorLiveData<Double>().apply { value = 0.0 }
    val totalAmount: LiveData<Double> = _totalAmount

    private val _itemCount = MediatorLiveData<Int>().apply { value = 0 }
    val itemCount: LiveData<Int> = _itemCount

    private var allowOutOfStock: Boolean = false

    init {
        _totalAmount.addSource(_cartItems) { list ->
            _totalAmount.value = list.sumByDouble { cartItem -> cartItem.quantity * cartItem.unitPrice() }
        }
        _itemCount.addSource(_cartItems) { list ->
            _itemCount.value = list.sumBy { it.quantity }
        }
    }

    fun setAllowOutOfStock(allow: Boolean) {
        allowOutOfStock = allow
    }

    fun getQuantity(itemId: Int): Int {
        return _cartItems.value?.firstOrNull { it.item.id == itemId }?.quantity ?: 0
    }

    fun addToCart(item: AdminItem) {
        val available = item.stock_quantity ?: 0
        if (!allowOutOfStock && available <= 1) return

        val current = _cartItems.value?.toMutableList() ?: mutableListOf()
        val index = current.indexOfFirst { it.item.id == item.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = if (allowOutOfStock) existing.quantity + 1 else min(existing.quantity + 1, available)
            current[index] = existing.copy(quantity = newQty)
        } else {
            val initialQty = if (allowOutOfStock) 1 else min(1, maxOf(available, 1))
            current.add(CartItem(item, initialQty))
        }
        _cartItems.value = current
    }

    fun updateQuantity(itemId: Int, quantity: Int) {
        val current = _cartItems.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index == -1) return

        if (quantity <= 0) {
            current.removeAt(index)
        } else {
            val available = current[index].item.stock_quantity ?: Int.MAX_VALUE
            val capped = if (allowOutOfStock) quantity else min(quantity, available)
            current[index] = current[index].copy(quantity = capped)
        }
        _cartItems.value = current
    }

    fun removeItem(itemId: Int) {
        val current = _cartItems.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index != -1) {
            current.removeAt(index)
            _cartItems.value = current
        }
    }

    private fun CartItem.unitPrice(): Double {
        val selling = item.sellingPriceValue
        val mrp = item.mrpValue
        return if (selling > 0) selling else mrp
    }
}

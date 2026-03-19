package com.svd.svdagencies.ui.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.svd.svdagencies.data.api.auth.ApiClient
import com.svd.svdagencies.data.model.admin.Items.AdminItem
import com.svd.svdagencies.ui.user.model.CartItem
import com.svd.svdagencies.data.model.user.PendingOrder
import com.svd.svdagencies.data.model.user.PendingOrderItem
import com.svd.svdagencies.data.model.user.CreateOrderRequest
import com.svd.svdagencies.data.model.user.OrderItemPayload
import kotlinx.coroutines.launch
import kotlin.math.min

class UserCartViewModel : ViewModel() {

    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _totalAmount = MediatorLiveData<Double>().apply { value = 0.0 }
    val totalAmount: LiveData<Double> = _totalAmount

    private val _itemCount = MediatorLiveData<Int>().apply { value = 0 }
    val itemCount: LiveData<Int> = _itemCount

    private val _pendingOrders = MutableLiveData<List<PendingOrder>>(emptyList())
    val pendingOrders: LiveData<List<PendingOrder>> = _pendingOrders

    private val _pendingLoading = MutableLiveData(false)
    val pendingLoading: LiveData<Boolean> = _pendingLoading

    private val _pendingError = MutableLiveData<String?>(null)
    val pendingError: LiveData<String?> = _pendingError

    private val _orderActionMessage = MutableLiveData<String?>()
    val orderActionMessage: LiveData<String?> = _orderActionMessage

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

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun loadPendingOrders() {
        viewModelScope.launch {
            _pendingLoading.value = true
            try {
                val response = ApiClient.userApi.getPendingOrders()
                _pendingOrders.value = response.orders
                _pendingError.value = null
            } catch (t: Throwable) {
                _pendingError.value = t.localizedMessage ?: "Unable to load pending orders"
            } finally {
                _pendingLoading.value = false
            }
        }
    }

    fun placeOrder(
        items: List<CartItem>,
        deliveryDate: String?,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        if (items.isEmpty()) {
            onResult(false, "Your cart is empty")
            return
        }
        val payload = CreateOrderRequest(
            items = items.map { OrderItemPayload(it.item.id, it.quantity, it.unitPrice()) },
            delivery_date = deliveryDate
        )

        viewModelScope.launch {
            try {
                val response = ApiClient.userApi.createOrder(payload)
                val success = response.success
                val message = response.message ?: if (success) "Order saved successfully" else "Order failed"
                if (success) {
                    clearCart()
                    loadPendingOrders()
                }
                _orderActionMessage.value = message
                onResult(success, message)
            } catch (t: Throwable) {
                val msg = t.localizedMessage ?: "Unable to place order"
                _orderActionMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun updatePendingOrder(
        orderId: Int,
        items: List<PendingOrderItem>,
        deliveryDate: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        if (items.isEmpty()) {
            onResult(false, "Add at least one item")
            return
        }

        val payload = CreateOrderRequest(
            items = items.map { OrderItemPayload(it.item_id, it.quantity, it.price) },
            delivery_date = deliveryDate
        )

        viewModelScope.launch {
            try {
                val response = ApiClient.userApi.editOrder(orderId, payload)
                val success = response.success
                val message = response.message ?: if (success) "Order updated" else "Update failed"
                if (success) loadPendingOrders()
                _orderActionMessage.value = message
                onResult(success, message)
            } catch (t: Throwable) {
                val msg = t.localizedMessage ?: "Unable to update order"
                _orderActionMessage.value = msg
                onResult(false, msg)
            }
        }
    }

    fun deletePendingOrder(
        orderId: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = ApiClient.userApi.deleteOrder(orderId)
                val success = (response["success"] as? Boolean) ?: false
                val message = (response["message"] as? String)
                    ?: if (success) "Order deleted" else "Deletion failed"
                if (success) loadPendingOrders()
                _orderActionMessage.value = message
                onResult(success, message)
            } catch (t: Throwable) {
                val msg = t.localizedMessage ?: "Unable to delete order"
                _orderActionMessage.value = msg
                onResult(false, msg)
            }
        }
    }
}

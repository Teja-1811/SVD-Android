package com.svd.svdagencies.ui.user

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.svd.svdagencies.R
import com.svd.svdagencies.data.model.user.UserCustomer
import com.svd.svdagencies.data.model.user.UserDashboardResponse
import com.svd.svdagencies.data.repository.UserDashboardObserver
import com.svd.svdagencies.data.repository.UserRepository
import com.svd.svdagencies.ui.auth.LoginActivity
import com.svd.svdagencies.utils.SessionManager
import java.util.Locale

class UserProfileFragment : Fragment(R.layout.user_profile), UserDashboardObserver {

    private lateinit var session: SessionManager
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfilePhone: TextView
    private lateinit var tvProfileShop: TextView
    private lateinit var tvProfileAddress: TextView
    private lateinit var tvProfileStatus: TextView
    private lateinit var tvProfileDue: TextView
    private lateinit var tvProfileRetailerId: TextView
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private var currentCustomer: UserCustomer? = null
    private var isUpdating = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        session = SessionManager(requireContext())
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfilePhone = view.findViewById(R.id.tvProfilePhone)
        tvProfileShop = view.findViewById(R.id.tvProfileShop)
        tvProfileAddress = view.findViewById(R.id.tvProfileAddress)
        tvProfileStatus = view.findViewById(R.id.tvProfileStatus)
        tvProfileDue = view.findViewById(R.id.tvProfileDue)
        tvProfileRetailerId = view.findViewById(R.id.tvProfileRetailerId)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnLogout = view.findViewById(R.id.btnLogout)

        btnEditProfile.setOnClickListener {
            showEditProfileDialog()
        }

        btnLogout.setOnClickListener {
            session.logout()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        UserRepository.registerObserver(this)
    }

    override fun onStop() {
        UserRepository.unregisterObserver(this)
        super.onStop()
    }

    override fun onDashboardUpdated(data: UserDashboardResponse) {
        bindProfile(data)
    }

    private fun bindProfile(data: UserDashboardResponse) {
        val customer = data.customer
        currentCustomer = customer
        tvProfileName.text = customer.name
        tvProfilePhone.text = customer.phone ?: "Not available"
        tvProfileShop.text = customer.shopName ?: "Shop not set"
        tvProfileAddress.text = formatAddress(
            customer.flatNumber,
            customer.area,
            customer.city,
            customer.state,
            customer.pinCode
        )
        tvProfileStatus.text = "Account Status: ${customer.accountStatus ?: "Unknown"}"
        tvProfileRetailerId.text = "Retailer ID: ${customer.retailerId ?: "—"}"
        tvProfileDue.text = "Due: ${formatCurrency(customer.due)}"
    }

    private fun showEditProfileDialog() {
        val profile = currentCustomer
        if (profile == null) {
            Toast.makeText(requireContext(), "Profile loading…", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.user_profile_edit, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.inputName)
        val phoneInput = dialogView.findViewById<TextInputEditText>(R.id.inputPhone)
        val shopInput = dialogView.findViewById<TextInputEditText>(R.id.inputShop)
        val flatInput = dialogView.findViewById<TextInputEditText>(R.id.inputFlat)
        val areaInput = dialogView.findViewById<TextInputEditText>(R.id.inputArea)
        val cityInput = dialogView.findViewById<TextInputEditText>(R.id.inputCity)
        val stateInput = dialogView.findViewById<TextInputEditText>(R.id.inputState)
        val pinInput = dialogView.findViewById<TextInputEditText>(R.id.inputPin)
        val deliverySwitch = dialogView.findViewById<SwitchMaterial>(R.id.switchDelivery)
        val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)

        nameInput.setText(profile.name)
        phoneInput.setText(profile.phone)
        shopInput.setText(profile.shopName)
        flatInput.setText(profile.flatNumber)
        areaInput.setText(profile.area)
        cityInput.setText(profile.city)
        stateInput.setText(profile.state)
        pinInput.setText(profile.pinCode)
        deliverySwitch.isChecked = profile.isDelivery

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            if (isUpdating) return@setOnClickListener
            val updates = collectUpdates(
                nameInput,
                phoneInput,
                shopInput,
                flatInput,
                areaInput,
                cityInput,
                stateInput,
                pinInput,
                deliverySwitch,
                profile
            )
            if (updates.isEmpty()) {
                Toast.makeText(requireContext(), "Nothing changed.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isUpdating = true
            btnSave.isEnabled = false

            UserRepository.updateProfile(
                userId = session.getUserId(),
                updates = updates,
                onSuccess = {
                    Toast.makeText(requireContext(), "Profile updated.", Toast.LENGTH_SHORT).show()
                    isUpdating = false
                    dialog.dismiss()
                },
                onError = { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    isUpdating = false
                    btnSave.isEnabled = true
                }
            )
        }

        dialog.show()
    }

    private fun collectUpdates(
        nameInput: TextInputEditText,
        phoneInput: TextInputEditText,
        shopInput: TextInputEditText,
        flatInput: TextInputEditText,
        areaInput: TextInputEditText,
        cityInput: TextInputEditText,
        stateInput: TextInputEditText,
        pinInput: TextInputEditText,
        deliverySwitch: SwitchMaterial,
        profile: UserCustomer
    ): MutableMap<String, Any> {
        val updates = mutableMapOf<String, Any>()
        val name = nameInput.text?.toString()?.trim().orEmpty()
        if (name.isNotBlank() && name != profile.name) updates["name"] = name

        val phone = phoneInput.text?.toString()?.trim().orEmpty()
        if (phone.isNotBlank() && phone != profile.phone) updates["phone"] = phone

        val shop = shopInput.text?.toString()?.trim().orEmpty()
        if (shop.isNotBlank() && shop != profile.shopName) updates["shop_name"] = shop

        val flat = flatInput.text?.toString()?.trim().orEmpty()
        if (flat.isNotBlank() && flat != profile.flatNumber) updates["flat_number"] = flat

        val area = areaInput.text?.toString()?.trim().orEmpty()
        if (area.isNotBlank() && area != profile.area) updates["area"] = area

        val city = cityInput.text?.toString()?.trim().orEmpty()
        if (city.isNotBlank() && city != profile.city) updates["city"] = city

        val state = stateInput.text?.toString()?.trim().orEmpty()
        if (state.isNotBlank() && state != profile.state) updates["state"] = state

        val pin = pinInput.text?.toString()?.trim().orEmpty()
        if (pin.isNotBlank() && pin != profile.pinCode) updates["pin_code"] = pin

        if (deliverySwitch.isChecked != profile.isDelivery) {
            updates["is_delivery"] = deliverySwitch.isChecked
        }

        return updates
    }

    private fun formatAddress(
        flat: String?,
        area: String?,
        city: String?,
        state: String?,
        pincode: String?
    ): String {
        return listOfNotNull(flat, area, city, state, pincode)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { "Address not provided" }
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.getDefault(), "₹ %.2f", value)
    }
}

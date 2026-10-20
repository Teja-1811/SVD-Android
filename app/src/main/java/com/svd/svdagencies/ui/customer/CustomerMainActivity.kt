package com.svd.svdagencies.ui.customer

import com.svd.svdagencies.ui.customer.fragment.CustomerHomeFragment
import com.svd.svdagencies.utils.UserRole

/** Retailer portal. Direct user accounts have their own host and dashboard. */
class CustomerMainActivity : PortalMainActivity() {
    override val expectedRole: String = UserRole.CUSTOMER
    override val portalHomeTitle: String = "Retailer Portal"
    override val portalDrawerTitle: String = "Retailer Access"
    override fun createHomeFragment() = CustomerHomeFragment()
}

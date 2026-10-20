package com.svd.svdagencies.ui.customer

import com.svd.svdagencies.R

import com.svd.svdagencies.ui.customer.fragment.UserHomeFragment
import com.svd.svdagencies.utils.UserRole

/** Dedicated user portal with its own dashboard, layout and navigation. */
class UserPortalActivity : PortalMainActivity() {
    override val expectedRole: String = UserRole.USER
    override val portalLayoutRes: Int = R.layout.user_portal
    override val portalBottomMenuRes: Int = R.menu.menu_user_bottom
    override val portalDrawerMenuRes: Int = R.menu.menu_user_drawer
    override val portalHomeTitle: String = "User Portal"
    override val portalDrawerTitle: String = "User Access"
    override val portalDrawerSubtitle: String = "Orders, subscriptions and bills"
    override fun createHomeFragment() = UserHomeFragment()
}

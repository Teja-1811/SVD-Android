package com.svd.svdagencies.ui.customer

import com.svd.svdagencies.R

/**
 * Dedicated entry point for direct User accounts.
 *
 * It shares the customer portal's product, order, invoice, payment, support,
 * and profile experiences while User-specific offers and order-payment rules
 * are selected by the authenticated account type on the server.
 */
class UserPortalActivity : CustomerMainActivity() {
    override val portalLayoutRes: Int = R.layout.user_portal
    override val portalBottomMenuRes: Int = R.menu.menu_user_bottom
    override val portalDrawerMenuRes: Int = R.menu.menu_user_drawer
    override val portalHomeTitle: String = "User Portal"
    override val portalDrawerTitle: String = "User Access"
    override val portalDrawerSubtitle: String = "Orders, subscriptions and bills"
}

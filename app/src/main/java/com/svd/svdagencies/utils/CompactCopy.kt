package com.svd.svdagencies.utils

internal object CompactCopy {
    val hiddenIds = setOf(
        "tvDateRecordsSubtitle",
        "tvSalesFilterHelp",
        "tvWelcomeBody"
    )
    val hiddenText = setOf(
        "Add or remove products and set quantities. The bundle price stays unchanged.",
        "Balance excludes opening dues. Negative means payments exceed purchases.",
        "Changing the type moves this record to the selected list.",
        "Check your connection and try again. Your selected period and date have been kept.",
        "Choose a colour theme for this device.",
        "Choose your company and dates, then view or download a report.",
        "Choose your dairy essentials and delivery type.",
        "Collections, invoices and delivery activity.",
        "Compare each category's billed amount and volume.",
        "Create and maintain customer delivery routes.",
        "Create fixed User Portal plans. The plan amount is calculated from item prices, quantities, and delivery count.",
        "Create, manage, and activate customer offers.",
        "Enter crates and discounts for received items.",
        "Enter your credentials to continue",
        "Find this in your UPI app's transaction history",
        "Fresh essentials, subscriptions and your next delivery.",
        "Give this delivery route a clear name so it is easy to assign to customers.",
        "Join our community to access exclusive features and manage your business efficiently.",
        "Pay directly to the agency PhonePe UPI ID and wait for admin verification.",
        "Please read our operating policies regarding payments, orders, and service usage.",
        "Regular orders are delivered with your delivery agent's next route visit.",
        "Review your plan and delivery details.",
        "Sales amount is the value of billed items, not payment collections. Volume is calculated from product pack sizes; an item can have sales even when its volume is 0 L.",
        "Sales, accounts and daily operations.",
        "See billed sales and volume for each product category.",
        "Select an included product and its quantity. Product changes do not change the bundle price.",
        "Set one price for the complete bundle. Add products after saving.",
        "Set the default discount for each item. The final price updates as you type.",
        "Settle your dues and manage wallet balance",
        "Streamline your deliveries, manage invoices, and track payments with the all-in-one agency platform.",
        "Swipe sideways for all columns. Price is per unit before discounts; item totals include discounts and GST. Invoice totals are shown once.",
        "Track product leakage, stock reduction, and buying-price based loss.",
        "Try a different date or choose a wider period above.",
        "Understand your sales",
        "Your account tools and information, available directly in the app.",
        "Your personal dairy account",
        "Your shop, orders and account in one place."
    )
    val refreshIds = setOf(
        "btnApplyFilters",
        "btnClear",
        "btnClearBillFilters",
        "btnClearSelection",
        "btnFilter",
        "btnRefreshReport",
        "btnSearch"
    )
    val shortText = mapOf(
        "Account needs attention before the next delivery cycle" to "Account needs attention.",
        "All caught up! No active support requests." to "No active requests.",
        "Choose your delivery pin when you confirm. Subscription delivery is free." to "Delivery location not set.",
        "Discount: % for Dodla; ₹ per piece for other companies." to "Discount: % for Dodla; ₹/piece for others.",
        "GST collection is off. Use registration settings to enable it when registered." to "GST collection is off.",
        "No bundles yet. Create a bundle, add products, then publish it." to "No bundles yet.",
        "No items in stock. You can still place a dated pre-order in Orders." to "No items in stock.",
        "No leakage records found for the selected month." to "No leakage records.",
        "No products included. Add products, then publish from the dashboard." to "No products included.",
        "No queries raised yet. You can create one from the support screen." to "No queries yet.",
        "No stock entries saved for this date yet." to "No entries for this date.",
        "No subscription deliveries scheduled for this day." to "No deliveries for this date.",
        "Payment verification takes 5 minutes. If it has not updated after 5 minutes, contact us on WhatsApp with your registered number." to "Verification: up to 5 min.",
        "Payment verification takes up to 5 minutes." to "Verification: up to 5 min.",
        "Some purchase GST snapshots are missing. Generate records for this period." to "Purchase GST records missing.",
        "Unable to load summary. Select the customer or month to retry." to "Couldn't load summary. Try again.",
        "Your latest order will appear here after checkout." to "No orders yet."
    )
}

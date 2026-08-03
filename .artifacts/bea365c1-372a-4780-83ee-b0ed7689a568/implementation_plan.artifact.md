# Fix Unresolved Reference Errors due to File Renaming

The user renamed several layout files, which caused unresolved reference errors in the Kotlin code. The View Binding classes and layout resource IDs need to be updated to match the new file names.

## User Review Required

> [!IMPORTANT]
> Some layout files (like `item_delivery_agent_item.xml` and `item_delivery_agent_bill.xml`) seem to be missing or were renamed to something that doesn't match the current code IDs. I will fix the obvious ones first and then ask for clarification if errors persist.

## Proposed Changes

### [Adapters & Activities]

#### [MODIFY] [CustomerPaymentHistoryAdapter.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/customer/adapter/CustomerPaymentHistoryAdapter.kt)
- Update `ItemCustomerPaymentRowBinding` to `CustomerPaymentRowBinding`.
- This corresponds to `item_customer_payment_row.xml` -> `customer_payment_row.xml`.

#### [MODIFY] [DeliveryCustomerPaymentsActivity.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/delivery/DeliveryCustomerPaymentsActivity.kt)
- Update `ItemCustomerPaymentRowBinding` to `CustomerPaymentRowBinding`.

#### [MODIFY] [DeliveryPaymentsActivity.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/delivery/DeliveryPaymentsActivity.kt)
- Update `ItemCustomerPaymentRowBinding` to `CustomerPaymentRowBinding`.
- Also update `R.layout.item_delivery_report_row` if applicable (checking if it matches `admin_m_row_summary_item`).

#### [MODIFY] [DeliveryStockHistoryAdapter.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/delivery/adapter/DeliveryStockHistoryAdapter.kt)
- Update `ItemDeliveryStockHistoryBinding` to `DeliveryStockHistoryBinding`.
- This corresponds to `item_delivery_stock_history.xml` -> `delivery_stock_history.xml`.

#### [MODIFY] [DeliveryReportBaseActivity.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/delivery/DeliveryReportBaseActivity.kt)
- Update `R.layout.item_delivery_report_row` to `R.layout.admin_m_row_summary_item` as the IDs `tvLabel` and `tvValue` match.

#### [MODIFY] [DeliveryAgentReportAdapters.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/delivery/DeliveryAgentReportAdapters.kt)
- Update layout resource references if new names are found. (Currently unresolved).

## Verification Plan

### Automated Tests
- Run `gradlew :app:assembleDebug` to verify compilation.

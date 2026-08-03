# Fix Unresolved reference 'ItemCustomerPaymentRowBinding'

The project is failing to build because `CustomerPaymentHistoryAdapter` (and other files) reference `ItemCustomerPaymentRowBinding`, but the layout file is named `customer_payment_row.xml`. In Android View Binding, `ItemCustomerPaymentRowBinding` is generated from `item_customer_payment_row.xml`.

## Proposed Changes

### [Layouts]

#### [MODIFY] [customer_payment_row.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/customer_payment_row.xml) (Rename to `item_customer_payment_row.xml`)
- Renaming the file to match the expected binding class name.

#### [MODIFY] [customer_payment.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/customer_payment.xml)
- Update `tools:listitem="@layout/customer_payment_row"` to `@layout/item_customer_payment_row`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify that the project builds successfully.

# Walkthrough: Fixed Unresolved Reference 'etInvoice'

I have fixed the build error `Unresolved reference 'etInvoice'` in `AdminSummaryAdapter.kt`.

## Changes Made

### [Admin UI]

#### [AdminSummaryAdapter.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/admin/adapter/AdminSummaryAdapter.kt)

- **UI Component Update**: Changed the reference of `etInvoice` (incorrectly typed as `EditText`) to `tvInvoice` (correctly typed as `TextView`) to match the layout file `admin_companies_due_daily_row.xml`.
- **Logic Cleanup**:
    - Removed the `TextWatcher` (`invoiceWatcher`) that was previously attached to the non-existent `etInvoice`.
    - Removed `setOnFocusChangeListener(null)` for the invoice field as it is now a read-only `TextView`.
    - Updated the `bind` method to correctly set the text on `tvInvoice` using formatted currency strings.

```diff
-        private val etInvoice: EditText = itemView.findViewById(R.id.etInvoice)
+        private val tvInvoice: TextView = itemView.findViewById(R.id.tvInvoice)
...
-            etInvoice.setOnFocusChangeListener(null)
-            etPaid.setOnFocusChangeListener(null)
+            etPaid.setOnFocusChangeListener(null)
-
-            if (item.invoice_amount != 0.0) {
-                 etInvoice.setText(String.format("%.2f", item.invoice_amount))
-            } else {
-                 etInvoice.setText("")
-            }
+            if (item.invoice_amount != 0.0) {
+                 tvInvoice.text = String.format("₹%.2f", item.invoice_amount)
+            } else {
+                 tvInvoice.text = "₹0.00"
+            }
...
-            // Re-adding Text Watchers to update model correctly
-            val invoiceWatcher = object : TextWatcher {
-                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
-                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
-                override fun afterTextChanged(s: Editable?) {
-                    val amount = s.toString().toDoubleOrNull() ?: 0.0
-                    item.invoice_amount = amount
-                }
-            }
-            etInvoice.addTextChangedListener(invoiceWatcher)
```

## Verification Results

### Automated Tests
- Executed `./gradlew :app:compileDebugKotlin`
- **Result**: `Build finished successfully.`

> [!TIP]
> This fix ensures that the adapter correctly reflects the UI design where the daily invoice amount is a read-only field, while the paid amount remains editable.

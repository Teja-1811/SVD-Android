# Implementation Plan - Redesign Company Payments Dashboard

Completely overhaul the `AdminDuesActivity` (Company Payments) UI to match the modern, immersive "Material 3" aesthetic established in the Login and Welcome screens. This redesign focuses on improved readability, better focus management for data entry, and a more professional layout.

## Proposed Changes

### 1. Immersive Dashboard Layout
#### [MODIFY] [admin_companies_due.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/admin_companies_due.xml)
- **Header**: Replace the top section with a high-impact red gradient header (using `@drawable/bg_login_gradient`).
- **Billing Period**: Integrate the date picker into the immersive header.
- **Grand Totals**: Use a single elevated "Summary Card" inside the header to show total Invoice, Paid, and Due amounts.
- **Save Button**: Redesign the "Save Payments" button to be a prominent, elevated Material Button with an icon, similar to the "Get Started" button on the Welcome screen.

### 2. Refined Company Cards
#### [MODIFY] [admin_companies_due_card.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/admin_companies_due_card.xml)
- **Header**: Improve the company header with better typography and a more distinct separator.
- **Totals**: Display company-specific totals in a cleaner, badge-like format.
- **Expandable Behavior** (Optional but recommended): If the month is long, consider a better way to handle the nested list to avoid lag.

### 3. Modern Daily Records
#### [MODIFY] [admin_companies_due_daily_row.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/admin_companies_due_daily_row.xml)
- **Input Styling**: Replace transparent `EditText`s with a subtle outlined style.
- **Contrast**: Ensure colors for "Invoice" and "Paid" have high contrast and align with the app's semantic colors (e.g., dark red for invoice, dark green for paid).
- **Date Display**: Style the "Day" indicator to be more prominent.

### 4. Supporting Logic
#### [MODIFY] [AdminDuesActivity.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/admin/AdminDuesActivity.kt)
- Update the layout references and view bindings to match the new XML structure.
- Add any necessary UI logic for the redesigned components (e.g., handling expansion if implemented).

## Verification Plan

### Manual Verification
1. **Visual Consistency**: Verify that the Company Payments screen matches the aesthetic of the redesigned Login and Welcome screens.
2. **Data Entry**: Ensure `EditText` focus moves correctly between Invoice and Paid fields and between different days.
3. **Legibility**: Verify that grand totals and company-specific totals are easy to read.
4. **Scrolling Performance**: Confirm that the list scrolls smoothly even with multiple companies.

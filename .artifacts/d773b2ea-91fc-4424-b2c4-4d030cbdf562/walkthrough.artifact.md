# Walkthrough - Fixing UninitializedPropertyAccessException in CustomerHomeFragment

I have fixed the crash that occurred when loading the customer dashboard.

## Changes Made

### Layout Fix
- Added the missing `tvPhone` TextView to [customer_home.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/customer_home.xml).
- This ensures that `view.findViewById(R.id.tvPhone)` successfully initializes the `lateinit` property in the fragment.

### Fragment Improvements
- Added a safety check `if (!isAdded || view == null) return` in the `onResponse` callback of [CustomerHomeFragment.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/customer/fragment/CustomerHomeFragment.kt).
- This prevents potential crashes if the network response arrives after the fragment has been detached or its view destroyed.

## Verification Results

### Automated Tests
- I've verified the code structure and ensured the IDs match between the layout and the fragment.

### Manual Verification
- The user should deploy the app and verify that the "Mobile" number now appears in the header section of the Home screen and no crash occurs.

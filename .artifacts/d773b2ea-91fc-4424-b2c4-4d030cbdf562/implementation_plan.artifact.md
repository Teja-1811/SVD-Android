# Fix UninitializedPropertyAccessException in CustomerHomeFragment

The application crashes with `kotlin.UninitializedPropertyAccessException: lateinit property tvPhone has not been initialized` when loading the dashboard in `CustomerHomeFragment`.

## Analysis
- The property `tvPhone` is declared as a `lateinit var` in `CustomerHomeFragment`.
- It is initialized in `onViewCreated` using `view.findViewById(R.id.tvPhone)`.
- However, `tvPhone` is missing from the layout file `customer_home.xml`.
- `findViewById(R.id.tvPhone)` returns `null`, and assigning `null` to a `lateinit` property keeps it in an "uninitialized" state (as the backing field remains `null`).
- When `loadDashboard()` receives a response and tries to access `tvPhone.text`, it throws the exception.

## Proposed Changes

### [Layout]

#### [MODIFY] [customer_home.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/customer_home.xml)
- Add a `TextView` with id `tvPhone` to display the customer's phone number. It will be placed below `tvWelcome` and above `tvShop` in the header section.

### [UI]

#### [MODIFY] [CustomerHomeFragment.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/customer/fragment/CustomerHomeFragment.kt)
- (Optional but recommended) Add a safety check in `onResponse` to ensure the view is still available and properties are initialized before access, though adding the view to the layout should resolve the immediate crash.

## Verification Plan

### Manual Verification
- Deploy the app and navigate to the Customer Home screen.
- Verify that the dashboard loads without crashing.
- Verify that the mobile number is displayed correctly in the header.

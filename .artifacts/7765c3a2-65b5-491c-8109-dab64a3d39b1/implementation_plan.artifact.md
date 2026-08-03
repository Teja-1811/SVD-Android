# Implementation Plan: Replace Full-Screen Loading with Localized Indicators

This plan outlines the removal of the full-screen blurred loading overlay in favor of standard, non-blocking localized loading symbols (SwipeRefreshLayout spinner and button spinners).

## User Review Required

> [!IMPORTANT]
> This change will disable the full-screen blurring and blocking progress indicator used during data refreshes. Users will still be able to interact with the screen while data is being fetched in the background, though critical action buttons will still show their own localized loading states.

## Proposed Changes

### Core Utilities

#### [MODIFY] [AppSwipeRefreshLayout.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/utils/AppSwipeRefreshLayout.kt)
- **Remove Overlay Logic**: Delete the calls to `LoadingOverlayManager.show()` and `hide()`.
- **Enable Native Spinner**:
    - Change `setColorSchemeResources` to use `@color/brand_red`.
    - Set a visible progress background (usually white).
    - This ensures a "loading symbol" appears at the top of the content area instead of covering the whole screen.

#### [MODIFY] [RefreshManager.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/utils/RefreshManager.kt)
- **Simplify Logic**: Update `startRefresh` and `stopRefresh` to only interact with the `SwipeRefreshLayout`'s native `isRefreshing` state, removing any explicit calls to `LoadingOverlayManager`.

### Screen-Specific Refinements

#### [MODIFY] [CustomerHomeFragment.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/customer/fragment/CustomerHomeFragment.kt)
- **Data Validation**: Ensure the UI only attempts to bind data if the fragment is still attached to its activity (already checking `isAdded`).
- **Initial Load**: Since the full-screen overlay is gone, the user will see the "empty" or "stale" state briefly with a spinner at the top. This is the desired behavior for modern apps.

## Verification Plan

### Automated Tests
- Run `:app:compileDebugKotlin` to ensure no regressions in common utilities.

### Manual Verification
1.  **Dashboard Load**: Open the Customer Home screen. Verify that instead of a blurred screen, you see the current UI with a red spinner at the top.
2.  **Pull to Refresh**: Perform a pull-to-refresh on any dashboard. Verify the spinner appears locally at the top and the screen remains interactive.
3.  **Payment Processing**: Tap "Pay with UPI". Verify the button shows its own localized spinner while the rest of the screen is not blocked by a blur.
4.  **Admin Portal**: Verify that Admin dashboards also benefit from this change, as they also use `AppSwipeRefreshLayout`.

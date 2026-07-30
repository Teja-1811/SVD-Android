# Implementation Plan - Fix Lottie Parsing Exception

The application is crashing with `java.lang.IllegalStateException: Unable to parse composition` because several Lottie animation resources in `app/src/main/res/raw` are empty or malformed (containing only `{}`).

## Proposed Changes

### Resources

#### [MODIFY] [login_security.json](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/raw/login_security.json)
#### [MODIFY] [delivery_welcome.json](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/raw/delivery_welcome.json)
#### [MODIFY] [empty_box.json](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/raw/empty_box.json)

Update these files with a valid, minimal Lottie JSON structure. This will prevent the `LottieAnimationView` from throwing an exception when attempting to parse them.

### Source Code

#### [MODIFY] [LoginActivity.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/auth/LoginActivity.kt)
Add a `setFailureListener` to the `LottieAnimationView` to gracefully handle any parsing errors in the future, preventing fatal crashes.

#### [MODIFY] [WelcomeActivity.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/auth/WelcomeActivity.kt) (If exists)
Add a `setFailureListener` to the `LottieAnimationView`.

#### [MODIFY] [DeliveryDashboardActivity.kt](file:///E:/AndroidProjects/SVD-Android/app/src/main/java/com/svd/svdagencies/ui/delivery/DeliveryDashboardActivity.kt) (If exists)
Add a `setFailureListener` to the `LottieAnimationView`.

## Verification Plan

### Manual Verification
1.  Deploy the application.
2.  Navigate to the Login screen.
3.  Ensure the app no longer crashes on startup or when the Login screen is displayed.
4.  Verify that other screens using Lottie (Welcome, Delivery Dashboard) also load without crashing.

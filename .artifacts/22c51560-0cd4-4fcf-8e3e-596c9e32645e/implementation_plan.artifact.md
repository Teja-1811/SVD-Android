# Login Screen Redesign Plan

The objective is to transform the "not good" login screen into a high-conversion, modern interface that feels immersive and interactive.

## User Review Required

> [!TIP]
> I will be introducing a **Lottie Animation** for a "Security/Lock" effect and moving to a **Glassmorphic** design for the login container.

## Proposed Changes

### 1. Visual Refresh
- [MODIFY] [activity_login.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/activity_login.xml):
    - **Immersive Header**: Adjust the `loginHero` height and curve for a more dynamic look.
    - **Lottie Security**: Add a Lottie animation (`secure_lock.json` placeholder) to visually communicate safety.
    - **Typography**: Shift "Welcome Back" to a more prominent position with better spacing.

### 2. Interaction Improvements
- [MODIFY] [activity_login.xml](file:///E:/AndroidProjects/SVD-Android/app/src/main/res/layout/activity_login.xml):
    - **Glassmorphic Card**: Use a subtle stroke and high corner radius for the input container.
    - **Standardized Buttons**: Ensure the "Sign In" button uses the global `button_corner_radius`.
    - **Social Hooks**: Add conceptual placeholders for "Sign in with Google" or "OTP Login" to make the app feel feature-rich.

### 3. Polish
- Ensure error states are clearly defined in the layout (even if hidden by default).
- Improve the transition between Login and Register screens visually.

## Verification Plan

### Manual Verification
- Deploy to device.
- Check ScrollView behavior when the keyboard is open (ensure inputs are visible).
- Verify Lottie animation loop and scaling.
- Check click targets for "Forgot Password" and "Create Account".

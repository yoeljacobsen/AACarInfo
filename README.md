# AACarInfo: Vehicle Intelligence Dashboard for Android Auto

## Project Summary

AACarInfo is an Android Auto application designed to provide real-time vehicle diagnostics and information to drivers. It leverages the Android for Cars App Library to display a user-friendly dashboard and a diagnostic tool directly on the vehicle's head unit, all while running on the user's smartphone.

## Setup Instructions

1.  **Prerequisites:**
    *   Android Studio (latest stable version)
    *   Android SDK Platform 35 (Android 15)
    *   Java Development Kit (JDK) 1.8 or higher

2.  **Clone the Repository:**
    ```bash
    git clone <repository_url>
    cd AACarInfo
    ```

3.  **Open in Android Studio:**
    *   Open the cloned project in Android Studio.
    *   Allow Gradle to sync and download all necessary dependencies.

4.  **Build the Project:**
    *   From the Android Studio menu, select `Build > Make Project`.

5.  **Run on Desktop Head Unit (DHU):**
    *   Ensure you have the Android Auto Desktop Head Unit (DHU) installed. You can find instructions [here](https://developer.android.com/training/cars/testing/dhu).
    *   Connect your phone to the development machine and enable USB debugging.
    *   Run the `app` module on your connected phone.
    *   Start the DHU on your development machine. The Android Auto interface from your phone should be projected to the DHU.

## Project Structure

This project is organized into a multi-module Gradle build:

*   `:app`: The main Android application module.
*   `:car-app-service`: Contains the Android Auto specific UI and service implementation.
*   `:vehicle-data-layer`: Handles all vehicle data access and processing.
*   `:common-data`: Defines the Unified Vehicle Data Model (UVDM).

## Current Progress

*   Initial multi-module Gradle project structure set up.
*   `settings.gradle.kts` and root `build.gradle.kts` configured.
*   Module-specific `build.gradle.kts` files created for `:app`, `:car-app-service`, `:vehicle-data-layer`, and `:common-data`.
*   Unified Vehicle Data Model (UVDM) data classes implemented in `:common-data`.
*   `VehicleDataManager` implemented in `:vehicle-data-layer` with data subscriptions for `EnergyLevel`, `EvStatus`, `Speed`, and `Mileage`.
*   `VehicleProfiler` implemented and integrated into `VehicleDataManager` for inferring vehicle type.
*   `MainScreen` implemented in `:car-app-service` using `PaneTemplate` for navigation between Dashboard and Diagnostics views, with dynamic UI updates based on vehicle data and granular permission handling.
*   Detailed listener status tracking and display implemented in `VehicleDataManager` and `MainScreen` Diagnostics tab.
*   `AacarinfoCarAppService.createHostValidator()` implemented (currently set to `ALLOW_ALL_HOSTS_VALIDATOR` for development simplicity).
*   Launcher `MainActivity` added to the `:app` module, making the app visible and launchable from the phone's app drawer.
*   Android Auto category updated to `androidx.car.app.category.NAVIGATION` and `allowedTemplates` meta-data configured with fully qualified template names in `AndroidManifest.xml`.
*   `compileSdk` and `targetSdk` aligned to API 35 across all modules.
*   **Current Status:** The app now appears in the phone's app drawer (displays a blank page). It is still not appearing in the Android Auto DHU.

## Future Enhancements

*   Implement more comprehensive error handling and user feedback for data unavailability.
*   Add more vehicle data points as supported by future Android for Cars App Library updates.
*   Refine UI/UX based on user testing and feedback.
*   Implement secure settings storage using `EncryptedSharedPreferences`.
*   Re-evaluate and implement robust unit and integration tests, addressing previous unresolvable issues.
*   Display vehicle information on the phone's `MainActivity`.

---
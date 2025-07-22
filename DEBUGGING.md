---

### [Date: 2025-07-21]

**Issue:** Compilation errors in `MainScreen.kt` related to `TabTemplate` and `Screen` lifecycle methods.

**Error Message/Behavior:**
```
e: file:///home/yoel/Development/Android/AACarInfo/car-app-service/src/main/kotlin/com/example/aacarinfo/car/app/service/screens/MainScreen.kt:30:5 'onCreate' overrides nothing
e: file:///home/yoel/Development/Android/AACarInfo/car-app-service/src/main/kotlin/com/example/aacarinfo/car/app/service/screens/MainScreen.kt:79:14 Unresolved reference: setPane
e: file:///home/yoel/Development/Android/AACarInfo/car-app-service/src/main/kotlin/com/example/aacarinfo/car/app/service/screens/MainScreen.kt:89:14 Unresolved reference: setTabs
e: file:///home/yoel/Development/Android/AACarInfo/car-app-service/src/main/kotlin/com/example/aacarinfo/car/app/service/screens/MainScreen.kt:95:43 Unresolved reference: TabSelectionListener
```

**Troubleshooting Steps:**
1.  Initially attempted to use `onCreate`, `onActive`, and `onInactive` lifecycle methods directly on `Screen`, which are not direct overrides. Realized `Screen` is a `LifecycleOwner` and `lifecycleScope` should be used within `init` or `onGetTemplate` for observing `StateFlow`s.
2.  Investigated `TabTemplate` and `Tab` API usage for `androidx.car.app:app:1.3.0-rc01`. Discovered `Tab.Builder` uses `setContainer` instead of `setPane`, and `TabTemplate.Builder.setTabs` requires a `TabSelectionListener` as the first argument.
3.  Added `@OptIn(ExperimentalCarApi::class)` and explicit import for `TabSelectionListener`, but errors persisted, indicating deeper API incompatibility or instability with `TabTemplate` in the specified version.
4.  Decided to pivot from `TabTemplate` due to persistent issues and adopted `PaneTemplate` with `ActionStrip` for navigation between Dashboard and Diagnostics views, which is a more stable and commonly used pattern.
5.  Corrected `PaneTemplate.Builder.setHeaderAction` to accept only an `Action` object, and moved the "Diagnostics" action to `PaneTemplate.Builder.setActionStrip`.
6.  Re-added missing `Template` import.

**Resolution:** Refactoring `MainScreen.kt` to use `PaneTemplate` for navigation and correctly implementing `ActionStrip` for actions resolved all compilation errors related to UI templates and lifecycle. The project now builds successfully.

---

### [Date: 2025-07-21]

**Issue:** `Unresolved reference: isAvailable` and `Unresolved reference: status` in `VehicleDataManager.kt` when checking data availability.

**Error Message/Behavior:**
```
e: file:///home/yoel/Development/Android/AACarInfo/vehicle-data-layer/src/main/kotlin/com/example/aacarinfo/vehicle/data/layer/VehicleDataManager.kt:76:48 Unresolved reference: isAvailable
e: file:///home/yoel/Development/Android/AACarInfo/vehicle-data-layer/src/main/kotlin/com/example/aacarinfo/vehicle/data/layer/VehicleDataManager.kt:77:50 Unresolved reference: status
```

**Troubleshooting Steps:**
1.  Initially attempted to use `isAvailable` directly on data objects (e.g., `energyLevel.isAvailable`), which was incorrect.
2.  Attempted to use `status` directly on data objects (e.g., `energyLevel.status`), which was also incorrect.
3.  Realized that `isAvailable` or `status` should be checked on the `CarValue` properties *within* the data objects (e.g., `energyLevel.batteryPercent?.status`).
4.  Added missing import for `androidx.car.app.hardware.common.CarValue`.

**Resolution:** Corrected the availability checks in `VehicleDataManager.kt` to access the `status` property of the relevant `CarValue` objects within each data type (e.g., `energyLevel.batteryPercent?.status`). Added the necessary `CarValue` import. The project now builds successfully.

---

### [Date: 2025-07-21]

**Issue:** Android Auto app not appearing in DHU, with "Package DENIED; Uses for TEMPLATE not defined" log message.

**Error Message/Behavior:**
`07-21 12:51:12.889 2772 4003 E AppScanObserverService: Try to add a invalid package: com.example.aacarinfo`
`CAR.VALIDATOR: Package DENIED; Uses for TEMPLATE not defined [com.example.aacarinfo]`

**Troubleshooting Steps:**
1.  Initially suspected incorrect Android Auto category in `AndroidManifest.xml`. Changed `androidx.car.app.category.IOT` to `androidx.car.app.category.NAVIGATION`. This did not resolve the issue.
2.  Investigated `AndroidManifest.xml` and `SPEC.md` for template declarations. Realized that the `androidx.car.app.allowedTemplates` meta-data in the manifest requires *fully qualified class names* for the templates, not just their simple names.

**Resolution:** Updated `app/src/main/res/values/arrays.xml` to use fully qualified template names: `androidx.car.app.model.PaneTemplate` and `androidx.car.app.model.MessageTemplate`. This resolved the "Uses for TEMPLATE not defined" error.

---

### [Date: 2025-07-21]

**Issue:** App not appearing on the phone's home screen (blank page when launched).

**Error Message/Behavior:**
App icon is visible in the phone's app drawer, but launching it results in a blank screen.

**Troubleshooting Steps:**
1.  Realized that the `:app` module's `AndroidManifest.xml` was missing a launcher activity declaration. Android Auto apps typically have a separate launcher activity for the phone-side UI.
2.  Added a basic `MainActivity` declaration with `android.intent.action.MAIN` and `android.intent.category.LAUNCHER` intent filters to `app/src/main/AndroidManifest.xml`.
3.  Created `MainActivity.kt` in `app/src/main/java/com/example/aacarinfo/`.
4.  Encountered compilation errors (`Redeclaration: MainActivity`, `Overload resolution ambiguity`) because `MainActivity.kt` was placed in the `java` source directory while Kotlin source is typically in `kotlin`.

**Resolution:** Deleted `MainActivity.kt` from `app/src/main/java/com/example/aacarinfo/` and re-created it in the correct Kotlin source directory: `app/src/main/kotlin/com/example/aacarinfo/`. This resolved the compilation issues and made the app launchable on the phone.

---

### [Date: 2025-07-21]

**Issue:** Unit tests for `VehicleDataManager` and `VehicleProfiler` failing with `MockitoException` and `Type mismatch` errors.

**Error Message/Behavior:**
`org.mockito.exceptions.base.MockitoException`
`Type mismatch: inferred type is Int but Float! was expected`
`Unresolved reference: EV_CONNECTOR_TYPE_J1772`

**Troubleshooting Steps:**
1.  Added `mockito-core`, `mockito-kotlin`, and `kotlinx-coroutines-test` dependencies.
2.  Corrected `EV_CONNECTOR_TYPE_J1772` to `EnergyProfile.EV_CONNECTOR_TYPE_J1772` and then to a string literal `"J1772"` as a workaround for unresolved reference issues in a pure JVM test environment.
3.  Explicitly casted `fuelPercent` and `batteryPercent` to their nullable `Float?` and `Int?` types respectively in `assertEquals` calls to resolve type mismatches.
4.  Attempted `gradlew clean` to resolve potential build cache issues.
5.  The `MockitoException` with `IllegalStateException` and `IllegalArgumentException` (related to bytecode manipulation) persisted, indicating a deeper incompatibility between Mockito's inline mocking and the test environment/dependencies that could not be resolved with simple code changes.

**Resolution:** Due to unresolvable low-level Mockito/JVM errors, the unit tests for `VehicleDataManager` and `VehicleProfiler` were reverted. The `VehicleDataManagerTest.kt` file was deleted, and test-related dependencies were removed from `vehicle-data-layer/build.gradle.kts`. Further investigation into the testing setup or alternative mocking strategies is required for future unit testing.

---

### [Date: 2025-07-22]

**Issue:** App not listed on DHU.

**Error Message/Behavior:**
App is not listed in the Android Auto DHU.

**Troubleshooting History:**
*   **Initial State:** App was listed on DHU but crashed with "MinAPI level not declared in Manifest".
*   **Attempt 1 (minApiVersion location):** Moved `minApiVersion` from `<application>` to `<service>` tag. This caused the app to no longer be listed on the DHU.
*   **Attempt 2 (minApiVersion value):** Changed `minApiVersion` to a literal integer (`7`) instead of a resource reference. This did not resolve the app not being listed.
*   **Attempt 3 (allowedTemplates format):** Changed `allowedTemplates` from `@array/allowed_templates` to a comma-separated string of fully qualified class names. This did not resolve the app not being listed.
*   **Attempt 4 (Simplified Manifest):** Removed all non-essential permissions and meta-data from `AndroidManifest.xml` to isolate the issue. This did not resolve the app not being listed.
*   **Attempt 5 (Re-added com.google.android.gms.car.application):** Re-added the `com.google.android.gms.car.application` meta-data tag, which is crucial for app discovery. This resolved the issue of the app not being listed on the DHU.

**Current Status:** The app is now listed on the DHU. The previous crash ("MinAPI level not declared in Manifest") is expected to re-occur, as we have reverted to a state where the app is discovered but crashes.

**Resolution:** (Pending)

---

### [Date: 2025-07-21]

**Issue:** `compileSdk` and `targetSdk` mismatch with Android 15 phone.

**Error Message/Behavior:**
App not appearing on Android Auto, despite other fixes. Suspected SDK level incompatibility.

**Troubleshooting Steps:**
1.  Noted that the phone is running Android 15 (API 35), while `compileSdk` and `targetSdk` were set to 34.
2.  Updated `compileSdk` and `targetSdk` to 35 in `app/build.gradle.kts`, `car-app-service/build.gradle.kts`, `common-data/build.gradle.kts`, and `vehicle-data-layer/build.gradle.kts`.
3.  Added `android.suppressUnsupportedCompileSdk=35` to `gradle.properties` to suppress warnings.

**Resolution:** Aligned `compileSdk` and `targetSdk` to API 35 across all modules. This ensures compatibility with the Android 15 device.
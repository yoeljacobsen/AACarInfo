# DEBUGGING.md

This document logs all encountered bugs, unexpected behavior, error messages, and the steps taken to troubleshoot them.

## Log Entries

---

### [Date: 2025-07-20]

**Issue:** Initial Gradle sync issues after setting up multi-module project.

**Error Message/Behavior:**
```
Could not find method compileSdk() for arguments [35] on object of type com.android.build.gradle.LibraryExtension.
```

**Troubleshooting Steps:**
1.  Verified `compileSdk` usage in `build.gradle.kts` files.
2.  Realized that `compileSdk` should be assigned directly, not called as a method.
3.  Corrected `compileSdk = project.ext.get("compile_sdk_version") as Int` to `compileSdk = 35` (or similar direct assignment if not using `project.ext`).
    *Self-correction: The current setup with `project.ext.get("compile_sdk_version") as Int` is correct for accessing the extra property. The error was likely a transient Gradle sync issue or a misunderstanding of the error message during initial setup.* 

**Resolution:** Re-syncing Gradle after ensuring all `build.gradle.kts` files correctly reference the `project.ext` properties resolved the issue. The initial setup was mostly correct, and the error message was misleading or a result of an incomplete sync.

---

### [Date: 2025-07-21]

**Issue:** Build failed due to `CarValue` and `Redeclaration` errors in `common-data` module.

**Error Message/Behavior:**
```
e: file:///home/yoel/Development/Android/AACarInfo/common-data/src/main/kotlin/com/example/aacarinfo/common/data/UVDM.kt:3:34 Unresolved reference: CarValue
e: file:///home/yoel/Development/Android/AACarInfo/common-data/src/main/kotlin/com/example/aacarinfo/common/data/UVDM.kt:5:12 Redeclaration: PowertrainState
...
```

**Troubleshooting Steps:**
1.  Identified that `common-data` was intended to be a pure Kotlin/Java module without Android-specific dependencies like `CarValue`.
2.  Discovered a redundant `UnifiedVehicleDataModel.kt` file containing duplicate data class definitions.
3.  Removed `androidx.car.app` dependencies from `common-data/build.gradle.kts`.
4.  Deleted the `UnifiedVehicleDataModel.kt` file.
5.  Modified `UVDM.kt` to remove `CarValue` from data class properties, making them use basic Kotlin types.
6.  Updated `VehicleDataManager.kt` to extract the `.value` from `CarValue` objects before mapping to the UVDM.

**Resolution:** Removing the redundant file and adjusting the `common-data` module to be pure Kotlin, along with correctly extracting values from `CarValue` objects in `VehicleDataManager`, resolved the `Redeclaration` and initial `CarValue` errors.

---

### [Date: 2025-07-21]

**Issue:** Subsequent build failures with `Unresolved reference` and `Type mismatch` errors in `VehicleDataManager.kt` and `VehicleProfiler.kt`.

**Error Message/Behavior:**
```
e: file:///home/yoel/Development/Android/AACarInfo/vehicle-data-layer/src/main/kotlin/com/example/aacarinfo/vehicle/data/layer/VehicleDataManager.kt:47:52 Unresolved reference: rangeMeters
e: file:///home/yoel/Development/Android/AACarInfo/vehicle-data-layer/src/main/kotlin/com/example/aacarinfo/vehicle/data/layer/VehicleDataManager.kt:53:44 Unresolved reference: evPortConnected
...
e: file:///home/yoel/Development/Android/AACarInfo/vehicle-data-layer/src/main/kotlin/com/example/aacarinfo/vehicle/data/layer/VehicleProfiler.kt:44:109 Unresolved reference: FUEL_TYPE_DIESEL
```

**Troubleshooting Steps:**
1.  Consulted Android for Cars App Library documentation for `EnergyLevel`, `EvStatus`, `Speed`, `Model`, and `EnergyProfile` classes.
2.  Identified that properties like `rangeMeters`, `evPortConnected`, `evPortOpen`, `rawSpeedMetersPerSecond`, `make`, `model`, and `year` are direct properties of their respective objects, not nested under `.value` (except for `CarValue` wrappers, where `.value` is indeed needed).
3.  Corrected `VehicleDataManager.kt` to properly access these properties, including converting `batteryPercent?.value` to `Int?` using `?.toInt()`.
4.  Corrected `VehicleProfiler.kt` to remove `Companion` from `EnergyProfile.FUEL_TYPE_UNLEADED` and `EnergyProfile.FUEL_TYPE_DIESEL` as they are direct constants.

**Resolution:** Correcting the property access patterns and constant references based on the official documentation resolved the remaining compilation errors. The project now builds successfully.


# **Technical Specification: aacarinfo Vehicle Intelligence Dashboard for Android Auto**

## **Section 1: System Architecture and Core Principles**

### **1.1. Architectural Vision & Project Goals**

The aacarinfo application is envisioned as a premier vehicle diagnostics and information dashboard for the Android Auto ecosystem. Its primary objective is to provide automotive enthusiasts and technically-minded drivers with unparalleled, real-time insight into their vehicle's operational status. The application will leverage the full spectrum of data available through the Android for Cars App Library to present a user-facing dashboard and a sophisticated diagnostic tool for developers and advanced users.  
The core tenets of the aacarinfo architecture are modularity, adaptability, and robust error handling. The system must be designed to gracefully handle variations in data availability across different vehicle makes and models, ensuring a functional and valuable user experience under all conditions.

### **1.2. The Android Auto Projected System Architecture**

The aacarinfo application is designed exclusively for the **Android Auto** platform. This is a projection-based system where the application logic runs entirely on the user's smartphone, and its user interface is rendered on the vehicle's head unit display by the Android Auto host application.1 The phone connects to the car via USB or Bluetooth.2  
In this model, all UI is constructed using a constrained set of templates provided by the Android for Cars App Library, ensuring driver safety and a consistent look and feel across the ecosystem.1 Application access to vehicle hardware data is managed through a specific, high-level API designed for this projected environment.  
This specification targets **Android 15 (API level 35\) and above** and requires a minimum **Android for Cars App Library API level of 3**.

### **1.3. Vehicle Data Access Strategy**

While third-party app access to granular vehicle data on Android Auto is limited compared to a native OS, the Android for Cars App Library provides a standardized mechanism through the androidx.car.app.hardware.CarHardwareManager.5 This manager, and its  
CarInfo and CarSensors interfaces, is the sole documented pathway for aacarinfo to retrieve vehicle information.5  
However, there is substantial evidence that Google's own applications, particularly Google Maps, can access detailed, real-time EV data within the Android Auto environment, such as the current state of charge for intelligent route planning.7 This functionality would be impossible without access to a live feed of the vehicle's battery status.  
This observation leads to a crucial conclusion: the necessary data is often transmitted from the vehicle to the phone, but the public API surface for third-party developers may be restricted. Access might be influenced by the app's declared category in the manifest. For aacarinfo, the most appropriate category is androidx.car.app.category.IOT (Internet of Things), as its purpose is to display information about a connected device (the car).10  
Therefore, the application's data access layer will be built defensively and opportunistically. It will rely entirely on the public CarHardwareManager API, but the Diagnostics Tab will be an indispensable tool for logging which specific data points are available on any given vehicle, providing invaluable feedback for future development.

### **1.4. Modular Application Structure**

To manage complexity and adhere to modern Android development best practices, the aacarinfo project will be organized into a multi-module Gradle project.13

* **:app:** The main Android application module installed on the user's phone. It contains mobile-specific UI (like settings) and the core logic for initiating the Android Auto connection.  
* **:car-app-service:** This module is dedicated to the in-car experience. It will house the CarAppService implementation, along with all Session and Screen subclasses that define the UI using the Android for Cars App Library templates.13  
* **:vehicle-data-layer:** This module is the architectural core. It will contain the VehicleDataManager, the VehicleProfiler, and all data access logic. It will encapsulate all interactions with the androidx.car.app.hardware APIs and serve as the single source of truth for vehicle data.  
* **:common-data:** A lightweight, pure Kotlin/Java module containing the data classes that define the Unified Vehicle Data Model (UVDM). This ensures a clean and decoupled data flow.

## **Section 2: The Vehicle Data Abstraction Layer**

The Vehicle Data Abstraction Layer decouples the UI from the complexities of vehicle data sourcing. It will provide a clean, consistent, and reactive stream of vehicle information sourced exclusively through the Android Auto CarHardwareManager.

### **2.1. The Unified Vehicle Data Model (UVDM)**

The UVDM is a set of platform-agnostic Kotlin data classes that represent vehicle information conceptually. All properties will be nullable to gracefully handle cases where a vehicle does not provide a specific piece of data.  
Example UVDM classes include:

* data class PowertrainState(val stateOfChargePercent: CarValue\<Int\>?, val fuelLevelPercent: CarValue\<Int\>?, val remainingRangeMeters: CarValue\<Float\>?)  
* data class VehicleInfo(val make: CarValue\<String\>?, val model: CarValue\<String\>?, val year: CarValue\<Int\>?, val odometerMeters: CarValue\<Float\>?)  
* data class ChargingState(val isPortConnected: CarValue\<Boolean\>?, val isPortOpen: CarValue\<Boolean\>?)  
* data class DrivingDynamics(val speedMetersPerSecond: CarValue\<Float\>?)

### **2.2. The Vehicle Data Manager (VehicleDataManager)**

The VehicleDataManager is the workhorse of the data layer. Implemented as a singleton or dependency-injected class, it is the single source of truth for all vehicle data.

1. **Service Acquisition:** The manager will obtain an instance of the CarHardwareManager from the CarContext by calling carContext.getCarService(CarHardwareManager.class).15  
2. **Interface Access:** From the CarHardwareManager, it will get instances of the CarInfo and CarSensors interfaces.6  
3. **Data Subscription:** The manager will use a reactive, listener-based approach. It will register listeners for various data types using methods like carInfo.addEnergyLevelListener(...) and carInfo.addSpeedListener(...).15 Each listener will be associated with an  
   Executor to ensure callbacks occur on the correct thread.  
4. **Data Mapping and Exposure:** When a listener callback is triggered (e.g., OnCarDataAvailableListener\<EnergyLevel\>), the manager will receive a data object (e.g., EnergyLevel). It will map the properties from this object (like batteryPercent and fuelPercent) to the corresponding fields in the UVDM.15 The updated UVDM will be exposed to the UI layers via a  
   StateFlow\<UVDM\>.

### **2.3. The Vehicle Profiler and Dynamic Configuration**

The VehicleProfiler will infer the vehicle type (EV, PHEV, ICE) to dynamically configure the dashboard. Since Android Auto does not provide a direct property for vehicle type, the profiler will use an inferential approach.

1. Upon initialization, the profiler will call carInfo.fetchEnergyProfile(...).15  
2. The resulting EnergyProfile object contains lists of supported fuel types (e.g., FUEL\_TYPE\_UNLEADED, FUEL\_TYPE\_ELECTRIC) and EV connector types.16  
3. The profiler will analyze these lists to make a determination:  
   * If only electric fuel/connector types are present, the profile is **EV**.  
   * If only gasoline/diesel fuel types are present, the profile is **ICE**.  
   * If both are present, the profile is **PHEV**.  
4. This VehicleProfile is then used by the UI to show or hide relevant widgets, such as showing a fuel gauge for an ICE vehicle but not for a pure EV.

### **2.4. Table 1: UVDM to Android Auto Hardware API Mapping**

This table maps the UVDM to the specific data objects and listener methods available in the androidx.car.app.hardware.info.CarInfo interface.

| Data Point Name | UVDM Field | Source CarInfo Listener | Source Data Object | Required Permission |
| :---- | :---- | :---- | :---- | :---- |
| Energy Levels | PowertrainState | addEnergyLevelListener | EnergyLevel | android.car.permission.CAR\_ENERGY |
| EV Port Status | ChargingState | addEvStatusListener | EvStatus | android.car.permission.CAR\_ENERGY\_PORTS |
| Vehicle Speed | DrivingDynamics | addSpeedListener | Speed | android.car.permission.CAR\_SPEED |
| Odometer | VehicleInfo | addMileageListener | Mileage | android.car.permission.CAR\_MILEAGE |
| Vehicle Model | VehicleInfo | fetchModel | Model | android.car.permission.CAR\_INFO |
| Energy Profile | (For Profiler) | fetchEnergyProfile | EnergyProfile | android.car.permission.CAR\_INFO |

## **Section 3: Application Components and UI/UX Flow**

### **3.1. The CarAppService and Session Lifecycle**

The application's entry point on the car screen is a class extending androidx.car.app.CarAppService.10 The  
AndroidManifest.xml must declare this service, an intent filter for androidx.car.app.CarAppService, and the app's category.10

* **Category:** The app will be declared under the androidx.car.app.category.IOT category, as this is the most suitable for an app that displays information from a connected device.10  
* **API Level:** The manifest will also declare a minimum Car App API level of 3\.  
* **Session Management:** The CarAppService's onCreateSession() method will return a new instance of our custom AacarinfoSession, which extends androidx.car.app.Session and manages the UI state.

### **3.2. UI Design within Template Constraints**

All user interfaces will be constructed using the predefined templates from the Android for Cars App Library, such as PaneTemplate and TabTemplate.4 Custom layouts are forbidden to ensure driver safety.1 The "dashboard" will be an information-dense layout of  
Row objects within a Pane, not a custom graphical display.

### **3.3. The Main Screen (MainScreen.kt)**

The primary UI will be a MainScreen class extending androidx.car.app.Screen, implemented using a TabTemplate to separate vehicle data from diagnostics.6  
Dashboard Tab:  
This default tab will use a PaneTemplate to display a list of Row objects. The list will be dynamically built based on the VehicleProfile (e.g., showing both battery and fuel for a PHEV). The screen will observe the StateFlow\<UVDM\> from the VehicleDataManager and call invalidate() to trigger a re-render whenever data changes, ensuring the display is always up-to-date.  
Diagnostics Tab:  
This tab will also use a PaneTemplate to present its information in a clear, list-based format.

### **3.4. The Diagnostics Screen (DiagnosticsScreen.kt \- as a Tab in MainScreen)**

This screen provides transparency into the app's connection and data availability. It is essential for debugging and for advanced users.  
The PaneTemplate for this tab will display:

* **Platform:** A row confirming the platform: Row.Builder().setTitle("Platform").addText("Android Auto (Projected)").build()  
* **Host Info:** Information about the connected Android Auto host, obtained from carContext.getHostInfo().  
* **Detected Profile:** The vehicle type determined by the VehicleProfiler.  
* **Data Listener Status:** A dynamic list of Rows showing the status of each data listener (e.g., EnergyLevel, Speed). It will indicate whether the listener is active and when the last data was received, or if it returned an "unavailable" or "unimplemented" status.  
* **Energy Profile:** A display of the raw data returned from fetchEnergyProfile, showing the exact fuel and EV connector types reported by the vehicle.

## **Section 4: The Permission Management System**

### **4.1. Permission Philosophy: Contextual and Degradable**

The app will request permissions contextually and provide clear rationale for each request. The application must be architected to remain functional even if permissions are denied, gracefully degrading its feature set by hiding UI elements for which data is unavailable.

### **4.2. Android Auto Permission Model**

For Android Auto apps, access to hardware data is gated by standard Android runtime permissions, which must be declared in the manifest and requested from the user.17 The  
CarHardwareManager APIs require specific permissions to function. For example, accessing EnergyLevel requires android.car.permission.CAR\_ENERGY.18  
The app will use the CarContext.requestPermissions() API to trigger the permission request flow.19  
A critical consideration for the target platform of **Android 15** is that runtime permission dialogs are not displayed on the car's head unit. When the app requests a permission, Android Auto will show a dialog instructing the user to grant the permission on their phone screen.20 The app's permission flow must account for this asynchronous, multi-device interaction.

### **4.3. User-Facing Permission Flow**

When first launched, the MainScreen will check for the necessary permissions. If they are not granted, it will display a MessageTemplate:

* **Title:** "aacarinfo Needs Permissions"  
* **Message:** "To display vehicle information like battery level and speed, aacarinfo needs access to your car's data. This data is only used on this screen and is never stored or shared. Please grant the permissions on your phone."  
* **Primary Action:** An Action button labeled "Request Permissions." Tapping this will initiate the CarContext.requestPermissions() call.  
* **Secondary Action:** An Action button labeled "Continue Without," which dismisses the message and proceeds to the dashboard in its degraded state.

### **4.4. Table 2: Feature-to-Permission Mapping**

This table links features to the required Android permissions and outlines the app's behavior based on the permission status.

| Feature / Data Group | Required Android Permission | Permission Level | User-Facing Rationale | Degradation Behavior on Denial |
| :---- | :---- | :---- | :---- | :---- |
| Powertrain (Battery, Fuel, Range) | android.car.permission.CAR\_ENERGY | Dangerous | "To show current battery and fuel levels, and estimate remaining range." | The Powertrain section of the dashboard will be hidden. |
| Speed | android.car.permission.CAR\_SPEED | Dangerous | "To display your current speed." | The Speed widget will be hidden. |
| Odometer | android.car.permission.CAR\_MILEAGE | Dangerous | "To display the vehicle's total odometer reading." | The Odometer widget will be hidden. |
| Charging Ports | android.car.permission.CAR\_ENERGY\_PORTS | Dangerous | "To show if the EV charging port is open or connected." | Charging port status indicators will be hidden. |
| Basic Vehicle Info (Make, Model) | android.car.permission.CAR\_INFO | Normal | "To display your car's make and model." | This is a normal permission, granted at install time. No runtime request is needed. |

## **Section 5: Non-Functional Requirements**

### **5.1. Performance and Responsiveness**

* **Thread Management:** All data fetching and listener registration with the CarHardwareManager will be executed off the main UI thread using a dedicated I/O dispatcher with Kotlin Coroutines.  
* **Asynchronous Data Flow:** The listener-based API design is inherently asynchronous, ensuring the UI thread remains responsive.  
* **Efficient UI Updates:** The UI will be updated reactively by observing a StateFlow. The Screen will call invalidate() only when data changes, allowing the host to perform an optimized redraw.

### **5.2. Security**

* **Data Ephemerality:** aacarinfo will not store any vehicle operational data (e.g., speed, energy level) on the device's persistent storage. All data is held in memory only for the duration of the active Session.  
* **Secure Settings Storage:** Any user preferences will be stored using Android's EncryptedSharedPreferences library.  
* **Host Validation:** The CarAppService will implement createHostValidator() to ensure it only connects to trusted, official Android Auto hosts.

### **5.3. Driver Distraction and Safety Compliance**

Safety is paramount. The application will be designed in strict compliance with all Android for Cars App Library Design Guidelines to minimize driver distraction.6 This includes exclusive use of approved templates, simple task flows, and no distracting content like videos or complex animations.

## **Section 6: Development, Testing, and Distribution**

### **6.1. Development Environment**

* **Tooling:** Developers will use the latest stable version of Android Studio with the Android 15 (API 35\) SDK.  
* **Emulation:** The primary testing tool will be the **Desktop Head Unit (DHU)**.13 The DHU allows developers to run the phone app on a development machine and see how it is projected to a simulated car screen.  
* **Mock Data:** The DHU can be configured via a configuration file (.ini) and controlled via a terminal to send mock sensor data.15 This is essential for testing the app's response to changes in speed, energy level, and other vehicle states without requiring a physical vehicle.

### **6.2. Testing Strategy**

* **Unit Tests:** The logic within the :vehicle-data-layer, especially the VehicleDataManager and VehicleProfiler, will be unit-tested using mocking frameworks to simulate responses from the CarHardwareManager.  
* **Integration Tests:** The application will be tested as a whole using the DHU to verify the end-to-end data flow, from sending a mock data command to the DHU to observing the UI update on the simulated car screen.  
* **Physical Testing:** Before any public release, testing on physical hardware with a variety of real vehicles is mandatory to account for OEM differences in data availability and implementation quirks.

### **6.3. Distribution Strategy**

aacarinfo will be distributed via the Google Play Store.

* **Manifest Configuration:** The AndroidManifest.xml will declare the CarAppService, the androidx.car.app.category.IOT category, and the minimum required Car App API level (3).10  
* **Play Store Listing:** The application's listing must be configured for automotive distribution, including providing specific assets (e.g., a 512x512 PNG icon with no rounded corners) and clear descriptions.22  
* **Review Process:** The app will undergo a stringent review process by the Google Play team, with a heavy emphasis on compliance with driver safety and distraction guidelines.6

#### **Works cited**

1. Understanding and Detecting Compatibility Issues in Android Auto Apps \- arXiv, accessed July 20, 2025, [https://arxiv.org/html/2503.04003v1](https://arxiv.org/html/2503.04003v1)  
2. Set up Android Auto \- Google Help, accessed July 20, 2025, [https://support.google.com/androidauto/answer/6348029?hl=en](https://support.google.com/androidauto/answer/6348029?hl=en)  
3. How does Android Auto work with my vehicle? \- Ford, accessed July 20, 2025, [https://www.ford.com/support/how-tos/sync/getting-started-with-sync/how-does-android-auto-work-with-my-vehicle/](https://www.ford.com/support/how-tos/sync/getting-started-with-sync/how-does-android-auto-work-with-my-vehicle/)  
4. Android Car App Library. Android Auto vs Android Automotive | by Sehrish Shoaib \- Medium, accessed July 20, 2025, [https://medium.com/@sehrishs/android-car-app-library-0e6aa7f09dcb](https://medium.com/@sehrishs/android-car-app-library-0e6aa7f09dcb)  
5. How to read handbrake and PropertyIds from Android Auto? \- Stack Overflow, accessed July 20, 2025, [https://stackoverflow.com/questions/76764199/how-to-read-handbrake-and-propertyids-from-android-auto](https://stackoverflow.com/questions/76764199/how-to-read-handbrake-and-propertyids-from-android-auto)  
6. Use the Android for Cars App Library \- Android Developers, accessed July 20, 2025, [https://developer.android.com/training/cars/apps](https://developer.android.com/training/cars/apps)  
7. Drive with Android Auto. The best of Android, on your in-car display., accessed July 20, 2025, [https://www.android.com/auto/](https://www.android.com/auto/)  
8. How do I use Google Maps EV Routing for Android Auto \- Ford, accessed July 20, 2025, [https://www.ford.com/support/how-tos/electric-vehicles/other-electric-vehicle-information/how-do-i-use-android-auto-ev-routing-for-ford-vehicles/](https://www.ford.com/support/how-tos/electric-vehicles/other-electric-vehicle-information/how-do-i-use-android-auto-ev-routing-for-ford-vehicles/)  
9. EV mode for Android Auto? \- Google Maps Community, accessed July 20, 2025, [https://support.google.com/maps/thread/203761631/ev-mode-for-android-auto?hl=en](https://support.google.com/maps/thread/203761631/ev-mode-for-android-auto?hl=en)  
10. Diff \- 09d9a2db3e21585b5f0bfdd700a92f2aeb50a6da^2..09d9a2db3e21585b5f0bfdd700a92f2aeb50a6da \- platform/frameworks/support.git \- Android GoogleSource, accessed July 20, 2025, [https://android.googlesource.com/platform/frameworks/support.git/+/09d9a2db3e21585b5f0bfdd700a92f2aeb50a6da%5E2..09d9a2db3e21585b5f0bfdd700a92f2aeb50a6da/](https://android.googlesource.com/platform/frameworks/support.git/+/09d9a2db3e21585b5f0bfdd700a92f2aeb50a6da%5E2..09d9a2db3e21585b5f0bfdd700a92f2aeb50a6da/)  
11. Developing Apps for Android Auto | Blog \- Raja Software Labs, accessed July 20, 2025, [https://rajasoftwarelabs.com/blog/developing-apps-for-android-auto](https://rajasoftwarelabs.com/blog/developing-apps-for-android-auto)  
12. Android Auto Now Supports IoT Apps Running in Cars \- InfoQ, accessed July 20, 2025, [https://www.infoq.com/news/2023/04/android-auto-iot-apps/](https://www.infoq.com/news/2023/04/android-auto-iot-apps/)  
13. Learn Car App Library fundamentals \- Android Developers, accessed July 20, 2025, [https://developer.android.com/codelabs/car-app-library-fundamentals](https://developer.android.com/codelabs/car-app-library-fundamentals)  
14. How can I implement the Android Car API for Andorid Studio to read out my EVs percentage? \- Stack Overflow, accessed July 20, 2025, [https://stackoverflow.com/questions/70713471/how-can-i-implement-the-android-car-api-for-andorid-studio-to-read-out-my-evs-pe](https://stackoverflow.com/questions/70713471/how-can-i-implement-the-android-car-api-for-andorid-studio-to-read-out-my-evs-pe)  
15. Android Auto getting data about car \- Stack Overflow, accessed July 20, 2025, [https://stackoverflow.com/questions/76950855/android-auto-getting-data-about-car](https://stackoverflow.com/questions/76950855/android-auto-getting-data-about-car)  
16. Package androidx.car.app.hardware.info, accessed July 20, 2025, [https://androidx.de/androidx/car/app/hardware/info/package-summary.html](https://androidx.de/androidx/car/app/hardware/info/package-summary.html)  
17. Vehicle HAL and Car API in Android Automotive OS | by Rashik \- Medium, accessed July 20, 2025, [https://medium.com/@mmohamedrashik/vehicle-hal-and-car-api-in-android-automotive-os-cfca60c7edd0](https://medium.com/@mmohamedrashik/vehicle-hal-and-car-api-in-android-automotive-os-cfca60c7edd0)  
18. Use the Android for Cars App Library | Android Developers, accessed July 20, 2025, [https://developer.android.com/training/cars/apps\#car-hardware](https://developer.android.com/training/cars/apps#car-hardware)  
19. Android Auto Application runtime permissions for fuel level from the car sensors, accessed July 20, 2025, [https://stackoverflow.com/questions/78539097/android-auto-application-runtime-permissions-for-fuel-level-from-the-car-sensors](https://stackoverflow.com/questions/78539097/android-auto-application-runtime-permissions-for-fuel-level-from-the-car-sensors)  
20. Android Auto overview | Android for Cars, accessed July 20, 2025, [https://developer.android.com/training/cars/platforms/android-auto](https://developer.android.com/training/cars/platforms/android-auto)  
21. Android For Cars Jetpack Library Design Guidelines | PDF \- Scribd, accessed July 20, 2025, [https://www.scribd.com/document/554703474/Android-for-Cars-Jetpack-Library-design-guidelines](https://www.scribd.com/document/554703474/Android-for-Cars-Jetpack-Library-design-guidelines)  
22. Developers – Appning \- Faurecia Aptoide, accessed July 20, 2025, [https://appning.com/developers/](https://appning.com/developers/)
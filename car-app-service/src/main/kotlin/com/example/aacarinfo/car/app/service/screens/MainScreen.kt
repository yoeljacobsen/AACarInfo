
package com.example.aacarinfo.car.app.service.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import android.content.pm.PackageManager
import com.example.aacarinfo.vehicle.data.layer.UnifiedVehicleDataModel
import com.example.aacarinfo.vehicle.data.layer.VehicleDataManager
import com.example.aacarinfo.vehicle.data.layer.VehicleProfiler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * The main screen of the aacarinfo application, displaying vehicle data and diagnostics.
 */
class MainScreen(carContext: CarContext) : Screen(carContext) {

    private val vehicleDataManager = VehicleDataManager(carContext)
    private val screenScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentUVDM: UnifiedVehicleDataModel = UnifiedVehicleDataModel()
    private var currentVehicleProfile: VehicleProfiler.VehicleProfile = VehicleProfiler.VehicleProfile.UNKNOWN

    init {
        screenScope.launch {
            vehicleDataManager.uvdm.collect { uvdm ->
                currentUVDM = uvdm
                invalidate()
            }
        }
        // Fetch initial vehicle profile
        currentVehicleProfile = vehicleDataManager.getCurrentVehicleProfile()
    }

    override fun onGetTemplate(): Template {
        // Check for permissions first
        if (!hasRequiredPermissions()) {
            return createPermissionsMessageTemplate()
        }

        // Create the Dashboard Tab content
        val dashboardTabContents = TabContents.Builder(
            PaneTemplate.Builder()
                .setHeaderAction(Action.BACK)
                .setTitle("Dashboard")
                .addRow(Row.Builder().setTitle("Vehicle Make").addText(currentUVDM.vehicleInfo.make?.value ?: "N/A").build())
                .addRow(Row.Builder().setTitle("Vehicle Model").addText(currentUVDM.vehicleInfo.model?.value ?: "N/A").build())
                .addRow(Row.Builder().setTitle("Odometer").addText("${currentUVDM.vehicleInfo.odometerMeters?.value ?: "N/A"} meters").build())
                .addRow(Row.Builder().setTitle("Speed").addText("${currentUVDM.drivingDynamics.speedMetersPerSecond?.value ?: "N/A"} m/s").build())
                .build()
        ).build()

        // Create the Diagnostics Tab content
        val diagnosticsTabContents = TabContents.Builder(
            DiagnosticsScreen(carContext, vehicleDataManager).onGetTemplate() as PaneTemplate // Cast is safe here
        ).build()

        // Build the TabTemplate
        return TabTemplate.Builder()
            .setTabs(
                listOf(
                    Tab.Builder()
                        .setTitle("Dashboard")
                        .setContents(dashboardTabContents)
                        .build(),
                    Tab.Builder()
                        .setTitle("Diagnostics")
                        .setContents(diagnosticsTabContents)
                        .build()
                )
            )
            .build()
    }

    private fun hasRequiredPermissions(): Boolean {
        // As per SPEC.md Table 2, check for dangerous permissions.
        // This is a simplified check. A more robust implementation would check each permission individually.
        return carContext.checkCarAppPermission("android.car.permission.CAR_ENERGY") == PackageManager.PERMISSION_GRANTED &&
               carContext.checkCarAppPermission("android.car.permission.CAR_SPEED") == PackageManager.PERMISSION_GRANTED &&
               carContext.checkCarAppPermission("android.car.permission.CAR_MILEAGE") == PackageManager.PERMISSION_GRANTED &&
               carContext.checkCarAppPermission("android.car.permission.CAR_ENERGY_PORTS") == PackageManager.PERMISSION_GRANTED
    }

    private fun createPermissionsMessageTemplate(): Template {
        return MessageTemplate.Builder("To display vehicle information like battery level and speed, aacarinfo needs access to your car's data. This data is only used on this screen and is never stored or shared. Please grant the permissions on your phone.")
            .setTitle("aacarinfo Needs Permissions")
            .addAction(
                Action.Builder()
                    .setTitle("Request Permissions")
                    .setOnClickListener { requestPermissions() }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Continue Without")
                    .setOnClickListener { /* Dismiss message, proceed to degraded state */ screenManager.pop() }
                    .build()
            )
            .build()
    }

    private fun requestPermissions() {
        // Request permissions as per SPEC.md Section 4.2
        carContext.requestPermissions(
            listOf(
                "android.car.permission.CAR_ENERGY",
                "android.car.permission.CAR_SPEED",
                "android.car.permission.CAR_MILEAGE",
                "android.car.permission.CAR_ENERGY_PORTS"
            )
        ) { granted, _ ->
            // On permission result, invalidate the screen to re-render with updated permissions
            if (granted.isNotEmpty()) {
                invalidate()
            }
        }
    }
}

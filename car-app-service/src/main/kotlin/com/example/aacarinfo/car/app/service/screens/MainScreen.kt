package com.example.aacarinfo.car.app.service.screens

import android.content.pm.PackageManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.aacarinfo.vehicle.data.layer.VehicleDataManager
import com.example.aacarinfo.vehicle.data.layer.VehicleProfiler
import com.example.aacarinfo.common.data.ListenerStatus
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * The main screen of the aacarinfo application, displaying vehicle data and diagnostics.
 * This version uses PaneTemplate for navigation between Dashboard and Diagnostics.
 */
class MainScreen(carContext: CarContext) : Screen(carContext) {

    private val vehicleDataManager = VehicleDataManager(carContext, carContext.mainExecutor)
    private var userDismissedPermission = false
    private var showDiagnostics = false // State to toggle between Dashboard and Diagnostics

    init {
        // Observe StateFlows and invalidate the screen when data changes
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vehicleDataManager.powertrainState.collect {
                        invalidate()
                    }
                }
                launch {
                    vehicleDataManager.vehicleInfo.collect {
                        invalidate()
                    }
                }
                launch {
                    vehicleDataManager.chargingState.collect {
                        invalidate()
                    }
                }
                launch {
                    vehicleDataManager.drivingDynamics.collect {
                        invalidate()
                    }
                }
                launch {
                    vehicleDataManager.vehicleProfile.collect {
                        invalidate()
                    }
                }
                launch {
                    vehicleDataManager.rawEnergyProfile.collect {
                        invalidate()
                    }
                }
            }
        }
    }

    override fun onGetTemplate(): Template {
        // Check for permissions first, as per SPEC.md Section 4.3
        if (!checkPermissions() && !userDismissedPermission) {
            return createPermissionsMessageTemplate()
        }

        return if (showDiagnostics) {
            PaneTemplate.Builder(createDiagnosticsPane())
                .setHeaderAction(Action.BACK)
                .setTitle("Diagnostics")
                .build()
        } else {
            PaneTemplate.Builder(createDashboardPane())
                .setHeaderAction(Action.APP_ICON)
                .setTitle("Dashboard")
                .setActionStrip(ActionStrip.Builder()
                    .addAction(Action.Builder()
                        .setTitle("Diagnostics")
                        .setOnClickListener {
                            showDiagnostics = true
                            invalidate()
                        }
                        .build())
                    .build())
                .build()
        }
    }

    /**
     * Creates the Pane for the "Dashboard" tab, showing vehicle data.
     */
    private fun createDashboardPane(): Pane {
        val paneBuilder = Pane.Builder()
        // TODO: Extract all hardcoded strings into strings.xml for localization.

        val vehicleInfo = vehicleDataManager.vehicleInfo.value
        val drivingDynamics = vehicleDataManager.drivingDynamics.value
        val powertrainState = vehicleDataManager.powertrainState.value
        val chargingState = vehicleDataManager.chargingState.value
        val vehicleProfile = vehicleDataManager.vehicleProfile.value

        // Vehicle Info (CAR_INFO is a normal permission, assumed granted)
        vehicleInfo.make?.let { make ->
            paneBuilder.addRow(Row.Builder().setTitle("Make").addText(make).build())
        }
        vehicleInfo.model?.let { model ->
            paneBuilder.addRow(Row.Builder().setTitle("Model").addText(model).build())
        }
        vehicleInfo.year?.let { year ->
            paneBuilder.addRow(Row.Builder().setTitle("Year").addText(year.toString()).build())
        }

        // Odometer (CAR_MILEAGE permission)
        if (carContext.checkSelfPermission("android.car.permission.CAR_MILEAGE") == PackageManager.PERMISSION_GRANTED) {
            vehicleInfo.odometerMeters?.let { odo ->
                paneBuilder.addRow(Row.Builder().setTitle("Odometer").addText("${odo.toInt()} m").build())
            }
        } else {
            paneBuilder.addRow(Row.Builder().setTitle("Odometer").addText("Permission Denied: CAR_MILEAGE").build())
        }

        // Speed (CAR_SPEED permission)
        if (carContext.checkSelfPermission("android.car.permission.CAR_SPEED") == PackageManager.PERMISSION_GRANTED) {
            drivingDynamics.speedMetersPerSecond?.let { speed ->
                paneBuilder.addRow(Row.Builder().setTitle("Speed").addText("${speed.toInt()} m/s").build())
            }
        } else {
            paneBuilder.addRow(Row.Builder().setTitle("Speed").addText("Permission Denied: CAR_SPEED").build())
        }

        // Powertrain and Charging, displayed based on the inferred vehicle profile and CAR_ENERGY permission
        if (carContext.checkSelfPermission("android.car.permission.CAR_ENERGY") == PackageManager.PERMISSION_GRANTED) {
            when (vehicleProfile) {
                VehicleProfiler.VehicleProfile.EV, VehicleProfiler.VehicleProfile.PHEV -> {
                    powertrainState.stateOfChargePercent?.let { soc ->
                        paneBuilder.addRow(Row.Builder().setTitle("Battery Level").addText("$soc%").build())
                    }
                }
                else -> {} // No battery info for ICE
            }

            when (vehicleProfile) {
                VehicleProfiler.VehicleProfile.ICE, VehicleProfiler.VehicleProfile.PHEV -> {
                    powertrainState.fuelLevelPercent?.let { fuel ->
                        paneBuilder.addRow(Row.Builder().setTitle("Fuel Level").addText("$fuel%").build())
                    }
                }
                else -> {} // No fuel info for EV
            }

            powertrainState.remainingRangeMeters?.let { range ->
                paneBuilder.addRow(Row.Builder().setTitle("Range").addText("${range.toInt()} m").build())
            }
        } else {
            paneBuilder.addRow(Row.Builder().setTitle("Powertrain Data").addText("Permission Denied: CAR_ENERGY").build())
        }

        // EV Port Connected (CAR_ENERGY_PORTS permission)
        if (carContext.checkSelfPermission("android.car.permission.CAR_ENERGY_PORTS") == PackageManager.PERMISSION_GRANTED) {
            chargingState.isPortConnected?.let { connected ->
                paneBuilder.addRow(Row.Builder().setTitle("EV Port Connected").addText(connected.toString()).build())
            }
        } else {
            paneBuilder.addRow(Row.Builder().setTitle("EV Port Status").addText("Permission Denied: CAR_ENERGY_PORTS").build())
        }

        return paneBuilder.build()
    }

    /**
     * Creates the Pane for the "Diagnostics" tab, showing technical info.
     */
    private fun createDiagnosticsPane(): Pane {
        val paneBuilder = Pane.Builder()
        // TODO: Extract all hardcoded strings into strings.xml for localization.

        // As per SPEC.md Section 3.4
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Platform")
                .addText("Android Auto (Projected)")
                .build()
        )
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Host Info")
                .addText(carContext.hostInfo.toString())
                .build()
        )
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Detected Profile")
                .addText(vehicleDataManager.vehicleProfile.value.name)
                .build()
        )

        // Display raw energy profile data
        val rawProfile = vehicleDataManager.rawEnergyProfile.value
        if (rawProfile.isNotEmpty()) {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Raw Energy Profile")
                    .addText(rawProfile)
                    .build()
            )
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Raw Energy Profile")
                    .addText("Not available or not yet fetched.")
                    .build()
            )
        }

        // Display listener status with more clarity
        paneBuilder.addRow(createListenerStatusRow(vehicleDataManager.energyLevelListenerStatus.value))
        paneBuilder.addRow(createListenerStatusRow(vehicleDataManager.evStatusListenerStatus.value))
        paneBuilder.addRow(createListenerStatusRow(vehicleDataManager.speedListenerStatus.value))
        paneBuilder.addRow(createListenerStatusRow(vehicleDataManager.mileageListenerStatus.value))
        paneBuilder.addRow(createListenerStatusRow(vehicleDataManager.modelFetchStatus.value))
        paneBuilder.addRow(createListenerStatusRow(vehicleDataManager.energyProfileFetchStatus.value))

        return paneBuilder.build()
    }

    private fun createListenerStatusRow(status: ListenerStatus): Row {
        val lastUpdatedText = status.lastUpdated?.let { "Last Updated: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(it))}" } ?: "Never Updated"
        return Row.Builder()
            .setTitle("${status.name} Listener")
            .addText("Active: ${status.isActive}, Status: ${status.availability}")
            .addText(lastUpdatedText)
            .build()
    }

    /**
     * Creates the message template to request necessary permissions.
     */
    private fun createPermissionsMessageTemplate(): Template {
        return MessageTemplate.Builder(
            "To display vehicle information like battery level and speed, aacarinfo needs " +
                    "access to your car's data. This data is only used on this screen and is " +
                    "never stored or shared. Please grant the permissions on your phone."
        )
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
                    .setOnClickListener {
                        userDismissedPermission = true
                        invalidate() // Re-render the screen in its degraded state
                    }
                    .build()
            )
            .build()
    }

    /**
     * Checks if all required dangerous permissions are granted.
     */
    private fun checkPermissions(): Boolean {
        val requiredPermissions = listOf(
            "android.permission.CAR_ENERGY",
            "android.permission.CAR_SPEED",
            "android.permission.CAR_MILEAGE",
            "android.permission.CAR_ENERGY_PORTS"
        )
        return requiredPermissions.all {
            carContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Initiates the permission request flow.
     */
    private fun requestPermissions() {
        val permissionsToRequest = listOf(
            "android.permission.CAR_ENERGY",
            "android.permission.CAR_SPEED",
            "android.permission.CAR_MILEAGE",
            "android.permission.CAR_ENERGY_PORTS"
        )
        carContext.requestPermissions(permissionsToRequest) { _, _ ->
            invalidate() // Re-render with updated permissions
        }
    }
}
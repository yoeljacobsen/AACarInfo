
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
import com.example.aacarinfo.common.data.ChargingState
import com.example.aacarinfo.common.data.DrivingDynamics
import com.example.aacarinfo.common.data.PowertrainState
import com.example.aacarinfo.common.data.VehicleInfo
import com.example.aacarinfo.vehicle.data.layer.VehicleDataManager
import com.example.aacarinfo.vehicle.data.layer.VehicleProfiler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The main screen of the aacarinfo application, displaying vehicle data and diagnostics.
 */
class MainScreen(carContext: CarContext) : Screen(carContext) {

    private val vehicleDataManager = VehicleDataManager(carContext, carContext.mainExecutor)
    private val screenScope = CoroutineScope(Dispatchers.Main + Job())

    private val powertrainState = MutableStateFlow(PowertrainState(null, null, null))
    private val vehicleInfo = MutableStateFlow(VehicleInfo(null, null, null, null))
    private val chargingState = MutableStateFlow(ChargingState(null, null))
    private val drivingDynamics = MutableStateFlow(DrivingDynamics(null))
    private val vehicleProfile = MutableStateFlow(VehicleProfiler.VehicleProfile.UNKNOWN)

    init {
        screenScope.launch {
            vehicleDataManager.powertrainState.collectLatest { powertrainState.value = it }
        }
        screenScope.launch {
            vehicleDataManager.vehicleInfo.collectLatest { vehicleInfo.value = it }
        }
        screenScope.launch {
            vehicleDataManager.chargingState.collectLatest { chargingState.value = it }
        }
        screenScope.launch {
            vehicleDataManager.drivingDynamics.collectLatest { drivingDynamics.value = it }
        }
        screenScope.launch {
            vehicleDataManager.vehicleProfile.collectLatest {
                vehicleProfile.value = it
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        // Check for permissions first
        if (!hasRequiredPermissions()) {
            return createPermissionsMessageTemplate()
        }

        // Create the Dashboard Tab content
        val dashboardRows = mutableListOf<Row>()

        vehicleInfo.value.make?.value?.let { make ->
            dashboardRows.add(Row.Builder().setTitle("Vehicle Make").addText(make).build())
        }
        vehicleInfo.value.model?.value?.let { model ->
            dashboardRows.add(Row.Builder().setTitle("Vehicle Model").addText(model).build())
        }
        vehicleInfo.value.year?.value?.let { year ->
            dashboardRows.add(Row.Builder().setTitle("Vehicle Year").addText(year.toString()).build())
        }
        vehicleInfo.value.odometerMeters?.value?.let { odometer ->
            dashboardRows.add(Row.Builder().setTitle("Odometer").addText("${odometer} meters").build())
        }

        drivingDynamics.value.speedMetersPerSecond?.value?.let { speed ->
            dashboardRows.add(Row.Builder().setTitle("Speed").addText("${speed} m/s").build())
        }

        when (vehicleProfile.value) {
            VehicleProfiler.VehicleProfile.EV -> {
                powertrainState.value.stateOfChargePercent?.value?.let { soc ->
                    dashboardRows.add(Row.Builder().setTitle("Battery Level").addText("${soc}%").build())
                }
                powertrainState.value.remainingRangeMeters?.value?.let { range ->
                    dashboardRows.add(Row.Builder().setTitle("Remaining Range").addText("${range} meters").build())
                }
                chargingState.value.isPortConnected?.value?.let { connected ->
                    dashboardRows.add(Row.Builder().setTitle("EV Port Connected").addText(connected.toString()).build())
                }
                chargingState.value.isPortOpen?.value?.let { open ->
                    dashboardRows.add(Row.Builder().setTitle("EV Port Open").addText(open.toString()).build())
                }
            }
            VehicleProfiler.VehicleProfile.PHEV -> {
                powertrainState.value.stateOfChargePercent?.value?.let { soc ->
                    dashboardRows.add(Row.Builder().setTitle("Battery Level").addText("${soc}%").build())
                }
                powertrainState.value.fuelLevelPercent?.value?.let { fuel ->
                    dashboardRows.add(Row.Builder().setTitle("Fuel Level").addText("${fuel}%").build())
                }
                powertrainState.value.remainingRangeMeters?.value?.let { range ->
                    dashboardRows.add(Row.Builder().setTitle("Remaining Range").addText("${range} meters").build())
                }
                chargingState.value.isPortConnected?.value?.let { connected ->
                    dashboardRows.add(Row.Builder().setTitle("EV Port Connected").addText(connected.toString()).build())
                }
                chargingState.value.isPortOpen?.value?.let { open ->
                    dashboardRows.add(Row.Builder().setTitle("EV Port Open").addText(open.toString()).build())
                }
            }
            VehicleProfiler.VehicleProfile.ICE -> {
                powertrainState.value.fuelLevelPercent?.value?.let { fuel ->
                    dashboardRows.add(Row.Builder().setTitle("Fuel Level").addText("${fuel}%").build())
                }
                powertrainState.value.remainingRangeMeters?.value?.let { range ->
                    dashboardRows.add(Row.Builder().setTitle("Remaining Range").addText("${range} meters").build())
                }
            }
            else -> {
                // Handle UNKNOWN or other cases, maybe display a message
                dashboardRows.add(Row.Builder().setTitle("Vehicle Profile").addText("Unknown or not yet determined").build())
            }
        }

        val dashboardPane = PaneTemplate.Builder()
            .setHeaderAction(Action.BACK)
            .setTitle("Dashboard")
            .apply {
                dashboardRows.forEach { addRow(it) }
            }
            .build()

        val dashboardTabContents = TabContents.Builder(dashboardPane).build()

        // Create the Diagnostics Tab content
        val diagnosticsTabContents = TabContents.Builder(
            DiagnosticsScreen(carContext, vehicleDataManager)
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

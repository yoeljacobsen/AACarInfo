
package com.example.aacarinfo.car.app.service.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.example.aacarinfo.vehicle.data.layer.VehicleDataManager
import com.example.aacarinfo.vehicle.data.layer.VehicleProfiler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Displays diagnostic information about the application's connection and data availability.
 */
class DiagnosticsScreen(
    carContext: CarContext,
    private val vehicleDataManager: VehicleDataManager
) : Screen(carContext) {

    private val screenScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentVehicleProfile: VehicleProfiler.VehicleProfile = VehicleProfiler.VehicleProfile.UNKNOWN
    private var currentEnergyProfile: EnergyProfile? = null

    init {
        screenScope.launch {
            vehicleDataManager.vehicleProfile.collectLatest { profile ->
                currentVehicleProfile = profile
                invalidate()
            }
        }
        screenScope.launch {
            vehicleDataManager.energyProfile.collectLatest { energyProfile ->
                currentEnergyProfile = energyProfile
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val hostInfo = carContext.hostInfo
        val rows = mutableListOf<Row>()

        rows.add(Row.Builder().setTitle("Platform").addText("Android Auto (Projected)").build())
        rows.add(Row.Builder().setTitle("Host Package").addText(hostInfo?.packageName ?: "N/A").build())
        rows.add(Row.Builder().setTitle("Host Version").addText(hostInfo?.hostVersion ?: "N/A").build())
        rows.add(Row.Builder().setTitle("Detected Profile").addText(currentVehicleProfile.name).build())

        currentEnergyProfile?.let {
            rows.add(Row.Builder().setTitle("Energy Profile").build())
            rows.add(Row.Builder().setTitle("  Fuel Types").addText(it.fuelTypes.value?.joinToString() ?: "N/A").build())
            rows.add(Row.Builder().setTitle("  EV Connector Types").addText(it.evConnectorTypes.value?.joinToString() ?: "N/A").build())
        }

        return PaneTemplate.Builder(
            Pane.Builder()
                .addRows(rows)
                .build()
        )
            .setTitle("Diagnostics")
            .build()
    }
}

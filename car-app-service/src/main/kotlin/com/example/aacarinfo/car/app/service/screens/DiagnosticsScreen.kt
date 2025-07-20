
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
import kotlinx.coroutines.flow.collect
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

    init {
        screenScope.launch {
            // Observe changes in vehicle profile if needed, or just fetch once
            currentVehicleProfile = vehicleDataManager.getCurrentVehicleProfile()
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val hostInfo = carContext.hostInfo

        return PaneTemplate.Builder(
            Pane.Builder()
                .addRow(Row.Builder().setTitle("Platform").addText("Android Auto (Projected)").build())
                .addRow(Row.Builder().setTitle("Host Package").addText(hostInfo?.packageName ?: "N/A").build())
                .addRow(Row.Builder().setTitle("Host Version").addText(hostInfo?.hostVersion ?: "N/A").build())
                .addRow(Row.Builder().setTitle("Detected Profile").addText(currentVehicleProfile.name).build())
                // TODO: Add dynamic list of data listener statuses and raw energy profile data
                .build()
        )
            .setTitle("Diagnostics")
            .build()
    }
}

package com.example.aacarinfo.vehicle.data.layer

import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.CarSensors
import com.example.aacarinfo.common.data.UnifiedVehicleDataModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Manages access to vehicle hardware data through the Android for Cars App Library.
 * This class is the single source of truth for all vehicle data, exposing it via a [StateFlow].
 */
class VehicleDataManager(
    private val carContext: CarContext
) {

    private val carHardwareManager: CarHardwareManager = carContext.getCarService(CarHardwareManager::class.java)
    private val carInfo: CarInfo = carHardwareManager.carInfo
    private val carSensors: CarSensors = carHardwareManager.carSensors

    private val _uvdm = MutableStateFlow(UnifiedVehicleDataModel())
    val uvdm: StateFlow<UnifiedVehicleDataModel> = _uvdm

    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val vehicleProfiler: VehicleProfiler = VehicleProfiler(carInfo, executor)

    init {
        // Fetch energy profile to infer vehicle type
        vehicleProfiler.fetchAndInferVehicleProfile { profile ->
            // We can update UVDM with profile info if needed, or just use it internally
            // For now, the profiler manages its own state.
        }

        // Add listeners for various car data types as per SPEC.md Table 1
        addEnergyLevelListener()
        addEvStatusListener()
        addSpeedListener()
        addMileageListener()
        fetchModel() // Model is fetched, not listened to continuously
    }

    /**
     * Updates the [UnifiedVehicleDataModel] with new data and emits it through the [uvdm] StateFlow.
     */
    private fun updateUVDM(update: (UnifiedVehicleDataModel) -> UnifiedVehicleDataModel) {
        _uvdm.value = update(_uvdm.value)
    }

    private fun addEnergyLevelListener() {
        carInfo.addEnergyLevelListener(executor) { energyLevel ->
            updateUVDM { currentUVDM ->
                currentUVDM.copy(
                    powertrainState = currentUVDM.powertrainState.copy(
                        stateOfChargePercent = energyLevel.batteryPercent?.value,
                        fuelLevelPercent = energyLevel.fuelPercent?.value as? Float, // Explicit cast
                        remainingRangeMeters = energyLevel.rangeMeters?.value
                    )
                )
            }
        }
    }

    private fun addEvStatusListener() {
        carInfo.addEvStatusListener(executor) { evStatus ->
            updateUVDM { currentUVDM ->
                currentUVDM.copy(
                    chargingState = currentUVDM.chargingState.copy(
                        isPortConnected = evStatus.chargingPortConnected?.value,
                        isPortOpen = evStatus.chargingPortOpen?.value
                    )
                )
            }
        }
    }

    private fun addSpeedListener() {
        carInfo.addSpeedListener(executor) { speed ->
            updateUVDM { currentUVDM ->
                currentUVDM.copy(
                    drivingDynamics = currentUVDM.drivingDynamics.copy(
                        speedMetersPerSecond = speed.displaySpeed?.value
                    )
                )
            }
        }
    }

    private fun addMileageListener() {
        carInfo.addMileageListener(executor) { mileage ->
            updateUVDM { currentUVDM ->
                currentUVDM.copy(
                    vehicleInfo = currentUVDM.vehicleInfo.copy(
                        odometerMeters = mileage.odometerMeters?.value
                    )
                )
            }
        }
    }

    private fun fetchModel() {
        carInfo.fetchModel(executor) { model ->
            updateUVDM { currentUVDM ->
                currentUVDM.copy(
                    vehicleInfo = currentUVDM.vehicleInfo.copy(
                        make = model.make?.value,
                        model = model.model?.value,
                        year = model.year?.value
                    )
                )
            }
        }
    }

    /**
     * Returns the current inferred vehicle profile from the [VehicleProfiler].
     */
    fun getCurrentVehicleProfile(): VehicleProfiler.VehicleProfile {
        return vehicleProfiler.getCurrentVehicleProfile()
    }
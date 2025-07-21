package com.example.aacarinfo.vehicle.data.layer

import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.CarSensors
import androidx.car.app.hardware.info.EnergyProfile
import com.example.aacarinfo.common.data.ChargingState
import com.example.aacarinfo.common.data.DrivingDynamics
import com.example.aacarinfo.common.data.PowertrainState
import com.example.aacarinfo.common.data.VehicleInfo
import com.example.aacarinfo.common.data.ListenerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

class VehicleDataManager(private val carContext: CarContext, private val executor: Executor) {

    private val carHardwareManager: CarHardwareManager = carContext.getCarService(CarHardwareManager::class.java)
    private val carInfo: CarInfo = carHardwareManager.carInfo
    private val carSensors: CarSensors = carHardwareManager.carSensors
    private val vehicleProfiler: VehicleProfiler = VehicleProfiler(carInfo, executor)

    private val _powertrainState = MutableStateFlow(PowertrainState(null, null, null))
    val powertrainState: StateFlow<PowertrainState> = _powertrainState.asStateFlow()

    private val _vehicleInfo = MutableStateFlow(VehicleInfo(null, null, null, null))
    val vehicleInfo: StateFlow<VehicleInfo> = _vehicleInfo.asStateFlow()

    private val _chargingState = MutableStateFlow(ChargingState(null, null))
    val chargingState: StateFlow<ChargingState> = _chargingState.asStateFlow()

    private val _drivingDynamics = MutableStateFlow(DrivingDynamics(null))
    val drivingDynamics: StateFlow<DrivingDynamics> = _drivingDynamics.asStateFlow()

    private val _vehicleProfile = MutableStateFlow(VehicleProfiler.VehicleProfile.UNKNOWN)
    val vehicleProfile: StateFlow<VehicleProfiler.VehicleProfile> = _vehicleProfile.asStateFlow()

    private val _rawEnergyProfile = MutableStateFlow("")
    val rawEnergyProfile: StateFlow<String> = _rawEnergyProfile.asStateFlow()

    private val _energyProfile = MutableStateFlow<EnergyProfile?>(null)
    val energyProfile: StateFlow<EnergyProfile?> = _energyProfile.asStateFlow()

    // Listener Statuses
    private val _energyLevelListenerStatus = MutableStateFlow(ListenerStatus("EnergyLevel", false))
    val energyLevelListenerStatus: StateFlow<ListenerStatus> = _energyLevelListenerStatus.asStateFlow()

    private val _evStatusListenerStatus = MutableStateFlow(ListenerStatus("EvStatus", false))
    val evStatusListenerStatus: StateFlow<ListenerStatus> = _evStatusListenerStatus.asStateFlow()

    private val _speedListenerStatus = MutableStateFlow(ListenerStatus("Speed", false))
    val speedListenerStatus: StateFlow<ListenerStatus> = _speedListenerStatus.asStateFlow()

    private val _mileageListenerStatus = MutableStateFlow(ListenerStatus("Mileage", false))
    val mileageListenerStatus: StateFlow<ListenerStatus> = _mileageListenerStatus.asStateFlow()

    private val _modelFetchStatus = MutableStateFlow(ListenerStatus("ModelFetch", false))
    val modelFetchStatus: StateFlow<ListenerStatus> = _modelFetchStatus.asStateFlow()

    private val _energyProfileFetchStatus = MutableStateFlow(ListenerStatus("EnergyProfileFetch", false))
    val energyProfileFetchStatus: StateFlow<ListenerStatus> = _energyProfileFetchStatus.asStateFlow()

    init {
        carInfo.addEnergyLevelListener(executor) { energyLevel ->
            _powertrainState.value = PowertrainState(
                stateOfChargePercent = energyLevel.batteryPercent?.value?.toInt(),
                fuelLevelPercent = energyLevel.fuelPercent?.value,
                remainingRangeMeters = energyLevel.rangeRemainingMeters?.value
            )
            _energyLevelListenerStatus.value = ListenerStatus(
                name = "EnergyLevel",
                isActive = true,
                lastUpdated = System.currentTimeMillis(),
                availability = when (energyLevel.batteryPercent?.status) {
                    CarValue.STATUS_SUCCESS -> "Available"
                    CarValue.STATUS_UNAVAILABLE -> "Unavailable"
                    CarValue.STATUS_UNIMPLEMENTED -> "Unimplemented"
                    else -> "Unknown"
                }
            )
        }

        carInfo.addEvStatusListener(executor) { evStatus ->
            _chargingState.value = ChargingState(
                isPortConnected = evStatus.evChargePortConnected?.value,
                isPortOpen = evStatus.evChargePortOpen?.value
            )
            _evStatusListenerStatus.value = ListenerStatus(
                name = "EvStatus",
                isActive = true,
                lastUpdated = System.currentTimeMillis(),
                availability = when (evStatus.evChargePortConnected?.status) {
                    CarValue.STATUS_SUCCESS -> "Available"
                    CarValue.STATUS_UNAVAILABLE -> "Unavailable"
                    CarValue.STATUS_UNIMPLEMENTED -> "Unimplemented"
                    else -> "Unknown"
                }
            )
        }

        carInfo.addSpeedListener(executor) { speed ->
            _drivingDynamics.value = DrivingDynamics(
                speedMetersPerSecond = speed.rawSpeedMetersPerSecond?.value
            )
            _speedListenerStatus.value = ListenerStatus(
                name = "Speed",
                isActive = true,
                lastUpdated = System.currentTimeMillis(),
                availability = when (speed.rawSpeedMetersPerSecond?.status) {
                    CarValue.STATUS_SUCCESS -> "Available"
                    CarValue.STATUS_UNAVAILABLE -> "Unavailable"
                    CarValue.STATUS_UNIMPLEMENTED -> "Unimplemented"
                    else -> "Unknown"
                }
            )
        }

        carInfo.addMileageListener(executor) { mileage ->
            _vehicleInfo.value = _vehicleInfo.value.copy(
                odometerMeters = mileage.odometerMeters?.value
            )
            _mileageListenerStatus.value = ListenerStatus(
                name = "Mileage",
                isActive = true,
                lastUpdated = System.currentTimeMillis(),
                availability = when (mileage.odometerMeters?.status) {
                    CarValue.STATUS_SUCCESS -> "Available"
                    CarValue.STATUS_UNAVAILABLE -> "Unavailable"
                    CarValue.STATUS_UNIMPLEMENTED -> "Unimplemented"
                    else -> "Unknown"
                }
            )
        }

        carInfo.fetchModel(executor) { model ->
            _vehicleInfo.value = _vehicleInfo.value.copy(
                make = model.manufacturer.value,
                model = model.name.value,
                year = model.year.value
            )
            _modelFetchStatus.value = ListenerStatus(
                name = "ModelFetch",
                isActive = true,
                lastUpdated = System.currentTimeMillis(),
                availability = when (model.manufacturer?.status) {
                    CarValue.STATUS_SUCCESS -> "Available"
                    CarValue.STATUS_UNAVAILABLE -> "Unavailable"
                    CarValue.STATUS_UNIMPLEMENTED -> "Unimplemented"
                    else -> "Unknown"
                }
            )
        }

        vehicleProfiler.fetchAndInferVehicleProfile { profile, energyProfile ->
            _vehicleProfile.value = profile
            _energyProfile.value = energyProfile
            _rawEnergyProfile.value = "Fuel: ${energyProfile.fuelTypes.value?.joinToString()}, EV: ${energyProfile.evConnectorTypes.value?.joinToString()}"
            _energyProfileFetchStatus.value = ListenerStatus(
                name = "EnergyProfileFetch",
                isActive = true,
                lastUpdated = System.currentTimeMillis(),
                availability = when (energyProfile.fuelTypes?.status) {
                    CarValue.STATUS_SUCCESS -> "Available"
                    CarValue.STATUS_UNAVAILABLE -> "Unavailable"
                    CarValue.STATUS_UNIMPLEMENTED -> "Unimplemented"
                    else -> "Unknown"
                }
            )
        }
    }
}

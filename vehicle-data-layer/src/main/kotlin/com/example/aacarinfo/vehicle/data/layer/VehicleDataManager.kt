package com.example.aacarinfo.vehicle.data.layer

import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.CarSensors
import androidx.car.app.hardware.info.EnergyProfile
import com.example.aacarinfo.common.data.ChargingState
import com.example.aacarinfo.common.data.DrivingDynamics
import com.example.aacarinfo.common.data.PowertrainState
import com.example.aacarinfo.common.data.VehicleInfo
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

    private val _energyProfile = MutableStateFlow<EnergyProfile?>(null)
    val energyProfile: StateFlow<EnergyProfile?> = _energyProfile.asStateFlow()

    init {
        carInfo.addEnergyLevelListener(executor) { energyLevel ->
            _powertrainState.value = PowertrainState(
                stateOfChargePercent = energyLevel.batteryPercent?.value?.toInt(),
                fuelLevelPercent = energyLevel.fuelPercent?.value,
                remainingRangeMeters = energyLevel.rangeRemainingMeters?.value
            )
        }

        carInfo.addEvStatusListener(executor) { evStatus ->
            _chargingState.value = ChargingState(
                isPortConnected = evStatus.evChargePortConnected?.value,
                isPortOpen = evStatus.evChargePortOpen?.value
            )
        }

        carInfo.addSpeedListener(executor) { speed ->
            _drivingDynamics.value = DrivingDynamics(
                speedMetersPerSecond = speed.rawSpeedMetersPerSecond?.value
            )
        }

        carInfo.addMileageListener(executor) { mileage ->
            _vehicleInfo.value = _vehicleInfo.value.copy(
                odometerMeters = mileage.odometerMeters?.value
            )
        }

        carInfo.fetchModel(executor) { model ->
            _vehicleInfo.value = _vehicleInfo.value.copy(
                make = model.make,
                model = model.model,
                year = model.year
            )
        }

        vehicleProfiler.fetchAndInferVehicleProfile { profile, energyProfile ->
            _vehicleProfile.value = profile
            _energyProfile.value = energyProfile
        }
    }
}

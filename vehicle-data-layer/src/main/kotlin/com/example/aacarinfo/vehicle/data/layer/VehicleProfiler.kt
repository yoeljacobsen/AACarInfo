
package com.example.aacarinfo.vehicle.data.layer

import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.EnergyProfile
import java.util.concurrent.Executor

/**
 * Infers the vehicle type (EV, PHEV, ICE) based on the [EnergyProfile] received from the car.
 */
class VehicleProfiler(
    private val carInfo: CarInfo,
    private val executor: Executor
) {

    /**
     * Represents the inferred vehicle profile.
     */
    enum class VehicleProfile {
        EV,
        PHEV,
        ICE,
        UNKNOWN
    }

    private var currentVehicleProfile: VehicleProfile = VehicleProfile.UNKNOWN

    /**
     * Fetches the energy profile and infers the vehicle type.
     * This method should be called upon initialization of the data layer.
     */
    fun fetchAndInferVehicleProfile(callback: (VehicleProfile, EnergyProfile) -> Unit) {
        carInfo.fetchEnergyProfile(executor) { energyProfile ->
            currentVehicleProfile = inferVehicleProfile(energyProfile)
            callback(currentVehicleProfile, energyProfile)
        }
    }

    private fun inferVehicleProfile(energyProfile: EnergyProfile): VehicleProfile {
        val fuelTypes = energyProfile.fuelTypes.value ?: emptyList()
        val evConnectorTypes = energyProfile.evConnectorTypes.value ?: emptyList()

        val hasElectricFuel = fuelTypes.any { it == EnergyProfile.FUEL_TYPE_ELECTRIC }
        val hasGasolineFuel = fuelTypes.any {
            it == EnergyProfile.FUEL_TYPE_UNLEADED ||
            it == EnergyProfile.FUEL_TYPE_DIESEL_1 ||
            it == EnergyProfile.FUEL_TYPE_DIESEL_2
        }
        val hasEvConnectors = evConnectorTypes.isNotEmpty()

        return when {
            hasElectricFuel && !hasGasolineFuel && hasEvConnectors -> VehicleProfile.EV
            hasElectricFuel && hasGasolineFuel && hasEvConnectors -> VehicleProfile.PHEV
            !hasElectricFuel && hasGasolineFuel && !hasEvConnectors -> VehicleProfile.ICE
            else -> VehicleProfile.UNKNOWN
        }
    }

    fun getCurrentVehicleProfile(): VehicleProfile = currentVehicleProfile
}

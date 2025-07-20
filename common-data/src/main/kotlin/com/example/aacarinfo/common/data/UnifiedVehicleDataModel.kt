package com.example.aacarinfo.common.data

/**
 * Represents the powertrain state of the vehicle, including battery, fuel, and range information.
 * All properties are nullable to gracefully handle cases where a vehicle does not provide specific data.
 */
data class PowertrainState(
    val stateOfChargePercent: Int? = null,
    val fuelLevelPercent: Float? = null,
    val remainingRangeMeters: Float? = null
)

/**
 * Represents general vehicle information such as make, model, year, and odometer.
 * All properties are nullable to gracefully handle cases where a vehicle does not provide specific data.
 */
data class VehicleInfo(
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val odometerMeters: Float? = null
)

/**
 * Represents the charging state of an electric vehicle, including port connection and open status.
 * All properties are nullable to gracefully handle cases where a vehicle does not provide specific data.
 */
data class ChargingState(
    val isPortConnected: Boolean? = null,
    val isPortOpen: Boolean? = null
)

/**
 * Represents the driving dynamics of the vehicle, such as current speed.
 * All properties are nullable to gracefully handle cases where a vehicle does not provide specific data.
 */
data class DrivingDynamics(
    val speedMetersPerSecond: Float? = null
)

/**
 * Represents the comprehensive Unified Vehicle Data Model (UVDM).
 * This data class aggregates all available vehicle information, with each component being nullable
 * to accommodate varying data availability across different vehicles.
 */
data class UnifiedVehicleDataModel(
    val powertrainState: PowertrainState = PowertrainState(),
    val vehicleInfo: VehicleInfo = VehicleInfo(),
    val chargingState: ChargingState = ChargingState(),
    val drivingDynamics: DrivingDynamics = DrivingDynamics()
)
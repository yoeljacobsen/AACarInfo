package com.example.aacarinfo.common.data

data class PowertrainState(
    val stateOfChargePercent: Int?,
    val fuelLevelPercent: Float?,
    val remainingRangeMeters: Float?
)

data class VehicleInfo(
    val make: String?,
    val model: String?,
    val year: Int?,
    val odometerMeters: Float?
)

data class ChargingState(
    val isPortConnected: Boolean?,
    val isPortOpen: Boolean?
)

data class DrivingDynamics(
    val speedMetersPerSecond: Float?
)

package com.example.aacarinfo.common.data

data class ListenerStatus(
    val name: String,
    val isActive: Boolean,
    val lastUpdated: Long? = null,
    val availability: String = "Unknown"
)

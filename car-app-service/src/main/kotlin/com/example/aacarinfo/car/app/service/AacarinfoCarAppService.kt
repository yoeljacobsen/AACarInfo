
package com.example.aacarinfo.car.app.service

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * Entry point for the aacarinfo Android Auto application.
 * This service manages the lifecycle of the car app and provides a [HostValidator] for security.
 */
class AacarinfoCarAppService : CarAppService() {

    override fun onCreateSession(): Session {
        return AacarinfoSession()
    }

    override fun createHostValidator(): HostValidator {
        // As per SPEC.md Section 5.2, implement host validation.
        // This example allows all hosts for simplicity during initial development.
        // In a production app, this should be restricted to trusted hosts.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }
}

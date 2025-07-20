
package com.example.aacarinfo.car.app.service

import androidx.car.app.Session
import androidx.car.app.Screen
import com.example.aacarinfo.car.app.service.screens.MainScreen

/**
 * Manages the lifecycle of the car app's UI and navigation stack.
 */
class AacarinfoSession : Session() {

    override fun onCreateScreen(intent: Intent): Screen {
        // The initial screen of the application.
        return MainScreen(carContext)
    }
}

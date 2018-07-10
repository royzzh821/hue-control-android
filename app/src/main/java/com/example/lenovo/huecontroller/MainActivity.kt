package com.example.lenovo.huecontroller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
  }

  /**
   * This method will be called (on the main thread for the app) when a
   * Bluetooth message is received.
   */
  fun receiveMessage(message: String) {
    // TODO
  }

  /**
   * Call this method to send a Bluetooth message to the light.
   */
  fun sendMessage(message: String) {
    // TODO
  }
}

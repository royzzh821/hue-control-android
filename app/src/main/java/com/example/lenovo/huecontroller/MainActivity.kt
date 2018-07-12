package com.example.lenovo.huecontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import java.io.IOException
import java.util.*

val BLUETOOTH_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b7541")

// Indicates that a message has ended. This character cannot appear in a message.
const val MESSAGE_SEPARATOR: Char = '|'

open class MainActivity : AppCompatActivity() {

  var bluetoothDevice: BluetoothDevice? = null
  private var bluetoothSocket: BluetoothSocket? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // We need Bluetooth.
    val bluetooth = BluetoothAdapter.getDefaultAdapter()
    if (bluetooth == null || !bluetooth.isEnabled) {
      startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0)
      return
    }

    // Since people don't always stay home, the Android device may disconnect from the Raspberry Pi.
    // This will try to connect (or reconnect) to the Raspberry Pi every three seconds.
    Timer().schedule(object : TimerTask() {
      override fun run() {
        try {
          getOrCreateBluetoothSocket()
        } catch (e: Exception) {
          Log.i("MainActivity", "Periodic connect failed", e)
        }
      }
    }, 0, 3000)

    sendMessage("?") // This will grab the current status of the light.
    val onbutton = findViewById<View>(R.id.HueOn) as Button
    onbutton.setOnClickListener {
      // Perform action on temp button click
      sendMessage("1")
    }
    val offbutton = findViewById<View>(R.id.HueOff) as Button
    offbutton.setOnClickListener {
      // Perform action on temp button click
      sendMessage("0")
    }
  }

  override fun onDestroy() {
    bluetoothSocket?.close()
    super.onDestroy()
  }

  /**
   * This method will be called (on the main thread for the app) when a
   * Bluetooth message is received.
   */
  fun receiveMessage(message: String) {
    if (message.equals("0")) {
      val onbutton = findViewById<View>(R.id.HueOn) as Button
      onbutton.isEnabled = true
      val offbutton = findViewById<View>(R.id.HueOff) as Button
      offbutton.isEnabled = false
    }
    if (message.equals("1")) {
      val onbutton = findViewById<View>(R.id.HueOn) as Button
      onbutton.isEnabled = false
      val offbutton = findViewById<View>(R.id.HueOff) as Button
      offbutton.isEnabled = true
    }
  }

  /**
   * Call this method to send a Bluetooth message to the light.
   */
  open fun sendMessage(message: String) {
    Thread(fun() {
      synchronized(bluetoothSocketLock) {
        val currentSocket = getOrCreateBluetoothSocket()
        currentSocket.outputStream.write(("$message|").toByteArray())
        currentSocket.outputStream.flush()
      }
    }, "Bluetooth Sender [$message]").start()
  }

  private val bluetoothSocketLock = Object()

  fun getOrCreateBluetoothSocket(): BluetoothSocket {
    synchronized(bluetoothSocketLock) {
      var currentSocket = bluetoothSocket
      if (currentSocket != null) return currentSocket
      var currentDevice = bluetoothDevice
      if (currentDevice == null) {
        for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
          if (device.name == "JR") { // TODO probably not a good idea to hardcode the name
            bluetoothDevice = device
            currentDevice = device
            break
          }
        }
      }
      if (currentDevice == null) {
        throw IOException("Could not find the device")
      }
      // Connect to the device.
      currentSocket = currentDevice.createRfcommSocketToServiceRecord(BLUETOOTH_UUID)
      if (!currentSocket.isConnected) {
        currentSocket.connect()
      }

      // Start reading data from the socket.
      Thread(fun() {
        val read = StringBuilder()
        val buffer = ByteArray(32)
        while (true) {
          val bytesRead = currentSocket.inputStream.read(buffer)
          if (bytesRead < 0) {
            // Is it even possible to get -1 objects?
            // Android uses this to signal that the device disconnected.
            break
          }
          // Add all the data that came from the socket into "read".
          read.append(String(buffer, 0, bytesRead))

          while (read.contains(MESSAGE_SEPARATOR)) {
            // End of a message. Pull it out of the data.
            val message = read.substring(0, read.indexOf(MESSAGE_SEPARATOR))
            read.delete(0, read.indexOf(MESSAGE_SEPARATOR) + 1)

            receiveMessageAsync(message)
          }
        }
      }, "Bluetooth Socket Reader").start()

      // And save the socket for future use.
      bluetoothSocket = currentSocket
      return currentSocket
    }
  }

  open fun receiveMessageAsync(message: String) {
    runOnUiThread {
      receiveMessage(message)
    }
  }
}

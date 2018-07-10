package com.example.lenovo.huecontroller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import java.io.IOException
import java.util.*

// You can generate a unique value for yourself:
// 1. In the Android Studio menu bar, click Tools > Kotlin > Kotlin REPL
// 2. In the new window, type:
// import java.util.*
// UUID.randomUUID()
// 3. Click the Run button.
// 4. After a second, a unique value will appear. Copy that here.
val BLUETOOTH_UUID: UUID = UUID.fromString("dd88ab89-517f-489d-9129-05cf95300b15")

// Indicates that a message has ended. This character cannot appear in a message.
const val MESSAGE_SEPARATOR: Char = '|'

class MainActivity : AppCompatActivity() {

  private var bluetoothDevice: BluetoothDevice? = null
  private var bluetoothSocket: BluetoothSocket? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // We need Bluetooth.
    val bluetooth = BluetoothAdapter.getDefaultAdapter()
    if (!bluetooth.isEnabled) {
      startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0)
      return
    }

    // Since people don't always stay home, the Android device may disconnect from the Raspberry Pi.
    // This will try to connect (or reconnect) to the Raspberry Pi every three seconds.
    Timer().schedule(object : TimerTask() {
      override fun run() {
        getOrCreateBluetoothSocket()
      }
    }, 0, 3000)

    setContentView(R.layout.activity_main)
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
    // TODO
  }

  /**
   * Call this method to send a Bluetooth message to the light.
   */
  fun sendMessage(message: String) {
    Thread(fun() {
      synchronized(bluetoothSocketLock) {
        val currentSocket = getOrCreateBluetoothSocket()
        currentSocket.outputStream.write(("$message|").toByteArray())
        currentSocket.outputStream.flush()
      }
    }, "Bluetooth Sender [$message]").start()
  }

  private val bluetoothSocketLock = Object()

  private fun getOrCreateBluetoothSocket(): BluetoothSocket {
    synchronized(bluetoothSocketLock) {
      var currentSocket = bluetoothSocket
      if (currentSocket != null) return currentSocket
      if (bluetoothDevice == null) {
        for (device in BluetoothAdapter.getDefaultAdapter().bondedDevices) {
          if (device.name == "JR") { // TODO probably not a good idea
            bluetoothDevice = device

            // Connect to the device.
            currentSocket = device.createRfcommSocketToServiceRecord(BLUETOOTH_UUID)
            currentSocket.connect()

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
                  read.removeRange(0, read.indexOf(MESSAGE_SEPARATOR) + 1)

                  runOnUiThread {
                    receiveMessage(message)
                  }
                }
              }
            }, "Bluetooth Socket Reader").start()

            // And save the socket for future use.
            bluetoothSocket = currentSocket
          }
        }
      }
      if (currentSocket == null) {
        throw IOException("Could not connect to the device")
      }
      return currentSocket
    }
  }
}

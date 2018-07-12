package com.example.lenovo.huecontroller

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BluetoothUnitTest {
  @Test
  fun bluetoothSend() {
    val activity = MainActivity()
    val device: BluetoothDevice = mock(BluetoothDevice::class.java)
    val socket: BluetoothSocket = mock(BluetoothSocket::class.java)
    val stream = ByteArrayOutputStream()
    `when`(device.createRfcommSocketToServiceRecord(ArgumentMatchers.any())).thenReturn(socket)
    doNothing().`when`(socket).connect()
    `when`(socket.inputStream).thenReturn(ByteArrayInputStream(ByteArray(0)))
    `when`(socket.outputStream).thenReturn(BufferedOutputStream(stream))
    activity.bluetoothDevice = device
    activity.sendMessage("Hot Chocolate")
    Thread.sleep(100) // sendMessage() runs asynchronously, need to wait for it
    assertEquals("Hot Chocolate|", stream.toString("US-ASCII"))
  }

  @Test
  fun bluetoothReceive() {
    var received: String? = null
    val activity = object : MainActivity() {
      override fun receiveMessageAsync(message: String) {
        received = message
      }
    }
    val device: BluetoothDevice = mock(BluetoothDevice::class.java)
    val socket: BluetoothSocket = mock(BluetoothSocket::class.java)
    `when`(device.createRfcommSocketToServiceRecord(ArgumentMatchers.any())).thenReturn(socket)
    doNothing().`when`(socket).connect()
    `when`(socket.inputStream).thenReturn(ByteArrayInputStream("Hot Chocolate|".toByteArray(Charsets.US_ASCII)))
    activity.bluetoothDevice = device
    activity.getOrCreateBluetoothSocket()
    Thread.sleep(100) // receiveMessage() is called asynchronously, need to wait for it
    assertEquals("Hot Chocolate", received)
  }
}
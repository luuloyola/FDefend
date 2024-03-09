package com.example.fdefend

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

import java.io.IOException
import java.io.InputStream
import java.util.*

class BluetoothService(private val mmSocket: BluetoothSocket) {

    private var mmInputStream: InputStream? = null
    private var isRunning: Boolean = false

    fun startReading(listener: (String) -> Unit) {
        try {
            mmInputStream = mmSocket.inputStream
            isRunning = true

            val buffer = ByteArray(1024)
            var numBytes: Int

            // Loop para leer datos entrantes
            while (isRunning) {
                // Lee desde el InputStream
                numBytes = try {
                    mmInputStream?.read(buffer) ?: 0
                } catch (e: IOException) {
                    // Manejar la desconexión u otra excepción
                    e.printStackTrace()
                    isRunning = false
                    -1
                }

                // Si se han recibido datos, convierte los bytes a String y pasa el mensaje al listener
                if (numBytes > 0) {
                    val incomingMessage = String(buffer, 0, numBytes)
                    listener(incomingMessage)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopReading() {
        isRunning = false
        try {
            mmInputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


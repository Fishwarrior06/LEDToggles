package com.fishwarrior06.ledtoggle

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.core.content.edit
import java.util.UUID

@SuppressLint("MissingPermission") // Previously granted in MainActivity
class LedTileService : TileService() {

    companion object {
        private const val TAG = "LedTileService"
        private const val PREFS_NAME = "LedTogglePrefs"
        private const val KEY_LED_STATE = "led_on_state"

        // Hardcoded physical MAC address
        private const val DEVICE_MAC = "XX:XX:XX:XX:XX:XX"

        // Protocol UUIDs extracted using JADX
        private val SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val CHAR_UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    }

    // Raw byte commands for ON, OFF, and the exact orange color (R:255 G:40 B:0)
    private val commandOn = byteArrayOf(0x7E, 0x04, 0x04, 0x01, 0x00, 0x01, 0xFF.toByte(), 0x00, 0xEF.toByte())
    private val commandOff = byteArrayOf(0x7E, 0x04, 0x04, 0x00, 0x00, 0x00, 0xFF.toByte(), 0x00, 0xEF.toByte())
    private val commandColor = byteArrayOf(0x7E, 0x07, 0x05, 0x03, 0xFF.toByte(), 0x28, 0x00, 0x00, 0xEF.toByte())

    // Triggered every time the Quick Settings panel is expanded to render the tile
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.label = "LED Lights"

        // Retrieve the last saved state from local persistent storage
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isLedCurrentlyOn = sharedPrefs.getBoolean(KEY_LED_STATE, false)

        // Sync the tile's visual state with the actual hardware state
        if (isLedCurrentlyOn) {
            tile.state = Tile.STATE_ACTIVE    // Active
        } else {
            tile.state = Tile.STATE_INACTIVE  // Inactive
        }
        tile.updateTile()
    }

    // Click event handler for the Quick Settings tile
    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return
        val sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        if (tile.state == Tile.STATE_ACTIVE) {
            // If active, the user wants to turn it off: send off command
            sendBlePackets(commandOff)
            tile.state = Tile.STATE_INACTIVE

            // Persist the updated state as OFF
            sharedPrefs.edit { putBoolean(KEY_LED_STATE, false) }
        } else {
            // If inactive, the user wants to turn it on: send on + color commands
            sendBlePackets(commandOn, commandColor)
            tile.state = Tile.STATE_ACTIVE

            // Persist the updated state as ON
            sharedPrefs.edit { putBoolean(KEY_LED_STATE, true) }
        }
        tile.updateTile()
    }

    private fun sendBlePackets(vararg packets: ByteArray) {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: return

        try {
            val device = adapter.getRemoteDevice(DEVICE_MAC)

            // OPTIMIZATION: Force TRANSPORT_LE for rapid connection routing in Android 16
            device.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Instant connection established. Discovering services...")
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Disconnected from LED safely.")
                        gatt.close()
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val service = gatt.getService(SERVICE_UUID)
                        val characteristic = service?.getCharacteristic(CHAR_UUID)

                        if (characteristic != null) {
                            Thread {
                                for ((index, packet) in packets.withIndex()) {
                                    // Clean and native characteristic write optimized for modern APIs
                                    gatt.writeCharacteristic(
                                        characteristic,
                                        packet,
                                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                    )

                                    // SPEED OPTIMIZATION:
                                    // If it's a single packet (turn off), bypass delays.
                                    // If multiple packets (turn on + color), drop the sequence delay to 30ms.
                                    if (packets.size > 1 && index < packets.size - 1) {
                                        Thread.sleep(30)
                                    }
                                }
                                gatt.disconnect() // Release the Bluetooth antenna immediately
                            }.start()
                        } else {
                            Log.e(TAG, "Error: Write characteristic fff3 not found.")
                            gatt.disconnect()
                        }
                    } else {
                        gatt.disconnect()
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE) // Low Energy hardware-forced link
        } catch (_: IllegalArgumentException) {
            Log.e(TAG, "Error with the provided MAC address: $DEVICE_MAC")
        }
    }
}
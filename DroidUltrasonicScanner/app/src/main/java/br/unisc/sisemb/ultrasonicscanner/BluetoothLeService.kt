package br.unisc.sisemb.ultrasonicscanner

import android.app.Service
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.os.IBinder
import android.content.Intent
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Binder
import android.util.Log
import java.util.*


/**
 * Created by dieg0 on 11/06/2017.
 */
class BluetoothLeService : Service() {
    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mBluetoothDeviceAddress: String? = null
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mConnectionState = STATE_DISCONNECTED
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private val mGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val intentAction: String
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED
                mConnectionState = STATE_CONNECTED
                broadcastUpdate(intentAction)
                Log.i(TAG, "Connected to GATT server.")
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt!!.discoverServices())
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED
                mConnectionState = STATE_DISCONNECTED
                Log.i(TAG, "Disconnected from GATT server.")
                broadcastUpdate(intentAction)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic,
                                          status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }

    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    private fun broadcastUpdate(action: String,
                                characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        val data = characteristic.value
        if (data != null && data.size > 0) {
            val stringBuilder = StringBuilder(data.size)
            var btArr: ByteArray = ByteArray(data.size)
            for ((i, byteChar) in data.withIndex()) {
                btArr!![i] = byteChar
                stringBuilder.append(String.format("%d ", byteChar))
            }
            Log.d("BluetoothLeServiceReceived", String(data) + "\n" + stringBuilder.toString())
            intent.putExtra(EXTRA_DATA, btArr)
        }
        sendBroadcast(intent)
    }

    inner class LocalBinder : Binder() {
        internal val service: BluetoothLeService
            get() = this@BluetoothLeService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent): Boolean {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close()
        return super.onUnbind(intent)
    }

    private val mBinder = LocalBinder()
    /**
     * Initializes a reference to the local Bluetooth adapter.

     * @return Return true if the initialization is successful.
     */
    fun initialize(): Boolean {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.")
                return false
            }
        }
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.

     * @param address The device address of the destination device.
     * *
     * *
     * @return Return true if the connection is initiated successfully. The connection result
     * *         is reported asynchronously through the
     * *         `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * *         callback.
     */
    fun connect(address: String?): Boolean {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.")
            return false
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address == mBluetoothDeviceAddress
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.")
            if (mBluetoothGatt!!.connect()) {
                mConnectionState = STATE_CONNECTING
                return true
            } else {
                return false
            }
        }
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.")
            return false
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        Log.d(TAG, "Trying to create a new connection.")
        mBluetoothDeviceAddress = address
        mConnectionState = STATE_CONNECTING
        return true
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * `BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)`
     * callback.
     */
    fun disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }

    /**
     * Request a read on a given `BluetoothGattCharacteristic`. The read result is reported
     * asynchronously through the `BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)`
     * callback.

     * @param characteristic The characteristic to read from.
     */
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.readCharacteristic(characteristic)
    }

    /**
     * Enables or disables notification on a give characteristic.

     * @param characteristic Characteristic to act on.
     * *
     * @param enabled If true, enable notification.  False otherwise.
     */
    fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic,
                                      enabled: Boolean) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.setCharacteristicNotification(characteristic, enabled)
        if (UUID_SCANNER_SENSOR.equals(characteristic.uuid)) {
            val descriptor = characteristic.getDescriptor(
                    UUID.fromString(CurieBleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            mBluetoothGatt!!.writeDescriptor(descriptor)
        }
    }


    fun readCustomCharacteristic() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("BluetoothLeServiceError", "BluetoothAdapter not initialized")
            return
        }
        val mCustomService = mBluetoothGatt!!.getService(UUID.fromString(CurieBleGattAttributes.SCANNER_SENSOR_SERVICE))
        if (mCustomService == null) {
            Log.w("BluetoothLeServiceError", "Custom BLE Service not found")
            return
        }
        val mReadCharacteristic = mCustomService!!.getCharacteristic(UUID.fromString(CurieBleGattAttributes.SCANNER_SENSOR_CHARACTERISTIC))
        if (mBluetoothGatt!!.readCharacteristic(mReadCharacteristic) === false) {
            Log.w("BluetoothLeServiceError", "Failed to read characteristic")
        }
    }

    fun writeCustomCharacteristic(value: ByteArray) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("BluetoothLeServiceError", "BluetoothAdapter not initialized")
            return
        }
        val mCustomService = mBluetoothGatt!!.getService(UUID.fromString(CurieBleGattAttributes.SCANNER_SENSOR_SERVICE))
        if (mCustomService == null) {
            Log.w("BluetoothLeServiceError", "Custom BLE Service not found")
            return
        }
        val mWriteCharacteristic = mCustomService!!.getCharacteristic(UUID.fromString(CurieBleGattAttributes.SCANNER_SENSOR_CHARACTERISTIC))
        mWriteCharacteristic.setValue(value)
        if (mBluetoothGatt!!.writeCharacteristic(mWriteCharacteristic) === false) {
            Log.w("BluetoothLeServiceError", "Failed to write characteristic")
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after `BluetoothGatt#discoverServices()` completes successfully.

     * @return A `List` of supported services.
     */
    val supportedGattServices: List<BluetoothGattService>?
        get() {
            if (mBluetoothGatt == null) return null
            return mBluetoothGatt!!.services
        }

    companion object {
        private val TAG = BluetoothLeService::class.java.simpleName
        private val STATE_DISCONNECTED = 0
        private val STATE_CONNECTING = 1
        private val STATE_CONNECTED = 2
        val ACTION_GATT_CONNECTED = "br.unisc.sisemb.ultrasonicscanner.ACTION_GATT_CONNECTED"
        val ACTION_GATT_DISCONNECTED = "br.unisc.sisemb.ultrasonicscanner.ACTION_GATT_DISCONNECTED"
        val ACTION_GATT_SERVICES_DISCOVERED = "br.unisc.sisemb.ultrasonicscanner.ACTION_GATT_SERVICES_DISCOVERED"
        val ACTION_DATA_AVAILABLE = "br.unisc.sisemb.ultrasonicscanner.ACTION_DATA_AVAILABLE"
        val EXTRA_DATA = "br.unisc.sisemb.ultrasonicscanner.EXTRA_DATA"
        val UUID_SCANNER_SENSOR = UUID.fromString(CurieBleGattAttributes.SCANNER_SENSOR_CHARACTERISTIC)
    }
}
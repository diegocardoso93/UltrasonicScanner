package br.unisc.sisemb.ultrasonicscanner

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.IBinder
import android.content.*
import android.util.Log
import java.util.*
import android.content.ComponentName
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.ServiceConnection
import android.os.Handler


/**
 * Created by dieg0 on 11/06/2017.
 */
class DeviceControl {
    var mDeviceAddress: String? = null
    var mBluetoothLeService: BluetoothLeService? = null
    var mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
    var mConnected = false
    var mNotifyCharacteristic: BluetoothGattCharacteristic? = null

    // Code to manage Service lifecycle.
    val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                mConnected = true
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                writeInit()
                val handler = Handler()
                handler.postDelayed({
                    registerGattServices(mBluetoothLeService!!.supportedGattServices)
                }, 1000)
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                if (intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)!=null){
                    Log.d("data", intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA).toString())
                    var packageAlert: Intent = Intent("PACKAGE_RECEIVED")
                    packageAlert.putExtra("package", intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA))
                    context.sendBroadcast(packageAlert)
                }
            }
        }
    }

    private fun registerGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val unknownServiceString = ""
        val unknownCharaString = ""
        val gattServiceData = ArrayList<HashMap<String, String>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String>>>()
        mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()
        // Loops through available GATT Services.
        for (gattService in gattServices) {
            uuid = gattService.uuid.toString()
            if (uuid == CurieBleGattAttributes.SCANNER_SENSOR_SERVICE) {
                val currentServiceData = HashMap<String, String>()
                currentServiceData.put(
                        LIST_NAME, CurieBleGattAttributes.lookup(uuid, unknownServiceString))
                currentServiceData.put(LIST_UUID, uuid)
                gattServiceData.add(currentServiceData)
                val gattCharacteristicGroupData = ArrayList<HashMap<String, String>>()
                val gattCharacteristics = gattService.characteristics
                val charas = ArrayList<BluetoothGattCharacteristic>()
                // Loops through available Characteristics.
                for (gattCharacteristic in gattCharacteristics) {
                    charas.add(gattCharacteristic)
                    val currentCharaData = HashMap<String, String>()
                    uuid = gattCharacteristic.uuid.toString()
                    currentCharaData.put(
                            LIST_NAME, CurieBleGattAttributes.lookup(uuid, unknownCharaString))
                    currentCharaData.put(LIST_UUID, uuid)
                    gattCharacteristicGroupData.add(currentCharaData)
                }
                mGattCharacteristics!!.add(charas)
            }
        }

        if (mGattCharacteristics != null) {
            val characteristic = mGattCharacteristics!![0].get(0)
            val charaProp = characteristic.getProperties()
            if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService!!.setCharacteristicNotification(
                            mNotifyCharacteristic!!, false)
                    mNotifyCharacteristic = null
                }
                mBluetoothLeService!!.readCharacteristic(characteristic)
            }
            if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                mNotifyCharacteristic = characteristic
                mBluetoothLeService!!.setCharacteristicNotification(
                        characteristic, true)
            }
        }

    }

    companion object {
        private val TAG = DeviceControl::class.java.simpleName
        val LIST_NAME = "NAME"
        val LIST_UUID = "LIST_UUID"
    }

    fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    fun writeInit() {
        if (mBluetoothLeService != null){
            mBluetoothLeService!!.writeCustomCharacteristic(byteArrayOf(0x02,0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x01,0x00,0x02,0x04))
        }
    }

    fun writeBle(characteristicVal: ByteArray) {
        if (mBluetoothLeService != null) {
            mBluetoothLeService!!.writeCustomCharacteristic(characteristicVal)
        }
    }

    fun readBle() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService!!.readCustomCharacteristic()
        }
    }
}
package br.unisc.sisemb.ultrasonicscanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    val deviceControl = DeviceControl()
    var mScannerView: ScannerView? = null
    var packSend: ByteArray = byteArrayOf(0)

    // Sub-activities result values
    companion object {
        val RESULT_BLE_OK = 1
        val RESULT_SETTINGS_SELECTED = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val fabBluetooth = findViewById(R.id.bluetooth) as FloatingActionButton
        fabBluetooth.setOnClickListener { view ->
            if (deviceControl.mDeviceAddress != null) {
                unregisterReceiver(deviceControl.mGattUpdateReceiver)
                deviceControl.mDeviceAddress = null
                unbindService(deviceControl.mServiceConnection)
                deviceControl.mBluetoothLeService = null
            }
            var tvStatus: TextView = findViewById(R.id.tvStatus) as TextView
            tvStatus.text = "parado"
            val deviceScanIntent = Intent(this, DeviceScanActivity::class.java)
            startActivityForResult(deviceScanIntent, RESULT_BLE_OK)
        }

        val fabSettings = findViewById(R.id.config) as FloatingActionButton
        fabSettings.setOnClickListener { view ->
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(settingsIntent, RESULT_SETTINGS_SELECTED)
            packSend = byteArrayOf(0)
        }

        mScannerView = findViewById(R.id.scannerView) as ScannerView

        registerReceiver(broadcastReceiver, IntentFilter("PACKAGE_RECEIVED"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_BLE_OK) {
            Log.d("DEVICE_NAME", data!!.getStringExtra("DEVICE_NAME"))
            Log.d("DEVICE_ADDRESS", data!!.getStringExtra("DEVICE_ADDRESS"))

            deviceControl.mDeviceAddress = data!!.getStringExtra("DEVICE_ADDRESS")
            val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
            bindService(gattServiceIntent, deviceControl.mServiceConnection, Context.BIND_AUTO_CREATE)

            registerReceiver(deviceControl.mGattUpdateReceiver, deviceControl.makeGattUpdateIntentFilter())
            if (deviceControl.mBluetoothLeService != null) {
                val result = deviceControl.mBluetoothLeService!!.connect(deviceControl.mDeviceAddress)
                Log.d("MainActivity", "Connect request result = " + result)
            }
        } else if (resultCode == RESULT_SETTINGS_SELECTED) {
            if (data!!.hasExtra("REFRESH_RATE_SELECTED")) {
                packSend = MessageTemplates().REQ_SET_SCANNER_REFRESH_RATE_PACKAGE_TEMPLATE
                packSend[11] = data!!.getStringExtra("REFRESH_RATE_SELECTED").toByte()
                val crc = calculateChecksum(intArrayToPackage(byteArrayToIntArray(packSend)))
                packSend[12] = crc.toByte()
                deviceControl.writeBle(packSend)
            } else if (data!!.hasExtra("MAX_DISTANCE_SELECTED")) {
                val maxDistSelected = data!!.getStringExtra("MAX_DISTANCE_SELECTED").toInt()
                val maxDistByte0 = if (maxDistSelected > 255) 255 else maxDistSelected
                val maxDistByte1 = if (maxDistSelected > 255) maxDistSelected - 255 else 0
                packSend = MessageTemplates().REQ_SET_SCANNER_MAX_DISTANCE_TEMPLATE
                packSend[11] = maxDistByte0.toByte()
                packSend[12] = maxDistByte1.toByte()
                val crc = calculateChecksum(intArrayToPackage(byteArrayToIntArray(packSend)))
                packSend[13] = crc.toByte()
                deviceControl.writeBle(packSend)
            } else if (data!!.hasExtra("STOP_SELECTED")) {
                packSend = MessageTemplates().REQ_STOP_MESSAGES_TEMPLATE
                val crc = calculateChecksum(intArrayToPackage(byteArrayToIntArray(packSend)))
                packSend[11] = crc.toByte()
                deviceControl.writeBle(packSend)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(deviceControl.mServiceConnection)
        deviceControl.mBluetoothLeService = null
        unregisterReceiver(broadcastReceiver)
    }

    override fun onPause() {
        super.onPause()
        var tvStatus: TextView = findViewById(R.id.tvStatus) as TextView
        tvStatus.text = "parado"
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val arrayPackInt = byteArrayToIntArray(intent.getByteArrayExtra("package"))
            val pack: Package = intArrayToPackage(arrayPackInt)

            val checksum = calculateChecksum(pack)

            if (pack.payload[0] == Instructions.RESP_READ_SCANNER_SENSOR.ordinal && validatePackage(pack, checksum)) {
                mScannerView?.MAX_DISTANCE = getConfiguredMaxDistanceFromPack(pack).toFloat()
                mScannerView?.angle = getAngleFromPack(pack)
                val distance = getDistanceFromPack(pack)
                mScannerView?.distance = distance
                mScannerView?.insertGraphPoint(distance)
                mScannerView?.invalidate()
                packSend = if (packSend.size == 1) MessageTemplates().REQ_READ_SCANNER_SENSOR_PACKAGE_TEMPLATE else packSend
                updateLabels(arrayPackInt, "recebendo")

                val tvConfiguredRefreshRate = findViewById(R.id.tvConfiguredRefreshRate) as TextView
                val tvConfiguredMaxDistance = findViewById(R.id.tvConfiguredMaxDistance) as TextView
                tvConfiguredRefreshRate.text = "Taxa: " + getConfiguredRefreshRateFromPack(pack) + " pacotes/segundo"
                tvConfiguredMaxDistance.text = "Distância máx.: " + getConfiguredMaxDistanceFromPack(pack) + " cm"
            } else if (pack.payload[0] == Instructions.RESP_SET_SCANNER_REFRESH_RATE.ordinal && validatePackage(pack, checksum)) {
                updateLabels(arrayPackInt, "recebendo")
                Toast.makeText(context, "Taxa de atualização modificada com sucesso.", Toast.LENGTH_SHORT).show()
            } else if (pack.payload[0] == Instructions.RESP_SET_SCANNER_MAX_DISTANCE.ordinal && validatePackage(pack, checksum)){
                updateLabels(arrayPackInt, "recebendo")
                Toast.makeText(context, "Distância máxima modificada com sucesso.", Toast.LENGTH_SHORT).show()
            } else if (pack.payload[0] == Instructions.RESP_STOP_MESSAGES.ordinal && validatePackage(pack, checksum)) {
                updateLabels(arrayPackInt, "parado")
                Toast.makeText(context, "Envio de mensagens parado.", Toast.LENGTH_LONG).show()
            } else {
                Log.d("MainActivity", "Invalid package.");
            }
        }
    }

    internal fun updateLabels(arrayPackInt: IntArray, status: String) {
        var tvSended: TextView = findViewById(R.id.tvSended) as TextView
        val pacoteEnviado: String = "E\n" + getPackAsFormattedString(byteArrayToIntArray(packSend))
        tvSended.text = pacoteEnviado

        var tvReceived: TextView = findViewById(R.id.tvReceived) as TextView
        val pacoteRecebido: String = "R\n" + getPackAsFormattedString(arrayPackInt)
        tvReceived.text = pacoteRecebido

        var tvStatus: TextView = findViewById(R.id.tvStatus) as TextView
        tvStatus.text = status
    }

}

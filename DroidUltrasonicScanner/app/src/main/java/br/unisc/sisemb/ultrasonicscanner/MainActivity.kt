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
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    val deviceControl = DeviceControl()
    var mScannerView: ScannerView? = null

    companion object {
        val RESULT_BLE_OK = 1;
        val RESULT_SETTINGS_SELECTED = 2;
        val IOT = 2;
        val EOT = 4;

        val NOP = 0;
        val REQ_READ_SCANNER_SENSOR = 1;
        val REQ_STOP_MESSAGES = 2;
        val REQ_SET_SCANNER_REFRESH_RATE = 3;
        val REQ_SET_SCANNER_MAX_DISTANCE = 4;
        val RESP_READ_SCANNER_SENSOR = 5;
        val RESP_STOP_MESSAGES = 6;
        val RESP_SET_SCANNER_REFRESH_RATE = 7;
        val RESP_SET_SCANNER_MAX_DISTANCE = 8;

    }

    internal class Package(
        val iot: Int,
        val sk: IntArray,
        val pl: Int,
        val payload: IntArray,
        val crc: Int,
        val eot: Int) {
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
            val deviceScanIntent = Intent(this, DeviceScanActivity::class.java)
            startActivityForResult(deviceScanIntent, RESULT_BLE_OK)
            var tvStatus: TextView = findViewById(R.id.tvStatus) as TextView
            tvStatus.setText("Status: parado")
        }
        val fabSettings = findViewById(R.id.config) as FloatingActionButton
        fabSettings.setOnClickListener { view ->
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(settingsIntent, RESULT_SETTINGS_SELECTED)
        }
        mScannerView = findViewById(R.id.scannerView) as ScannerView

        registerReceiver(broadcastReceiver, IntentFilter("PACKAGE_RECEIVED"))
    }

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val arrayPackInt = byteArrayToIntArray(intent.getByteArrayExtra("package"))
            val pack: Package = intArrayToPackage(arrayPackInt)

            val checksum = calculateChecksum(pack)

            if (pack.payload[0] == RESP_READ_SCANNER_SENSOR && validatePackage(pack, checksum)) {
                mScannerView?.angle = getAngleFromPack(pack)
                mScannerView?.distance = getDistanceFromPack(pack)
                mScannerView?.invalidate()

                var tvSended: TextView = findViewById(R.id.tvSended) as TextView
                val pacoteEnviado: String = "Último Pacote enviado: \n" + getPackAsFormattedString(byteArrayToIntArray(deviceControl.INITIAL_PACKAGE))
                tvSended.setText(pacoteEnviado)

                var tvReceived: TextView = findViewById(R.id.tvReceived) as TextView
                val pacoteRecebido: String = "Último Pacote recebido:\n " + getPackAsFormattedString(arrayPackInt)
                tvReceived.setText( pacoteRecebido )

                var tvStatus: TextView = findViewById(R.id.tvStatus) as TextView
                tvStatus.setText("Status: recebendo")
            } else {
                Log.d("MainActivity", "Invalid package.");
            }
        }
    }

    fun byteArrayToIntArray(pack: ByteArray): IntArray {
        val packIntArray: IntArray = IntArray(pack.size)
        for ((i, byteChar) in pack.withIndex()) {
            if (byteChar < 0){
                val unsignedVal = 256 + byteChar.toInt()
                packIntArray.set(i, unsignedVal)
            }else{
                packIntArray.set(i, byteChar.toInt())
            }
        }
        return packIntArray
    }

    internal fun getAngleFromPack(p: Package): Float {
        return (p.payload[1] + p.payload[2] + 135).toFloat()
    }

    internal fun getDistanceFromPack(p: Package): Float {
        return (p.payload[3] + p.payload[4]).toFloat()
    }

    private fun intArrayToPackage(intPack: IntArray): Package {
        return Package(
            intPack[0],
            intPack.copyOfRange(1, 9),
            intPack[9],
            intPack.copyOfRange(10, 10 + intPack[9]),
            intPack[10 + intPack[9]],
            intPack[11 + intPack[9]]
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_BLE_OK) {
            Log.d("DEVICE_NAME", data?.getStringExtra("DEVICE_NAME"))
            Log.d("DEVICE_ADDRESS", data?.getStringExtra("DEVICE_ADDRESS"))

            deviceControl.mDeviceAddress = data?.getStringExtra("DEVICE_ADDRESS")
            val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
            bindService(gattServiceIntent, deviceControl.mServiceConnection, Context.BIND_AUTO_CREATE)

            registerReceiver(deviceControl.mGattUpdateReceiver, deviceControl.makeGattUpdateIntentFilter())
            if (deviceControl.mBluetoothLeService != null) {
                val result = deviceControl.mBluetoothLeService!!.connect(deviceControl.mDeviceAddress)
                Log.d("rds", "Connect request result=" + result)
            }
        } else if (resultCode == RESULT_SETTINGS_SELECTED) {

        }

    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(deviceControl.mServiceConnection)
        deviceControl.mBluetoothLeService = null
        unregisterReceiver(broadcastReceiver)
    }

    internal fun calculateChecksum(pack: Package): Int {
        var result: Int = pack.iot
        var i = 0
        while (i < 8) {
            result = result xor pack.sk[i]
            i++
        }
        result = result xor pack.pl
        i = 0
        while (i < pack.pl) {
            result = result xor pack.payload[i]
            i++
        }
        return result
    }

    internal fun getPackAsFormattedString(data: IntArray): String {

        val stringBuilder = StringBuilder(data.size)
        for ( inteiro in data) {
            stringBuilder.append(String.format("%d ", inteiro))
        }
        return stringBuilder.toString()

    }

    internal fun validatePackage(pack: Package, CRC: Int): Boolean {
        return pack.iot == IOT && pack.crc == CRC && pack.eot == EOT
    }
}

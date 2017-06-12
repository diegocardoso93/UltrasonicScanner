package br.unisc.sisemb.ultrasonicscanner

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.BaseAdapter
import android.widget.Toast
import android.content.pm.PackageManager
import android.widget.ListView


/**
 * Created by dieg0 on 10/06/2017.
 */
class DeviceScanActivity : AppCompatActivity() {

    companion object {
        private val RESULT_BLE_OK = 1
        private val REQUEST_ENABLE_BT = 2
        private val SCAN_PERIOD: Long = 10000
    }

    private var mLeDeviceListAdapter: LeDeviceListAdapter? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mScanning: Boolean = false
    private var mHandler: Handler? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_scan_activity)
        mHandler = Handler()

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()

        if (!mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            scanLeDevice(true)
        }

        mLeDeviceListAdapter = LeDeviceListAdapter()
        val listAdapter = findViewById(R.id.devicesListView) as ListView
        listAdapter.adapter = mLeDeviceListAdapter
        listAdapter.setOnItemClickListener({ parent, view, position, id ->
            val device = mLeDeviceListAdapter!!.getDevice(position)
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("DEVICE_NAME", device!!.name)
            intent.putExtra("DEVICE_ADDRESS", device!!.address)
            if (mScanning) {
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                mScanning = false
            }
            setResult(RESULT_BLE_OK, intent)
            finish()
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish()
            return
        }
        onResume()
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
        mLeDeviceListAdapter!!.clear()
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            mHandler!!.postDelayed({
                mScanning = false
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                invalidateOptionsMenu()
            }, SCAN_PERIOD)
            mScanning = true
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
        }
        invalidateOptionsMenu()
    }

    private inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice>
        private val mInflator: LayoutInflater

        init {
            mLeDevices = ArrayList<BluetoothDevice>()
            mInflator = this@DeviceScanActivity.layoutInflater
        }

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice? {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            var view = view
            val viewHolder: ViewHolder
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null)
                viewHolder = ViewHolder()
                viewHolder.deviceAddress = view!!.findViewById(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById(R.id.device_name) as TextView
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }
            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.length > 0)
                viewHolder.deviceName!!.text = deviceName
            else
                viewHolder.deviceName!!.setText("Unknown device")
            viewHolder.deviceAddress!!.text = device.address
            return view
        }
    }

    private val mLeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            mLeDeviceListAdapter!!.addDevice(device)
            mLeDeviceListAdapter!!.notifyDataSetChanged()
        }
    }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }
}
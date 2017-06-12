package br.unisc.sisemb.ultrasonicscanner

/**
 * Created by dieg0 on 11/06/2017.
 */
object CurieBleGattAttributes {
    private val attributes = HashMap<String, String>()
    val SCANNER_SENSOR_SERVICE = "19b10010-e8f2-537e-4f6c-d104768a1214"
    val SCANNER_SENSOR_CHARACTERISTIC = "19b10011-e8f2-537e-4f6c-d104768a1214"
    var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    init {
        // Service
        attributes.put(SCANNER_SENSOR_SERVICE, "Scanner Distance Sensor Service")
        // Characteristic
        attributes.put(SCANNER_SENSOR_CHARACTERISTIC, "Scanner Distance Sensor")
    }

    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes.get(uuid)
        return name ?: defaultName
    }
}
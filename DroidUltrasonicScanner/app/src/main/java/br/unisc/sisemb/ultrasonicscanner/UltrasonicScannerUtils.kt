package br.unisc.sisemb.ultrasonicscanner

/**
 * Created by dieg0 on 24/06/2017.
 */


enum class Instructions {
    NOP,
    REQ_READ_SCANNER_SENSOR,
    REQ_STOP_MESSAGES,
    REQ_SET_SCANNER_REFRESH_RATE,
    REQ_SET_SCANNER_MAX_DISTANCE,
    RESP_READ_SCANNER_SENSOR,
    RESP_STOP_MESSAGES,
    RESP_SET_SCANNER_REFRESH_RATE,
    RESP_SET_SCANNER_MAX_DISTANCE
}

data class Package(
    val iot: Int,
    val sk: IntArray,
    val pl: Int,
    val payload: IntArray,
    val crc: Int,
    val eot: Int
)

data class SecurityKey(
    val sk0: Byte = 0x00,
    val sk1: Byte = 0x01,
    val sk2: Byte = 0x02,
    val sk3: Byte = 0x03,
    val sk4: Byte = 0x04,
    val sk5: Byte = 0x05,
    val sk6: Byte = 0x06,
    val sk7: Byte = 0x07
)

data class TransmissionDelimitters(
    val IOT: Byte = 0x02,
    val EOT: Byte = 0x04
)

data class MessageTemplates(
    val sk: SecurityKey = SecurityKey(),
    val td: TransmissionDelimitters = TransmissionDelimitters(),
    val rsr: Byte = Instructions.REQ_SET_SCANNER_REFRESH_RATE.ordinal.toByte(),
    val rsd: Byte = Instructions.REQ_SET_SCANNER_MAX_DISTANCE.ordinal.toByte(),
    val d200: Byte = 0xC8.toByte(),
    val d200crc: Byte = 0xcd.toByte(),
    val rstop: Byte = Instructions.REQ_STOP_MESSAGES.ordinal.toByte()
) {
    val REQ_READ_SCANNER_SENSOR_PACKAGE_TEMPLATE: ByteArray = byteArrayOf(td.IOT, sk.sk0, sk.sk1, sk.sk2, sk.sk3, sk.sk4, sk.sk5, sk.sk6, sk.sk7, 0x01, 0x01, 0x02, td.EOT)
    val SET_REFRESH_RATE_PACKAGE_TEMPLATE: ByteArray = byteArrayOf(td.IOT, sk.sk0, sk.sk1, sk.sk2, sk.sk3, sk.sk4, sk.sk5, sk.sk6, sk.sk7, 0x02, rsr, 0x05, 0x06, td.EOT)
    val SET_MAX_DISTANCE_PACKAGE_TEMPLATE: ByteArray = byteArrayOf(td.IOT, sk.sk0, sk.sk1, sk.sk2, sk.sk3, sk.sk4, sk.sk5, sk.sk6, sk.sk7, 0x03, rsd, d200, 0x00, d200crc, td.EOT)
    val SET_STOP_PACKAGE_TEMPLATE: ByteArray = byteArrayOf(td.IOT, sk.sk0, sk.sk1, sk.sk2, sk.sk3, sk.sk4, sk.sk5, sk.sk6, sk.sk7, 0x01, rstop, 0x01, td.EOT)
}

fun getAngleFromPack(p: Package): Float {
    return (p.payload[1] + p.payload[2] + 135).toFloat()
}

fun getDistanceFromPack(p: Package): Float {
    return (p.payload[3] + p.payload[4]).toFloat()
}

fun getConfiguredRefreshRateFromPack(p: Package): Int {
    return (p.payload[5]).toInt()
}

fun getConfiguredMaxDistanceFromPack(p: Package): Int {
    return (p.payload[6] + p.payload[7]).toInt()
}

fun byteArrayToIntArray(pack: ByteArray): IntArray {
    val packIntArray: IntArray = IntArray(pack.size)
    for ((i, byteChar) in pack.withIndex()) {
        if (byteChar < 0){
            val unsignedVal = 256 + byteChar.toInt()
            packIntArray[i] = unsignedVal
        }else{
            packIntArray[i] = byteChar.toInt()
        }
    }
    return packIntArray
}

fun intArrayToPackage(intPack: IntArray): Package {
    return Package(
        intPack[0],
        intPack.copyOfRange(1, 9),
        intPack[9],
        intPack.copyOfRange(10, 10 + intPack[9]),
        intPack[10 + intPack[9]],
        intPack[11 + intPack[9]]
    )
}

fun getPackAsFormattedString(data: IntArray): String {

    val stringBuilder = StringBuilder(data.size)
    for ( inteiro in data) {
        stringBuilder.append(String.format("%d\n", inteiro))
    }
    return stringBuilder.toString()

}

fun calculateChecksum(pack: Package): Int {
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

fun validatePackage(pack: Package, CRC: Int): Boolean {
    return pack.iot == TransmissionDelimitters().IOT.toInt() && pack.crc == CRC && pack.eot == TransmissionDelimitters().EOT.toInt()
}

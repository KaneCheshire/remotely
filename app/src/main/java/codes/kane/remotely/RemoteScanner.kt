package codes.kane.remotely

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.util.forEach

/// Scans for nearby Boosted remotes.
/// See bottom of file for an example output.
class RemoteScanner {

    companion object {
        private const val TAG = "RemoteScanner"

        /// These are defined here: https://devzone.nordicsemi.com/f/nordic-q-a/29083/ble-advertising-data-flags-field-and-discovery
        /// Representing different flags (that can be combined) and included in the advertising packet.
        const val BLE_GAP_ADV_FLAG_LE_LIMITED_DISC_MODE = 0x01
        const val BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE = 0x02
        const val BLE_GAP_ADV_FLAG_BR_EDR_NOT_SUPPORTED = 0x04
        const val BLE_GAP_ADV_FLAG_LE_BR_EDR_CONTROLLER = 0x08
        const val BLE_GAP_ADV_FLAG_LE_BR_EDR_HOST = 0x10
    }

    private val discoveries = mutableSetOf<BluetoothDevice>()
    private val scanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    private val scanCallback = object : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            Log.d(TAG, "onBatchScanResults() - ${results?.size}")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "onScanFailed() - $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (!discoveries.add(result!!.device)) return // Callback gets called repeatedly, we only care about the first time it's scanned
            Log.d(TAG, "onScanResult() ------------START-------------- \n\n")
            val dataComplete = result!!.dataStatus == ScanResult.DATA_COMPLETE
            Log.d(TAG, "onScanResult() - dataComplete $dataComplete (${result.dataStatus})")
            val isConnectable = result.isConnectable
            Log.d(TAG, "onScanResult() - isConnectable $isConnectable")
            val bluetoothClass = result.device.bluetoothClass
            Log.d(TAG, "onScanResult() - bluetoothClass ${bluetoothClass.deviceClass} ${bluetoothClass.majorDeviceClass}")
            val isLegacy = result.isLegacy
            Log.d(TAG, "onScanResult() - isLegacy $isLegacy")
            val periodicAdvertisingInterval = result.periodicAdvertisingInterval
            Log.d(TAG, "onScanResult() - periodicAdvertisingInterval $periodicAdvertisingInterval")
            val txPower = result.txPower
            Log.d(TAG, "onScanResult() - txPower $txPower")
            val primaryPhy = result.primaryPhy
            Log.d(TAG, "onScanResult() - primaryPhy $primaryPhy")
            val secondaryPhy = result.secondaryPhy
            Log.d(TAG, "onScanResult() - secondaryPhy $secondaryPhy")
            val advertisingSid = result.advertisingSid
            Log.d(TAG, "onScanResult() - advertisingSid $advertisingSid")
            val scanRecord = result.scanRecord!!
            val rawScanResultBytes = scanRecord.bytes
            Log.d(TAG, "onScanResult() - raw scan result bytes ${rawScanResultBytes.hexString}")
            val flags = scanRecord.advertiseFlags
            Log.d(TAG, "onScanResult() - flags $flags")
            val flagsContainLELimited = flags.hasFlag(BLE_GAP_ADV_FLAG_LE_LIMITED_DISC_MODE)
            val flagsContainLEGeneral = flags.hasFlag(BLE_GAP_ADV_FLAG_LE_GENERAL_DISC_MODE)
            val flagsContainBREDRNotSupported = flags.hasFlag(BLE_GAP_ADV_FLAG_BR_EDR_NOT_SUPPORTED)
            val flagsContainBREDRController = flags.hasFlag(BLE_GAP_ADV_FLAG_LE_BR_EDR_CONTROLLER)
            val flagsContainBREDRHost = flags.hasFlag(BLE_GAP_ADV_FLAG_LE_BR_EDR_HOST)
            Log.d(TAG, "onScanResult() - flagsContainLELimited $flagsContainLELimited | flagsContainLEGeneral $flagsContainLEGeneral | flagsContainBREDRNotSupported $flagsContainBREDRNotSupported | flagsContainBREDRController $flagsContainBREDRController | flagsContainBREDRHost $flagsContainBREDRHost")
            val manufacturerSpecificDatas = scanRecord.manufacturerSpecificData
            manufacturerSpecificDatas.forEach { id, bytes ->
                Log.d(TAG, "onScanResult() - manufacturerSpecificData $id | ${bytes.hexString}")
            }
            val name = scanRecord.deviceName
            Log.d(TAG, "onScanResult() - name $name")
            val serviceDatas = scanRecord.serviceData
            serviceDatas.forEach {
                Log.d(TAG, "onScanResult() - serviceData ${it.key} ${it.value.hexString}")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val solicitedUUIDs = scanRecord.serviceSolicitationUuids
                Log.d(TAG, "onScanResult() - solicitedUUIDs ${solicitedUUIDs.map { it.uuid }}")
            }
            val serviceUUIDs = scanRecord.serviceUuids
            Log.d(TAG, "onScanResult() - serviceUUIDs ${serviceUUIDs.map { it.uuid }}")
            val txPowerLevel = scanRecord.txPowerLevel
            Log.d(TAG, "onScanResult() - txPowerLevel $txPowerLevel")
            Log.d(TAG, "onScanResult() ------------END-------------- \n\n")
            onDeviceDiscoveredCallback(result.device)
        }
    }
    private lateinit var onDeviceDiscoveredCallback: (BluetoothDevice) -> Unit

    /// Starts scanning for nearby remotes.
    /// All output is logged quite verbosely to the console.
    fun startScanning(onDeviceDiscovered: (BluetoothDevice) -> Unit) {
        this.onDeviceDiscoveredCallback = onDeviceDiscovered
        val filter = ScanFilter.Builder()
            // The service the remote advertises is all we need to discover nearby remotes
            // Note this doesn't mean the remote only has this service to use once you connect,
            // It's just what gets advertised.
            .setServiceUuid(ParcelUuid(UUIDs.Services.general))
            .build()
        val settings = ScanSettings.Builder()
            .setReportDelay(0)
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    fun stopScan() {
        scanner.stopScan(scanCallback)
    }
}

/*

onScanResult() ------------START--------------
onScanResult() - dataComplete true (0)
onScanResult() - isConnectable true
onScanResult() - isLegacy true
onScanResult() - periodicAdvertisingInterval 0
onScanResult() - txPower 127
onScanResult() - primaryPhy 1
onScanResult() - secondaryPhy 0
onScanResult() - advertisingSid 255
onScanResult() - raw scan result bytes 02 01 06 11 06 66 7C 50 17 55 5E 22 8D E6 11 56 00 2C 77 C4 F4 09 FF 00 00 03 03 02 FF FF FF 08 FF 00 C6 11 AF 99 FF FF 13 09 42 6F 6F 73 74 65 64 52 6D 74 39 39 41 46 31 31 43 36 00 00
onScanResult() - flags 6
onScanResult() - flagsContainLELimited false | flagsContainLEGeneral true | flagsContainBREDRNotSupported true | flagsContainBREDRController false | flagsContainBREDRHost false
onScanResult() - manufacturerSpecificData 0 | 03 03 02 FF FF FF
onScanResult() - manufacturerSpecificData 50688 | 11 AF 99 FF FF
onScanResult() - name BoostedRmt99AF11C6
onScanResult() - solicitedUUIDs []
onScanResult() - serviceUUIDs [f4c4772c-0056-11e6-8d22-5e5517507c66]
onScanResult() - txPowerLevel -2147483648
onScanResult() ------------END--------------

 */
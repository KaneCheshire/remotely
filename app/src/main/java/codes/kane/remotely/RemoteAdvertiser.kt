package codes.kane.remotely

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.os.ParcelUuid
import android.util.Log

/// Advertises this device as a Boosted remote (as closely as possible to how a real remote would)
class RemoteAdvertiser {

    companion object {
        private const val TAG = "RemoteAdvertiser"
    }

    private val advertiser = BluetoothAdapter.getDefaultAdapter().bluetoothLeAdvertiser
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "onStartFailure() - $errorCode")
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "onStartSuccess() - $settingsInEffect")
        }
    }
    private val settings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .build()

    // Starts advertising as a remote.
    // If you pass true for asPairing, then  it will advertise itself in the same manner that a
    // remote does in pairing mode when pressing the power button 5 times.
    fun startAdvertising(asPairing: Boolean) {
        // Since advertisement data can only have up to a maximum of 31 bytes,
        // the remote includes some info in the ad data, and some in the scan response.
        val id = if (asPairing) 2 else 0
        val adManufactuererData = byteArrayOf(0x03.toByte(), 0x03.toByte(), 0x02.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val adData = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUIDs.Services.general))
            /// The id is either 0 or 2 depending on whether it's pairing or not. That's all that changes.
            .addManufacturerData(id, adManufactuererData)
            .build()

        val scanResponseManufacturerData = byteArrayOf(0x11.toByte(), 0xAF.toByte(), 0x99.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val scanResponse = AdvertiseData.Builder()
            .addManufacturerData(50688, scanResponseManufacturerData)
            .setIncludeTxPowerLevel(false)
            .setIncludeDeviceName(true)
            .build()
        advertiser.startAdvertising(settings, adData, scanResponse, advertiseCallback)
    }

}
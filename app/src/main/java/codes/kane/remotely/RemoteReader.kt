package codes.kane.remotely

import android.bluetooth.*
import android.os.Handler
import android.os.Looper
import android.util.Log

/*
Connects and reads every value of every discoverable and readable characteristic.
 */
class RemoteReader {

    companion object {
        private const val TAG = "RemoteReader"
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.e(TAG, "onConnectionStateChange() - $status $newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (!gatt!!.discoverServices()) throw IllegalStateException("Discovering services should work")
            } else {
                when (status) {
                    133 -> retry() // 133 is a lower-level failure that basically means retry
                    else -> gatt!!.close() // Unhandled error, just close out. Usually means the other device disconnected.
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onServicesDiscovered() - $status")
            if (status != BluetoothGatt.GATT_SUCCESS) throw IllegalStateException("Failed to discover services, cannot continue. Status code $status")
            gatt!!.services.forEach { service ->
                Log.d(TAG, "Service: ${service.uuid} ${service.instanceId} ${service.type} ${service.includedServices.size}")
                service.characteristics.forEach { characteristic ->
                    Log.d(TAG, "    Characteristic: ${characteristic.uuid} ${characteristic.instanceId} ${characteristic.permissions} ${characteristic.properties} ${characteristic.writeType} ${characteristic.value?.hexString} ${characteristic.descriptors.size} ${characteristic.service.uuid}")
                    characteristic.descriptors.forEach { descriptor ->
                        Log.d(TAG, "        Descriptor: ${descriptor.uuid} ${descriptor.permissions} ${descriptor.value?.hexString}")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            Log.d(TAG, "onCharacteristicChanged() - ${characteristic!!.uuid}")
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Log.d(TAG, "onCharacteristicRead() - ${characteristic!!.uuid} $status")
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            Log.d(TAG, "onCharacteristicWrite() - ${characteristic!!.uuid} $status")
        }

        override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.d(TAG, "onDescriptorRead() - ${descriptor!!.uuid} $status")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            Log.d(TAG, "onDescriptorWrite() - ${descriptor!!.uuid} $status")
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d(TAG, "onMtuChanged() - $mtu $status")
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            Log.d(TAG, "onPhyRead() - $txPhy $rxPhy $status")
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            Log.d(TAG, "onPhyUpdate() - $txPhy $rxPhy $status")
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            Log.d(TAG, "onReadRemoteRssi() - $rssi $status")
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            Log.d(TAG, "onReliableWriteCompleted() - $status")
        }
    }
    private lateinit var gatt: BluetoothGatt

    fun read(remote: BluetoothDevice) {
        gatt = remote.connectGatt(
            null,
            false, // false means it will time out after a little while with error 133, allowing you to retry. Otherwise it just waits indefinitely which sometimes never works.
            callback,
            BluetoothDevice.TRANSPORT_LE,
            BluetoothDevice.PHY_LE_1M_MASK,
            Handler(Looper.getMainLooper())
        )
    }

    private fun retry() {
        gatt.close() // Important to call
        read(gatt.device)
    }

}

/*

Service: 00001801-0000-1000-8000-00805f9b34fb 1 0 0
    Characteristic: 00002a05-0000-1000-8000-00805f9b34fb 3 0 32 2 null 1
        Descriptor: 00002902-0000-1000-8000-00805f9b34fb 0 null
Service: 00001800-0000-1000-8000-00805f9b34fb 5 0 0
    Characteristic: 00002a00-0000-1000-8000-00805f9b34fb 7 0 2 2 null 0
    Characteristic: 00002a01-0000-1000-8000-00805f9b34fb 9 0 2 2 null 0
    Characteristic: 00002a04-0000-1000-8000-00805f9b34fb 11 0 2 2 null 0
Service: afc05da0-0cd4-11e6-a148-3e1d05defe78 12 0 0
    Characteristic: afc0653e-0cd4-11e6-a148-3e1d05defe78 14 0 18 2 null 1
        Descriptor: 00002902-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: afc063f4-0cd4-11e6-a148-3e1d05defe78 17 0 2 2 null 0
    Characteristic: afc0653f-0cd4-11e6-a148-3e1d05defe78 19 0 4 1 null 0
    Characteristic: afc06540-0cd4-11e6-a148-3e1d05defe78 21 0 2 2 null 0
Service: f4c4772c-0056-11e6-8d22-5e5517507c66 22 0 0
    Characteristic: f4c47a4c-0056-11e6-8d22-5e5517507c66 24 0 12 1 null 1
        Descriptor: 00002904-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: f4c47d8a-0056-11e6-8d22-5e5517507c66 27 0 12 1 null 1
        Descriptor: 00002904-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: f4c47e66-0056-11e6-8d22-5e5517507c66 30 0 12 1 null 1
        Descriptor: 00002904-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: f4c429f3-0056-11e6-8d22-5e5517507c66 33 0 12 1 null 1
        Descriptor: 00002904-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: f4c48032-0056-11e6-8d22-5e5517507c66 36 0 12 1 null 1
        Descriptor: 00002904-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: f4c4293f-0056-11e6-8d22-5e5517507c66 39 0 14 1 null 0
Service: 0000180a-0000-1000-8000-00805f9b34fb 40 0 0
    Characteristic: 00002a24-0000-1000-8000-00805f9b34fb 42 0 2 2 null 0
    Characteristic: 00002a25-0000-1000-8000-00805f9b34fb 44 0 2 2 null 1
        Descriptor: 00002902-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: 00002a27-0000-1000-8000-00805f9b34fb 47 0 2 2 null 1
        Descriptor: 00002902-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: 00002a26-0000-1000-8000-00805f9b34fb 50 0 2 2 null 0
    Characteristic: 00002a29-0000-1000-8000-00805f9b34fb 52 0 2 2 null 0
    Characteristic: 00002a50-0000-1000-8000-00805f9b34fb 54 0 2 2 null 0
Service: 00001016-d102-11e1-9b23-00025b00a5a5 55 0 0
    Characteristic: 00001013-d102-11e1-9b23-00025b00a5a5 57 0 10 2 null 0
    Characteristic: 00001018-d102-11e1-9b23-00025b00a5a5 59 0 8 2 null 0
    Characteristic: 00001014-d102-11e1-9b23-00025b00a5a5 61 0 18 2 null 1
        Descriptor: 00002902-0000-1000-8000-00805f9b34fb 0 null
    Characteristic: 00001011-d102-11e1-9b23-00025b00a5a5 64 0 2 2 null 0
*/
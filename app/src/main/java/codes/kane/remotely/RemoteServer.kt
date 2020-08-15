package codes.kane.remotely

import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import codes.kane.remotely.UUIDs.Services.general
import codes.kane.remotely.UUIDs.Services.throttle
import codes.kane.remotely.UUIDs.Services.ota
import java.lang.IllegalStateException
import java.util.*

/// Turns the device into a GATT service representing the same
/// services and characteristics that a real remote has.
/// It's hard to know if this is exactly right since we don't know the permissions
/// that the remote adds to the characteristics, but we can check the properties.
/// In any case, the onConnectionStateChange never gets called, so not sure what exactly is wrong.
class RemoteServer(context: Context) {

    companion object {
        private const val TAG = "RemoteServer"
    }

    private val handler = Handler(Looper.getMainLooper())

    private val callback = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            Log.d(TAG, "onServiceAdded() - $status $service")
            if (status != BluetoothGatt.GATT_SUCCESS) throw IllegalStateException("Adding service failed ")
            handler.post { // Bump to the main thread to keep things consistent
                if (service?.uuid == general) addThrottleService() // Safely add services one at a time rather than all at once
                if (service?.uuid == throttle) addOTAService()
                if (service?.uuid == ota) onStartedCallback()
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange() - $status $newState ${device!!} ${device.bondState} ${device.type} ${device.bluetoothClass}")
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
            Log.d(TAG, "onCharacteristicReadRequest() - $device $requestId $offset, ${characteristic!!.uuid}")
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            Log.d(TAG, "onCharacteristicWriteRequest() - $device $requestId $offset, ${characteristic!!.uuid} $preparedWrite $responseNeeded $value ${value.hexString}")
            // TODO: Need to reply if necessary
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor?) {
            Log.d(TAG, "onDescriptorReadRequest $device $requestId $offset $descriptor")
            // TODO: Need to reply
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            Log.d(TAG, "onDescriptorWriteRequest $device $requestId $descriptor $preparedWrite $responseNeeded $offset $value")
            // TODO: Need to reply if necessary
        }

        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            Log.d(TAG, "onExecuteWrite $device $requestId $execute")
            // TODO: Need to reply
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            Log.d(TAG, "onMtuChanged $device $mtu")
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            Log.d(TAG, "onNotificationSent $device $status")
        }

        override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            Log.d(TAG, "onPhyRead $device $txPhy $rxPhy $status")
        }

        override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            Log.d(TAG, "onPhyUpdate $device $txPhy $rxPhy $status")
        }
    }
    private val server = context.getSystemService(BluetoothManager::class.java).openGattServer(context, callback)
    private lateinit var onStartedCallback: () -> Unit

    /// After all services are added, onStarted will be called.
    /// Ensure Bluetooth is on before calling this.
    fun start(onStarted: () -> Unit) {
        onStartedCallback = onStarted
        server.clearServices()
        addGeneralService()
    }

    private fun addGeneralService() {
        /// According to Core Bluetooth on iOS, no services are marked as primary, but if we don't make them
        /// BluetoothGattService.SERVICE_TYPE_PRIMARY then they aren't discoverable.
        /// Just another Apple bug I guess.
        val service = BluetoothGattService(general, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristicA = BluetoothGattCharacteristic(
            UUID.fromString("F4C47A4C-0056-11E6-8D22-5E5517507C66"), // 0x01 dims blue light, 0x02 starts orange lights cycling
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        service.addCharacteristic(characteristicA)

        val characteristicB = BluetoothGattCharacteristic(
            UUID.fromString("F4C47D8A-0056-11E6-8D22-5E5517507C66"), // Setting to 0x01 disconnects without turning off remote
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicB)

        val characteristicC = BluetoothGattCharacteristic(
            UUID.fromString("F4C47E66-0056-11E6-8D22-5E5517507C66"), // Setting to 0x01 makes bottom orange light flash, can't seem to turn off without turning off and on again
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicC)

        val characteristicD = BluetoothGattCharacteristic(
            UUID.fromString("F4C429F3-0056-11E6-8D22-5E5517507C66"), // Make beep indefinitely. 0x0 off, 0x01 on
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicD)

        val characteristicE = BluetoothGattCharacteristic(
            UUID.fromString("F4C48032-0056-11E6-8D22-5E5517507C66"), // Disconnect from controller side as if pressing button (turn off)
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicE)

        val characteristicF = BluetoothGattCharacteristic(
            UUID.fromString("F4C4293F-0056-11E6-8D22-5E5517507C66"), // name
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicF)
        server.addService(service)
    }

    private fun addThrottleService() {
        val service = BluetoothGattService(throttle, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristicA = BluetoothGattCharacteristic(
            UUID.fromString("AFC0653E-0CD4-11E6-A148-3E1D05DEFE78"),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristicA)

        val characteristicB = BluetoothGattCharacteristic(
            UUID.fromString("AFC063F4-0CD4-11E6-A148-3E1D05DEFE78"),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristicB)

        val characteristicC = BluetoothGattCharacteristic(
            UUID.fromString("AFC0653F-0CD4-11E6-A148-3E1D05DEFE78"),
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicC)

        val characteristicD = BluetoothGattCharacteristic(
            UUID.fromString("AAFC06540-0CD4-11E6-A148-3E1D05DEFE78"),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristicD)
        server.addService(service)
    }

    private fun addOTAService() {
        val service = BluetoothGattService(ota, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristicA = BluetoothGattCharacteristic(
            UUID.fromString("00001013-D102-11E1-9B23-00025B00A5A5"),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicA)

        val characteristicB = BluetoothGattCharacteristic(
            UUID.fromString("00001018-D102-11E1-9B23-00025B00A5A5"),
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicB)

        val characteristicC = BluetoothGattCharacteristic(
            UUID.fromString("00001014-D102-11E1-9B23-00025B00A5A5"),
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristicC)

        val characteristicD = BluetoothGattCharacteristic(
            UUID.fromString("00001011-D102-11E1-9B23-00025B00A5A5"),
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristicD)
        server.addService(service)
    }

}
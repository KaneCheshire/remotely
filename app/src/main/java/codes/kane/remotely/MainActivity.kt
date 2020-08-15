package codes.kane.remotely

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.IllegalStateException

class MainActivity : AppCompatActivity() {

    companion object {
        private const val ACTIVITY_REQUEST_PAIRING = 0
        private const val ACTIVITY_REQUEST_NOT_PAIRING = 1
    }

    /// Scans for nearby remotes
    private val scanner = RemoteScanner()

    /// Reads the data from remotes found by RemoteScanner
    private val reader = RemoteReader()

    /// Allows other devices to read/write data on this device.
    /// Should start this before advertising so the server is ready to go if something
    /// discovers this device.
    private val server by lazy { RemoteServer(this) }

    /// Advertises this device as a remote
    private val advertiser = RemoteAdvertiser()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.contains(PackageManager.PERMISSION_DENIED)) throw IllegalStateException("Permissions are required to continue")
        scanner.startScanning {
//            scanner.stopScan() // Uncomment these two lines to make the app read the values of the first remote it discovers
//            reader.read(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_scan.setOnClickListener {
            // Android requires location permission to scan with Bluetooth otherwise it silently fails
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }
        button_advertise.setOnClickListener {
            // Make sure this device is discoverable. This _shouldn't_ be required,
            // but doing it for the sake of making sure all bases are covered.
            ensureDiscoverable(false)
        }
        button_advertise_pairing.setOnClickListener {
            ensureDiscoverable(true)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_CANCELED) throw IllegalStateException("Discoverable mode required to continue")
        val asPairing = requestCode == ACTIVITY_REQUEST_PAIRING
        server.start { advertiser.startAdvertising(asPairing) }
    }

    private fun ensureDiscoverable(forPairing: Boolean) {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        // Request discoverable for 5 minutes / 300 seconds
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        // Hacky but easy way to know if we started this activity for advertising in pairing mode or not
        val requestCode = if (forPairing) ACTIVITY_REQUEST_PAIRING else ACTIVITY_REQUEST_NOT_PAIRING
        startActivityForResult(discoverableIntent, requestCode)
    }
}
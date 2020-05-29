package org.grunblatt.scdetector


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    var serviceUuids: List<UUID> =
        (BuildConfig.SERVICE_UUIDS).map { value -> UUID.fromString(value) }

    private var listening: Boolean = false
    private val maxSize: Int = 50

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var beacons: MutableList<ScanResult> = ArrayList<ScanResult>()

    private val REQUEST_ENABLE_BT: Int = 1337

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val mScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (beacons.size > maxSize) {
                beacons.removeAt(maxSize)
            }
            beacons.add(0, result)
            viewAdapter.notifyDataSetChanged()
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (sr in results) {
                if (beacons.size > maxSize) {
                    beacons.removeAt(maxSize - 1)
                }
                beacons.add(0, sr)
            }
            viewAdapter.notifyDataSetChanged()
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("scanFailed", "Error Code: $errorCode")
        }
    }

    private var BluetoothLeScanner: BluetoothLeScanner? = null


    /* Scan Settings */
    val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
        .setReportDelay(0L)
        .build()

    var scanFilters = BuildConfig.SERVICE_UUIDS.map { value ->
        ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(value))).build()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("listening", listening)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        listening = savedInstanceState.getBoolean("listening")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        viewManager = LinearLayoutManager(this)
        (viewManager as LinearLayoutManager).isItemPrefetchEnabled = false
        (viewManager as LinearLayoutManager).reverseLayout = false
        (viewManager as LinearLayoutManager).stackFromEnd = false
        viewAdapter = MyItemRecyclerViewAdapter(beacons)

        recyclerView = findViewById<RecyclerView>(R.id.list).apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(
                    this,
                    "The permission to get BLE location data is required",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ), 1
                )
            }
        } else {
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show()
        }

        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        BluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        listen.setOnClickListener { view ->
            if (!listening) {
                Snackbar.make(view, "Starting Listener", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                listen.setImageResource(R.drawable.ic_pause)
                BluetoothLeScanner?.startScan(scanFilters, scanSettings, mScanCallback)
                listening = true
            } else {
                Snackbar.make(view, "Stopping Listener", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
                listen.setImageResource(R.drawable.ic_play)
                BluetoothLeScanner?.stopScan(mScanCallback)
                listening = false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
}

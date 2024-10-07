package itan.com.bluetoothle.kt

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import itan.com.bluetoothle.R

class CentralActivity : AppCompatActivity() {

    private val EXTRAS_DEVICE_NAME: String = "DEVICE_NAME"
    private val EXTRAS_DEVICE_ADDRESS: String = "DEVICE_ADDRESS"

    private lateinit var bleCentral: BleCentral

    private lateinit var readTextView: TextView
    private lateinit var writeEditText: EditText
    private lateinit var notifyTextView: TextView

    private lateinit var readButton: Button
    private lateinit var writeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_central)

        bleCentral = BleCentral(baseContext)
        intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)?.apply {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val device = bluetoothAdapter.getRemoteDevice(this)
            bleCentral.connectToPeripheral(device)
        }
    }

    override fun onResume() {
        super.onResume()

        readTextView = findViewById(R.id.textview_read)
        writeEditText = findViewById(R.id.edittext_write)
        notifyTextView = findViewById(R.id.textview_notify)

        bleCentral.setupCharacteristic(
            { data ->
                readTextView.text = data
            },
            { data ->
                notifyTextView.text = data
            }
        )

        readButton = findViewById<Button>(R.id.button_read).apply {
            setOnClickListener {
                bleCentral.readCharacteristic()
            }
        }
        writeButton = findViewById<Button>(R.id.button_write).apply {
            setOnClickListener {
                val text = writeEditText.text.toString()
                bleCentral.writeCharacteristic(text.toByteArray())
                writeEditText.text.clear()
            }
        }
    }
}
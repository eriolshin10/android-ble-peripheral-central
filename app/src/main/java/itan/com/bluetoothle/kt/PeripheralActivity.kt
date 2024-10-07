package itan.com.bluetoothle.kt

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import itan.com.bluetoothle.R

class PeripheralActivity : AppCompatActivity() {

    private lateinit var blePeripheral: BlePeripheral

    private lateinit var readEditText: EditText
    private lateinit var writeTextView: TextView
    private lateinit var notifyEditText: EditText

    private lateinit var readButton: Button
    private lateinit var notifyButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_peripheral)

        blePeripheral = BlePeripheral(baseContext)
    }


    override fun onResume() {
        super.onResume()

        readEditText = findViewById(R.id.edittext_read)
        writeTextView = findViewById(R.id.textview_write)
        notifyEditText = findViewById(R.id.edittext_notify)

        blePeripheral.writeCentralDevice { data ->
            writeTextView.text = data
        }

        readButton = findViewById<Button>(R.id.button_read).apply {
            setOnClickListener {
                val text = readEditText.text.toString()
                blePeripheral.readCentralDevice(text.toByteArray())
                readEditText.text.apply {
                    hint = text
                    clear()
                }
            }
        }

        notifyButton = findViewById<Button>(R.id.button_notify).apply {
            setOnClickListener {
                val text = notifyEditText.text.toString()
                blePeripheral.notifyCentralDevice(text.toByteArray())
                notifyEditText.text.apply {
                    hint = text
                    clear()
                }
            }
        }
    }
}
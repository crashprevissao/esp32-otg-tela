package com.example.esp32displaymirror

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.os.Build
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), SerialInputOutputManager.Listener {

    private var usbIoManager: SerialInputOutputManager? = null
    private var serialPort: com.hoho.android.usbserial.driver.UsbSerialPort? = null
    private lateinit var monitorView: MonitorView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or 
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or 
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        monitorView = MonitorView(this)
        setContentView(monitorView)
        initUsb()
    }

    private fun initUsb() {
        val manager = getSystemService(Context.USB_SERVICE) as UsbManager
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "Conecte o cabo OTG no ESP32", Toast.LENGTH_SHORT).show()
            return
        }

        val driver = availableDrivers[0]
        val connection = manager.openDevice(driver.device) ?: return
        val port = driver.ports[0]
        
        try {
            port.open(connection)
            port.setParameters(115200, 8, com.hoho.android.usbserial.driver.UsbSerialPort.DATABITS_8, com.hoho.android.usbserial.driver.UsbSerialPort.PARITY_NONE)
            serialPort = port
            
            usbIoManager = SerialInputOutputManager(serialPort, this)
            Executors.newSingleThreadExecutor().submit(usbIoManager)
            Toast.makeText(this, "Conectado ao ESP32!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Erro Serial: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewData(data: ByteArray?) {
        data?.let {
            monitorView.updateData(it)
        }
    }

    override fun onRunError(e: Exception?) {
        runOnUiThread { Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show() }
    }

    fun sendTouchCoordinates(x: Int, y: Int) {
        val command = "T:$x,$y\n"
        try {
            serialPort?.write(command.toByteArray(), 200)
        } catch (e: IOException) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        usbIoManager?.stop()
        try { serialPort?.close() } catch (e: IOException) { }
    }

    inner class MonitorView(context: Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.WHITE
            textSize = 45f
        }
        private var infoText = "Aguardando frames do ESP32..."

        fun updateData(data: ByteArray) {
            infoText = "Dados recebidos: ${data.size} bytes"
            postInvalidate() 
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.BLACK)
            canvas.drawText(infoText, 100f, height / 2f, paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val esp32X = ((event.x / width) * 320).toInt()
                val esp32Y = ((event.y / height) * 240).toInt()
                sendTouchCoordinates(esp32X, esp32Y)
                return true
            }
            return super.onTouchEvent(event)
        }
    }
}

package com.example.esp32displaymirror // MUDE ISSO PARA O QUE ESTAVA NA LINHA 1 DO SEU ARQUIVO ORIGINAL

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val ACTION_USB_PERMISSION = "com.example.esp32displaymirror.USB_PERMISSION"
    private lateinit var usbManager: UsbManager
    private var esp32Device: UsbDevice? = null

    // 1. O OUVINTE (Agora blindado para não causar Crash)
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            Toast.makeText(context, "Permissão USB concedida!", Toast.LENGTH_SHORT).show()
                            
                            // --> SEU CÓDIGO DE LIGAR NO ESP32 VAI AQUI <--
                            
                        }
                    } else {
                        Toast.makeText(context, "Permissão USB negada pelo usuário.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main) // REMOVA AS "//" NO INÍCIO DESTA LINHA SE VOCÊ TIVER UMA TELA DE INTERFACE (XML)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        val filter = IntentFilter(ACTION_USB_PERMISSION)

        // CORREÇÃO CRÍTICA PARA ANDROID 14+ (Mata o Crash de Segurança)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }

        solicitarPermissaoUsbSegura()
    }

    // 2. FUNÇÃO QUE CHAMA A JANELINHA DO SISTEMA
    private fun solicitarPermissaoUsbSegura() {
        val deviceList = usbManager.deviceList

        if (deviceList.isEmpty()) {
            Toast.makeText(this, "Nenhum cabo OTG ou ESP32 conectado.", Toast.LENGTH_SHORT).show()
            return
        }

        esp32Device = deviceList.values.firstOrNull()

        esp32Device?.let { device ->
            if (usbManager.hasPermission(device)) {
                Toast.makeText(this, "A permissão já existe! Conectando...", Toast.LENGTH_SHORT).show()
                // --> SEU CÓDIGO DE CONEXÃO VAI AQUI <--
            } else {
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
                val permissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), flags)
                
                // Dispara a janela de permissão na tela
                usbManager.requestPermission(device, permissionIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }
}


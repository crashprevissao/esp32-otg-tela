package com.exemplo.esp32otg // <-- MUDE PARA O PACOTE REAL DO SEU PROJETO

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

    // Cria um "nome" único para a ação de permissão do nosso app
    private val ACTION_USB_PERMISSION = "com.exemplo.esp32otg.USB_PERMISSION" // <-- PODE DEIXAR ASSIM OU MUDAR PRO SEU PACOTE
    private lateinit var usbManager: UsbManager
    private var esp32Device: UsbDevice? = null

    // 1. CRIA O "OUVINTE"
    // Ele fica invisível esperando você clicar em "Permitir" ou "Negar" na janelinha
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    // Verifica se o usuário clicou no botão de permitir
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            // SUCESSO! A PERMISSÃO FOI CONCEDIDA.
                            Toast.makeText(context, "Permissão concedida! Conectando ao ESP32...", Toast.LENGTH_SHORT).show()

                            // --> AQUI VOCÊ COLOCA O SEU CÓDIGO REAL DE CONEXÃO COM A PLACA <--
                            // iniciarComunicacaoSerial(device)
                        }
                    } else {
                        // O usuário negou a permissão
                        Toast.makeText(context, "Permissão USB negada. O app não pode funcionar.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_main) // Descomente isso se você tiver uma tela/layout configurada

        // Prepara o gerenciador de USB
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // Registra o nosso "ouvinte" para que o Android avise ele quando a permissão for dada
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

        // Chama a função que inicia todo o processo
        solicitarPermissaoUsbSegura()
    }

    // 2. FUNÇÃO QUE VERIFICA E PEDE A PERMISSÃO
    private fun solicitarPermissaoUsbSegura() {
        val deviceList = usbManager.deviceList

        if (deviceList.isEmpty()) {
            Toast.makeText(this, "Nenhum cabo OTG ou ESP32 conectado.", Toast.LENGTH_SHORT).show()
            return
        }

        // Pega o primeiro dispositivo USB que estiver plugado (geralmente o ESP32)
        esp32Device = deviceList.values.firstOrNull()

        esp32Device?.let { device ->
            // Verifica se o Android já deu permissão antes
            if (usbManager.hasPermission(device)) {
                Toast.makeText(this, "Permissão já existe! Iniciando...", Toast.LENGTH_SHORT).show()
                // --> AQUI VOCÊ COLOCA O SEU CÓDIGO DE CONEXÃO <--

            } else {
                // NÃO TEM PERMISSÃO.
                // Cria a janelinha do Android para pedir a permissão.

                // Nota: O Android 12+ exige a flag FLAG_MUTABLE para permissões de USB
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
                
                val permissionIntent = PendingIntent.getBroadcast(
                    this, 0, Intent(ACTION_USB_PERMISSION), flags
                )

                // Esse comando é o que faz a janelinha pular na tela!
                usbManager.requestPermission(device, permissionIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // É obrigatório "desligar" o ouvinte quando o app fecha para não dar erro de memória
        unregisterReceiver(usbReceiver)
    }
}

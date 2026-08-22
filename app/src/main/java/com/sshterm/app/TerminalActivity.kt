package com.sshterm.app

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sshterm.app.databinding.ActivityTerminalBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class TerminalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerminalBinding
    private val manager get() = SessionHolder.manager

    // Le sequenze ANSI possono arrivare spezzate tra due letture SSH.
    private var ansiPending = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val activeManager = manager
        if (activeManager == null || !activeManager.isConnected) {
            finish()
            return
        }

        setupQuickCommandsBar()
        startReadingOutput()

        binding.buttonSend.setOnClickListener { sendTypedCommand() }
        binding.editCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTypedCommand()
                true
            } else false
        }

        binding.buttonManageQuick.setOnClickListener {
            startActivity(Intent(this, QuickCommandsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        setupQuickCommandsBar()
    }

    private fun setupQuickCommandsBar() {
        binding.layoutQuickCommands.removeAllViews()
        val commands = AppStorage.loadQuickCommands(this)
        commands.forEach { qc ->
            val btn = Button(this).apply {
                text = qc.label
                isAllCaps = false
                textSize = 12f
                minWidth = 0
                minimumWidth = 0
                setPadding(24, 0, 24, 0)
                setOnClickListener { sendCommandSafely(qc.command) }
            }
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(44)
            ).apply { marginEnd = dpToPx(6) }
            binding.layoutQuickCommands.addView(btn, params)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun sendTypedCommand() {
        val text = binding.editCommand.text.toString()
        if (text.isNotEmpty()) {
            binding.editCommand.setText("")
            sendCommandSafely(text)
        }
    }

    private fun sendCommandSafely(command: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val activeManager = manager
                if (activeManager == null || !activeManager.isConnected) {
                    withContext(Dispatchers.Main) {
                        appendOutput("\n[Connessione SSH non attiva]\n")
                    }
                    return@launch
                }
                activeManager.sendCommand(command)
            } catch (e: Exception) {
                val message = e.message ?: e.javaClass.simpleName
                withContext(Dispatchers.Main) {
                    appendOutput("\n[Errore invio comando: $message]\n")
                }
            }
        }
    }

    private fun startReadingOutput() {
        val input = manager?.inputStream ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val reader = BufferedReader(InputStreamReader(input))
            val buffer = CharArray(2048)
            while (manager?.isConnected == true) {
                try {
                    val read = reader.read(buffer)
                    if (read == -1) break
                    val raw = String(buffer, 0, read)
                    val clean = cleanAnsi(raw)
                    if (clean.isNotEmpty()) {
                        withContext(Dispatchers.Main) { appendOutput(clean) }
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    /**
     * Rimuove le sequenze di controllo ANSI/VT100 che una normale TextView non sa
     * interpretare (colori \u001B[01;34m, bracketed paste \u001B[?2004h, ecc.).
     * Conserva un piccolo frammento finale se una sequenza arriva spezzata.
     */
    @Synchronized
    private fun cleanAnsi(chunk: String): String {
        var text = ansiPending + chunk
        ansiPending = ""

        val lastEsc = text.lastIndexOf('\u001B')
        if (lastEsc >= 0) {
            val tail = text.substring(lastEsc)
            // Se il frammento ESC finale non contiene ancora un terminatore CSI/OSC,
            // lo teniamo per la lettura successiva.
            val completeCsi = Regex("^\\u001B\\[[0-?]*[ -/]*[@-~]").containsMatchIn(tail)
            val completeOsc = tail.contains("\u0007") || tail.contains("\u001B\\")
            if (!completeCsi && !completeOsc && tail.length < 64) {
                ansiPending = tail
                text = text.substring(0, lastEsc)
            }
        }

        // OSC: ESC ] ... BEL oppure ESC \\
        text = text.replace(Regex("\\u001B\\][^\\u0007]*(?:\\u0007|\\u001B\\\\)"), "")
        // CSI: ESC [ parametri/intermedi/finale (copre colori, erase, cursor, ?2004h/l, ecc.)
        text = text.replace(Regex("\\u001B\\[[0-?]*[ -/]*[@-~]"), "")
        // Escape a due caratteri rimasti.
        text = text.replace(Regex("\\u001B[@-_]"), "")
        // Alcuni controlli singoli non stampabili, lasciando TAB/CR/LF.
        text = text.filter { it == '\n' || it == '\r' || it == '\t' || it.code >= 32 }
        return text
    }

    private fun appendOutput(text: String) {
        binding.textOutput.append(text)
        binding.scrollOutput.post {
            binding.scrollOutput.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            manager?.disconnect()
            SessionHolder.manager = null
        }
    }
}

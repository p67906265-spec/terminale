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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerminalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val activeManager = manager
        if (activeManager == null || !activeManager.isConnected) {
            finish() // torna al login se non c'è connessione attiva
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
        // Ricarica la barra nel caso siano stati modificati i comandi rapidi
        setupQuickCommandsBar()
    }

    private fun setupQuickCommandsBar() {
        binding.layoutQuickCommands.removeAllViews()
        val commands = AppStorage.loadQuickCommands(this)
        commands.forEach { qc ->
            val btn = Button(this).apply {
                text = qc.label
                setOnClickListener { manager?.sendCommand(qc.command) }
            }
            binding.layoutQuickCommands.addView(btn)
        }
    }

    private fun sendTypedCommand() {
        val text = binding.editCommand.text.toString()
        if (text.isNotEmpty()) {
            manager?.sendCommand(text)
            binding.editCommand.setText("")
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
                    val chunk = String(buffer, 0, read)
                    withContext(Dispatchers.Main) { appendOutput(chunk) }
                } catch (e: Exception) {
                    break
                }
            }
        }
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

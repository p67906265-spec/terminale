package com.sshterm.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sshterm.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Precompila con l'ultima connessione usata (senza password)
        binding.editHost.setText(AppStorage.loadLastHost(this))
        binding.editUsername.setText(AppStorage.loadLastUser(this))
        binding.editPort.setText(AppStorage.loadLastPort(this).toString())

        binding.buttonConnect.setOnClickListener { attemptConnect() }
    }

    private fun attemptConnect() {
        val host = binding.editHost.text.toString().trim()
        val user = binding.editUsername.text.toString().trim()
        val pass = binding.editPassword.text.toString()
        val portText = binding.editPort.text.toString().trim()
        val port = if (portText.isEmpty()) 22 else portText.toIntOrNull() ?: 22

        if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            binding.textError.text = "Compila IP, utente e password"
            return
        }

        binding.textError.text = ""
        binding.progressBar.visibility = View.VISIBLE
        binding.buttonConnect.isEnabled = false

        lifecycleScope.launch {
            val manager = SshManager(applicationContext)
            val error = withContext(Dispatchers.IO) {
                try {
                    manager.connect(host, user, pass, port)
                    null
                } catch (e: Exception) {
                    e.message ?: "Errore di connessione"
                }
            }

            binding.progressBar.visibility = View.GONE
            binding.buttonConnect.isEnabled = true

            if (error != null) {
                binding.textError.text = error
            } else {
                AppStorage.saveLastConnection(this@LoginActivity, host, user, port)
                SessionHolder.manager = manager
                startActivity(Intent(this@LoginActivity, TerminalActivity::class.java))
            }
        }
    }
}

/**
 * Tiene in memoria la connessione SSH attiva così TerminalActivity
 * può usarla senza dover riconnettersi. Semplice per un'app a singola sessione.
 */
object SessionHolder {
    var manager: SshManager? = null
}

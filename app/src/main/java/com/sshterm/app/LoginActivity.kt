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
    private var unlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.visibility = View.INVISIBLE

        val lastMode = AppStorage.loadConnectionMode(this)
        binding.radioTailscale.isChecked = lastMode == "tailscale"
        binding.radioLocal.isChecked = lastMode != "tailscale"

        binding.radioConnectionMode.setOnCheckedChangeListener { _, _ ->
            if (unlocked) refreshConnectionMode()
        }

        binding.buttonConnect.setOnClickListener { attemptConnect() }

        authenticate()
    }

    private fun authenticate() {
        AppBiometricLock.authenticate(
            activity = this,
            onSuccess = {
                unlocked = true
                binding.root.visibility = View.VISIBLE
                refreshConnectionMode()
            },
            onFailure = {
                if (!unlocked) {
                    binding.root.visibility = View.VISIBLE
                    binding.textError.text =
                        "Configura o usa impronta/blocco schermo sicuro per accedere."
                    binding.buttonConnect.isEnabled = false
                }
            }
        )
    }

    private fun refreshConnectionMode() {
        val tailscale = binding.radioTailscale.isChecked

        binding.textConnectionSubtitle.text =
            if (tailscale) "Connessione SSH via Tailscale" else "Connessione SSH locale"

        binding.textConnectionHelp.text =
            if (tailscale) "Tailscale deve essere attivo. Inserisci IP 100.x.x.x o nome MagicDNS."
            else "Usa l'IP locale del server Linux."

        binding.editHost.hint =
            if (tailscale) "IP Tailscale o nome MagicDNS" else "Indirizzo IP locale"

        binding.editHost.setText(AppStorage.loadHost(this, tailscale))
        binding.editUsername.setText(AppStorage.loadUser(this, tailscale))
        binding.editPort.setText(AppStorage.loadPort(this, tailscale).toString())

        val savedPassword = try {
            SecurePasswordStorage.loadPassword(this, tailscale)
        } catch (_: Exception) {
            null
        }

        binding.editPassword.setText(savedPassword ?: "")
        binding.checkSavePassword.isChecked = savedPassword != null
    }

    private fun attemptConnect() {
        val tailscale = binding.radioTailscale.isChecked
        val host = binding.editHost.text.toString().trim()
        val user = binding.editUsername.text.toString().trim()
        val pass = binding.editPassword.text.toString()
        val portText = binding.editPort.text.toString().trim()
        val port = if (portText.isEmpty()) 22 else portText.toIntOrNull() ?: 22

        if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            binding.textError.text = "Compila indirizzo, utente e password"
            return
        }

        // Salva sempre host, porta, utente e modalità PRIMA del tentativo.
        AppStorage.saveConnectionProfile(
            this,
            host = host,
            user = user,
            port = port,
            tailscale = tailscale
        )

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
                    try { manager.disconnect() } catch (_: Exception) {}
                    e.message ?: e.javaClass.simpleName
                }
            }

            binding.progressBar.visibility = View.GONE
            binding.buttonConnect.isEnabled = true

            if (error != null) {
                binding.textError.text = error
                return@launch
            }

            // Il salvataggio della password NON deve mai far cadere una connessione SSH riuscita.
            if (binding.checkSavePassword.isChecked) {
                try {
                    SecurePasswordStorage.savePassword(this@LoginActivity, tailscale, pass)
                } catch (_: Exception) {
                    // Se la finestra biometrica del Keystore è scaduta, continua comunque.
                }
            } else {
                try {
                    SecurePasswordStorage.clearPassword(this@LoginActivity, tailscale)
                } catch (_: Exception) {
                }
            }

            SessionHolder.manager = manager
            startActivity(Intent(this@LoginActivity, TerminalActivity::class.java))
        }
    }
}

object SessionHolder {
    var manager: SshManager? = null
}

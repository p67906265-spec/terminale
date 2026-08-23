package com.sshterm.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
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

        binding.editUsername.setText(AppStorage.loadLastUser(this))
        binding.editPort.setText(AppStorage.loadLastPort(this).toString())

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
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val prompt = BiometricPrompt(
                    this,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(
                            result: BiometricPrompt.AuthenticationResult
                        ) {
                            super.onAuthenticationSucceeded(result)
                            unlocked = true
                            binding.root.visibility = View.VISIBLE
                            refreshConnectionMode()
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence
                        ) {
                            super.onAuthenticationError(errorCode, errString)
                            if (!unlocked) finish()
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Terminale")
                    .setSubtitle("Sblocca con impronta o blocco schermo")
                    .setAllowedAuthenticators(authenticators)
                    .build()

                prompt.authenticate(promptInfo)
            }

            else -> {
                // Se il dispositivo non ha biometria/blocco sicuro configurato,
                // non mostrare le credenziali salvate.
                binding.textError.text =
                    "Configura impronta o blocco schermo sicuro per usare Terminale."
                binding.root.visibility = View.VISIBLE
                binding.buttonConnect.isEnabled = false
            }
        }
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

        val savedHost = if (tailscale) {
            AppStorage.loadTailscaleHost(this)
        } else {
            AppStorage.loadLastHost(this)
        }
        binding.editHost.setText(savedHost)

        val savedPassword = SecurePasswordStorage.loadPassword(this, tailscale)
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
                AppStorage.saveLastConnection(
                    this@LoginActivity,
                    host,
                    user,
                    port,
                    tailscale = tailscale
                )

                if (binding.checkSavePassword.isChecked) {
                    SecurePasswordStorage.savePassword(this@LoginActivity, tailscale, pass)
                } else {
                    SecurePasswordStorage.clearPassword(this@LoginActivity, tailscale)
                }

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

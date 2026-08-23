package com.sshterm.app

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.PublicKey
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.Security
import org.bouncycastle.jce.provider.BouncyCastleProvider

/**
 * Gestisce una connessione SSH verso il PC Linux e mantiene aperta
 * una shell interattiva a cui inviare comandi (digitati o rapidi).
 *
 * SICUREZZA HOST KEY: usa Trust On First Use (TOFU).
 * Alla prima connessione memorizza l'impronta SHA-256 della chiave del server.
 * Dalle connessioni successive accetta solo la stessa chiave; se cambia,
 * la connessione viene rifiutata per proteggere da possibili attacchi MITM.
 */
class SshManager(private val context: Context) {

    private var client: SSHClient? = null
    private var session: Session? = null
    private var shell: Session.Shell? = null

    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set

    val isConnected: Boolean
        get() = client?.isConnected == true && shell?.isOpen == true

    /**
     * Apre la connessione SSH e avvia una shell interattiva.
     * Deve essere chiamata da un thread in background (es. coroutine su Dispatchers.IO).
     */
    @Throws(Exception::class)
    fun connect(host: String, username: String, password: String, port: Int = 22) {
        // Android include un provider BC ridotto che, su alcuni dispositivi,
        // non espone X25519. SSHJ puo' negoziare X25519 durante il key exchange.
        // Sostituiamo quindi il provider Android con la versione completa inclusa nell'app.
        ensureModernBouncyCastle()

        val c = SSHClient()
        c.addHostKeyVerifier(TofuHostKeyVerifier(context))
        c.connectTimeout = 8000
        c.connect(host, port)
        c.authPassword(username, password)

        val s = c.startSession()
        s.allocateDefaultPTY()
        val sh = s.startShell()

        client = c
        session = s
        shell = sh
        inputStream = sh.inputStream
        outputStream = sh.outputStream
    }

    /**
     * Invia un comando alla shell remota, aggiungendo il newline finale.
     */
    fun sendCommand(command: String) {
        val out = outputStream ?: return
        out.write((command + "\n").toByteArray())
        out.flush()
    }

    /**
     * Invia testo grezzo senza newline automatico (utile per input carattere per carattere,
     * es. Ctrl+C -> "\u0003").
     */
    fun sendRaw(text: String) {
        val out = outputStream ?: return
        out.write(text.toByteArray())
        out.flush()
    }

    private fun ensureModernBouncyCastle() {
        val current = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
        if (current == null || current.javaClass != BouncyCastleProvider::class.java) {
            if (current != null) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            }
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        }
    }

    private class TofuHostKeyVerifier(context: Context) : HostKeyVerifier {
        private val prefs = context.getSharedPreferences("ssh_host_keys", Context.MODE_PRIVATE)

        override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
            val id = "$hostname:$port"
            val fingerprint = sha256Fingerprint(key)
            val saved = prefs.getString(id, null)
            return if (saved == null) {
                prefs.edit().putString(id, fingerprint).apply()
                true
            } else {
                saved == fingerprint
            }
        }

        override fun findExistingAlgorithms(hostname: String, port: Int): List<String> = emptyList()

        private fun sha256Fingerprint(key: PublicKey): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
            return "SHA256:" + Base64.encodeToString(digest, Base64.NO_WRAP)
        }
    }

    fun disconnect() {
        try { shell?.close() } catch (_: Exception) {}
        try { session?.close() } catch (_: Exception) {}
        try { client?.disconnect() } catch (_: Exception) {}
        shell = null
        session = null
        client = null
        inputStream = null
        outputStream = null
    }
}

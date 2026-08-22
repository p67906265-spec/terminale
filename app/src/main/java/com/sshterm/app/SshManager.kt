package com.sshterm.app

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Gestisce una connessione SSH verso il PC Linux e mantiene aperta
 * una shell interattiva a cui inviare comandi (digitati o rapidi).
 *
 * NOTA SICUREZZA: usa PromiscuousVerifier, cioè accetta qualsiasi host key
 * senza verifica. Va bene per una rete domestica fidata, ma se in futuro
 * si vuole più sicurezza si può passare a un verificatore con host key fissa.
 */
class SshManager {

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
        val c = SSHClient()
        c.addHostKeyVerifier(PromiscuousVerifier())
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

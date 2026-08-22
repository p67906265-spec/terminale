package com.sshterm.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class QuickCommand(
    val label: String,
    val command: String
)

/**
 * Salva/carica i comandi rapidi e le credenziali di connessione
 * usando SharedPreferences (semplice, nessun server esterno).
 */
object AppStorage {

    private const val PREFS = "ssh_terminal_prefs"
    private const val KEY_COMMANDS = "quick_commands"
    private const val KEY_HOST = "last_host"
    private const val KEY_USER = "last_user"
    private const val KEY_PORT = "last_port"

    fun loadQuickCommands(context: Context): MutableList<QuickCommand> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_COMMANDS, null) ?: return defaultCommands()
        val arr = JSONArray(json)
        val list = mutableListOf<QuickCommand>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(QuickCommand(obj.getString("label"), obj.getString("command")))
        }
        return list
    }

    fun saveQuickCommands(context: Context, commands: List<QuickCommand>) {
        val arr = JSONArray()
        commands.forEach {
            val obj = JSONObject()
            obj.put("label", it.label)
            obj.put("command", it.command)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COMMANDS, arr.toString())
            .apply()
    }

    fun saveLastConnection(context: Context, host: String, user: String, port: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HOST, host)
            .putString(KEY_USER, user)
            .putInt(KEY_PORT, port)
            .apply()
        // Nota: la password NON viene salvata per motivi di sicurezza.
    }

    fun loadLastHost(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_HOST, "") ?: ""

    fun loadLastUser(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, "") ?: ""

    fun loadLastPort(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_PORT, 22)

    private fun defaultCommands() = mutableListOf(
        QuickCommand("Docker ps", "docker ps"),
        QuickCommand("Docker riavvia tutti", "docker restart \$(docker ps -q)"),
        QuickCommand("Home Assistant restart", "docker restart homeassistant"),
        QuickCommand("Spazio disco", "df -h"),
        QuickCommand("Uso memoria", "free -h")
    )
}

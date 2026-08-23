package com.sshterm.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class QuickCommand(
    val label: String,
    val command: String
)

object AppStorage {

    private const val PREFS = "ssh_terminal_prefs"
    private const val KEY_COMMANDS = "quick_commands"
    private const val KEY_CONNECTION_MODE = "connection_mode"

    private const val KEY_LOCAL_HOST = "local_host"
    private const val KEY_LOCAL_USER = "local_user"
    private const val KEY_LOCAL_PORT = "local_port"

    private const val KEY_TAILSCALE_HOST = "tailscale_host"
    private const val KEY_TAILSCALE_USER = "tailscale_user"
    private const val KEY_TAILSCALE_PORT = "tailscale_port"

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

    fun saveConnectionProfile(context: Context, host: String, user: String, port: Int, tailscale: Boolean) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_CONNECTION_MODE, if (tailscale) "tailscale" else "local")

        if (tailscale) {
            editor.putString(KEY_TAILSCALE_HOST, host)
                .putString(KEY_TAILSCALE_USER, user)
                .putInt(KEY_TAILSCALE_PORT, port)
        } else {
            editor.putString(KEY_LOCAL_HOST, host)
                .putString(KEY_LOCAL_USER, user)
                .putInt(KEY_LOCAL_PORT, port)
        }
        editor.apply()
    }

    fun loadHost(context: Context, tailscale: Boolean): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (tailscale) KEY_TAILSCALE_HOST else KEY_LOCAL_HOST
        prefs.getString(key, null)?.let { return it }

        val legacyKey = if (tailscale) "last_tailscale_host" else "last_host"
        return prefs.getString(legacyKey, "") ?: ""
    }

    fun loadUser(context: Context, tailscale: Boolean): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (tailscale) KEY_TAILSCALE_USER else KEY_LOCAL_USER
        prefs.getString(key, null)?.let { return it }
        return prefs.getString("last_user", "") ?: ""
    }

    fun loadPort(context: Context, tailscale: Boolean): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (tailscale) KEY_TAILSCALE_PORT else KEY_LOCAL_PORT
        return if (prefs.contains(key)) prefs.getInt(key, 22) else prefs.getInt("last_port", 22)
    }

    fun loadConnectionMode(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CONNECTION_MODE, "local") ?: "local"

    private fun defaultCommands() = mutableListOf(
        QuickCommand("Docker ps", "docker ps"),
        QuickCommand("Docker riavvia tutti", "docker restart \$(docker ps -q)"),
        QuickCommand("Home Assistant restart", "docker restart homeassistant"),
        QuickCommand("Spazio disco", "df -h"),
        QuickCommand("Uso memoria", "free -h")
    )
}

package com.sshterm.app

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.os.Build

/**
 * Memorizza le password SSH cifrate con AES/GCM.
 * La chiave AES vive nell'Android Keystore e non viene salvata nei Preferences.
 */
object SecurePasswordStorage {
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "TerminalePasswordKeyV2"
    private const val LEGACY_KEY_ALIAS = "TerminalePasswordKey"
    private const val PREFS = "secure_passwords"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)

        // La chiave può essere usata solo dopo una recente autenticazione
        // biometrica o con la credenziale sicura del dispositivo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                30,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(30)
        }

        generator.init(builder.build())
        return generator.generateKey()
    }


    private fun getLegacyKeyOrNull(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            keyStore.getKey(LEGACY_KEY_ALIAS, null) as? SecretKey
        } catch (_: Exception) {
            null
        }
    }

    fun savePassword(context: Context, tailscale: Boolean, password: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(if (tailscale) "password_tailscale" else "password_local", value)
            .apply()
    }

    fun loadPassword(context: Context, tailscale: Boolean): String? {
        val prefKey = if (tailscale) "password_tailscale" else "password_local"
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val value = prefs.getString(prefKey, null) ?: return null

        fun decryptWith(key: SecretKey): String? {
            return try {
                val parts = value.split(":", limit = 2)
                if (parts.size != 2) return null

                val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    GCMParameterSpec(128, iv)
                )
                String(cipher.doFinal(encrypted), Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }

        // Prima prova con la nuova chiave vincolata all'autenticazione.
        decryptWith(getOrCreateKey())?.let { return it }

        // Migrazione trasparente dalla chiave 1.13 non vincolata alla biometria.
        val legacy = getLegacyKeyOrNull() ?: return null
        val plain = decryptWith(legacy) ?: return null

        return try {
            savePassword(context, tailscale, plain)
            plain
        } catch (_: Exception) {
            null
        }
    }

    fun clearPassword(context: Context, tailscale: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(if (tailscale) "password_tailscale" else "password_local")
            .apply()
    }

    fun hasPassword(context: Context, tailscale: Boolean): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .contains(if (tailscale) "password_tailscale" else "password_local")
}

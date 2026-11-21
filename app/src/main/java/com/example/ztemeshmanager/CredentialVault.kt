package com.example.ztemeshmanager

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts the optional auto-login secret with an Android Keystore AES-GCM key. */
object CredentialVault {
    private const val alias = "zte_mesh_manager_credentials_v1"
    private const val prefsName = "zte_secure"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    fun save(context: Context, username: String, password: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
            .putString("username", username)
            .putString("password", Base64.encodeToString(payload, Base64.NO_WRAP))
            .apply()
    }

    fun load(context: Context): Pair<String, String>? {
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val username = prefs.getString("username", null) ?: return null
        val encoded = prefs.getString("password", null) ?: return null
        return try {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            if (payload.size <= 12) return null
            val iv = payload.copyOfRange(0, 12)
            val encrypted = payload.copyOfRange(12, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            username to String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }

    fun clear(context: Context) { context.getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit().clear().apply() }
}

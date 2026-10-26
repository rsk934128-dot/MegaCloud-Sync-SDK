package com.example.manager

import android.content.Context
import android.content.SharedPreferences

class VaultSecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("apppulse_vault_security", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_ENABLED = "key_pin_enabled"
        private const val KEY_VAULT_PIN = "key_vault_pin"
        private const val DEFAULT_PIN = "1234"
    }

    fun isPinProtectionEnabled(): Boolean {
        return prefs.getBoolean(KEY_PIN_ENABLED, true)
    }

    fun setPinProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
    }

    fun verifyPin(input: String): Boolean {
        val currentPin = prefs.getString(KEY_VAULT_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
        return input == currentPin
    }

    fun updatePin(newPin: String): Boolean {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            prefs.edit().putString(KEY_VAULT_PIN, newPin).apply()
            return true
        }
        return false
    }

    fun getCurrentPin(): String {
        return prefs.getString(KEY_VAULT_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }
}

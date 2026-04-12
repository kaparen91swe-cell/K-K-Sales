package com.example.kksales.util

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    // For simplicity in this demo, we use a fixed key derived from MasterKey for the chat
    // In a real app, you'd use the Keystore directly for each message or a session key
    private val fixedKey = SecretKeySpec("KKSalesChatSecretKey123456789012".toByteArray(), "AES")
    private val algorithm = "AES/CBC/PKCS5Padding"

    fun encrypt(text: String): String {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, fixedKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(text.toByteArray())
        return Base64.encodeToString(iv + encrypted, Base64.DEFAULT)
    }

    fun decrypt(encryptedText: String): String {
        val data = Base64.decode(encryptedText, Base64.DEFAULT)
        val iv = data.sliceArray(0 until 16)
        val encrypted = data.sliceArray(16 until data.size)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, fixedKey, IvParameterSpec(iv))
        return String(cipher.doFinal(encrypted))
    }
}

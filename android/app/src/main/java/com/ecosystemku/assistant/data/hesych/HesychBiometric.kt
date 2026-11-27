package com.ecosystemku.assistant.data.hesych

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AndroidKeyStore + BiometricPrompt helper that seals Hesych's raw DEK
 * behind strong device biometrics (fingerprint / face) without storing the master password.
 */
object HesychBiometric {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "hesych_bio_dek_key_v1"
    private const val PREF_BIO_IV = "hesych_bio_iv"
    private const val PREF_BIO_CT = "hesych_bio_ct"

    fun canUseBiometric(context: Context): Boolean {
        val mgr = BiometricManager.from(context)
        return mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun isBiometricEnrolledForVault(context: Context): Boolean {
        val prefs = context.getSharedPreferences("hesych_prefs", Context.MODE_PRIVATE)
        val iv = prefs.getString(PREF_BIO_IV, null)
        val ct = prefs.getString(PREF_BIO_CT, null)
        return !iv.isNullOrBlank() && !ct.isNullOrBlank() && hasKeystoreKey()
    }

    fun clearEnrollment(context: Context) {
        context.getSharedPreferences("hesych_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_BIO_IV)
            .remove(PREF_BIO_CT)
            .apply()
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
        }
    }

    fun enrollBiometricDek(
        activity: FragmentActivity,
        rawDek: ByteArray,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cipher = runCatching {
            val key = createFreshKeystoreKey()
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, key)
            }
        }.getOrElse { err ->
            onError("Gagal menyiapkan kunci biometrik: ${err.message}")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authedCipher = result.cryptoObject?.cipher
                    if (authedCipher == null) {
                        onError("Cipher biometrik tidak tersedia")
                        return
                    }
                    runCatching {
                        val ct = authedCipher.doFinal(rawDek)
                        val iv = authedCipher.iv
                        activity.getSharedPreferences("hesych_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString(PREF_BIO_IV, HesychCrypto.toBase64(iv))
                            .putString(PREF_BIO_CT, HesychCrypto.toBase64(ct))
                            .apply()
                    }.onSuccess {
                        onSuccess()
                    }.onFailure { err ->
                        onError("Gagal menyimpan kunci biometrik: ${err.message}")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Aktifkan Buka Kunci Biometrik")
            .setSubtitle("Konfirmasi sidik jari/wajah untuk membuka vault Hesych")
            .setNegativeButtonText("Batal")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    fun unlockWithBiometric(
        activity: FragmentActivity,
        onUnlocked: (SecretKey) -> Unit,
        onError: (String) -> Unit
    ) {
        val prefs = activity.getSharedPreferences("hesych_prefs", Context.MODE_PRIVATE)
        val ivB64 = prefs.getString(PREF_BIO_IV, null)
        val ctB64 = prefs.getString(PREF_BIO_CT, null)
        if (ivB64.isNullOrBlank() || ctB64.isNullOrBlank()) {
            onError("Biometrik belum diaktifkan untuk vault ini")
            return
        }

        val cipher = runCatching {
            val iv = HesychCrypto.fromBase64(ivB64)
            val key = getKeystoreKey() ?: error("Kunci biometrik tidak ditemukan")
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            }
        }.getOrElse { err ->
            clearEnrollment(activity)
            onError("Kunci biometrik kedaluwarsa, silakan buka dengan Master Password (${err.message})")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authedCipher = result.cryptoObject?.cipher
                    if (authedCipher == null) {
                        onError("Autentikasi biometrik gagal")
                        return
                    }
                    runCatching {
                        val rawDek = authedCipher.doFinal(HesychCrypto.fromBase64(ctB64))
                        val dek = HesychCrypto.importAesKey(rawDek)
                        rawDek.fill(0)
                        dek
                    }.onSuccess(onUnlocked)
                        .onFailure { err ->
                            onError("Gagal mendekripsi vault: ${err.message}")
                        }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Vault Hesych")
            .setSubtitle("Gunakan biometrik perangkat untuk mendekripsi vault")
            .setNegativeButtonText("Gunakan Password")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    }

    private fun hasKeystoreKey(): Boolean = runCatching {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        ks.containsAlias(KEY_ALIAS)
    }.getOrDefault(false)

    private fun getKeystoreKey(): SecretKey? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun createFreshKeystoreKey(): SecretKey {
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }
        kg.init(builder.build())
        return kg.generateKey()
    }
}

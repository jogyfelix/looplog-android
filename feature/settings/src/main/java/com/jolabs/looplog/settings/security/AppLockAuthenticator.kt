package com.jolabs.looplog.settings.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper over [BiometricPrompt] for the app lock. Accepts a strong biometric or the
 * device credential (PIN/pattern/password) as a fallback.
 *
 * The `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` combination isn't supported by
 * [BiometricPrompt] below API 30, so on API 29 we fall back to the deprecated
 * `setDeviceCredentialAllowed` to still offer the PIN fallback.
 */
object AppLockAuthenticator {

    /** True when the device can satisfy an app-lock challenge (biometric or credential). */
    fun canAuthenticate(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } else {
            val biometricOk = manager.canAuthenticate(BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
            val credentialOk =
                context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
            biometricOk || credentialOk
        }
    }

    /**
     * Shows the system authentication prompt. [onSuccess] fires on a verified identity;
     * [onError] fires on a non-recoverable dismissal (user cancel, no credential, etc.).
     * Individual failed attempts keep the prompt open and are not surfaced here.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                } else {
                    @Suppress("DEPRECATION")
                    setDeviceCredentialAllowed(true)
                }
            }
            .build()

        prompt.authenticate(info)
    }
}

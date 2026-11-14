package tech.isano.language.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.isano.language.core.engine.auth.CloudIdentityManager
import tech.isano.language.core.engine.jami.JamiDaemon
import tech.isano.language.core.engine.jami.JamiDaemonImpl

/**
 * ViewModel orchestrating the sovereign, zero-reading hybrid cloud onboarding flow.
 * Coordinates 4-digit PIN cryptography via [CloudIdentityManager] and the captivating
 * sequential magic loading experience ("The Illusion of Labor").
 */
class OnboardingViewModel(
    private val cloudIdentityManager: CloudIdentityManager = CloudIdentityManager(),
    private val jamiDaemon: JamiDaemon = JamiDaemonImpl()
) : ViewModel() {

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Executes the PIN-based lockbox authentication / recovery pipeline:
     * - Derives AES key from PIN+salt using PBKDF2.
     * - Decrypts existing archive or encrypts and stores a new Jami identity.
     * - Displays the shifting "Illusion of Labor" animation sequence in the UI.
     */
    fun authenticateWithVault(
        username: String,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isAuthenticating.value = true
            _errorMessage.value = null
            try {
                val startTime = System.currentTimeMillis()

                val (success, errorMsg) = cloudIdentityManager.lockboxAuth(username, pin)

                // Smooth illusion of labor: ensure at least 4200ms to cycle magic loading steps
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < 4200) {
                    delay(4200 - elapsed)
                }

                if (success) {
                    onSuccess()
                } else {
                    val err = errorMsg ?: "Invalid PIN for @$username"
                    _errorMessage.value = err
                    onError(err)
                }
            } catch (e: Exception) {
                val err = e.message ?: "Authentication failed"
                _errorMessage.value = err
                onError(err)
            } finally {
                _isAuthenticating.value = false
            }
        }
    }
}

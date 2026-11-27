package com.ecosystemku.assistant.ui.apps.hesych

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecosystemku.assistant.data.hesych.HesychBackupCodec
import com.ecosystemku.assistant.data.hesych.HesychBiometric
import com.ecosystemku.assistant.data.hesych.HesychBreachChecker
import com.ecosystemku.assistant.data.hesych.HesychHealth
import com.ecosystemku.assistant.data.hesych.HesychHealthReport
import com.ecosystemku.assistant.data.hesych.HesychTotp
import com.ecosystemku.assistant.data.hesych.HesychUnlockResult
import com.ecosystemku.assistant.data.hesych.HesychVaultRepository
import com.ecosystemku.assistant.data.local.AssistantDatabase
import com.ecosystemku.assistant.data.local.HesychCategory
import com.ecosystemku.assistant.data.local.HesychHistoryEntity
import com.ecosystemku.assistant.data.local.HesychVaultItem
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

data class HesychUiState(
    val isInitialized: Boolean = false,
    val isUnlocked: Boolean = false,
    val isBusy: Boolean = false,
    val storedItemCount: Int = 0,
    val items: List<HesychVaultItem> = emptyList(),
    val selectedCategory: HesychCategory? = null,
    val favoritesOnly: Boolean = false,
    val searchQuery: String = "",
    val selectedTag: String? = null,
    val lockError: String? = null,
    val failedAttempts: Int = 0,
    val lockoutUntilMs: Long = 0L,
    val secretLockedUntilMs: Long = 0L,
    val biometricAvailable: Boolean = false,
    val biometricEnrolled: Boolean = false,
    val healthReport: HesychHealthReport? = null,
    val isCheckingBreach: Boolean = false,
    val breachProgressLabel: String? = null,
    val bannerMessage: String? = null
)

class HesychViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AssistantDatabase.getInstance(application)
    private val dao = db.hesychDao()
    private val repo = HesychVaultRepository(dao)
    private val prefs = application.getSharedPreferences("hesych_prefs", Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(HesychUiState())
    val uiState: StateFlow<HesychUiState> = _uiState.asStateFlow()

    @Volatile
    private var activeDek: SecretKey? = null

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_MS_BASE = 10L * 60_000L
        private const val PREF_FAILED_ATTEMPTS = "hesych_failed_attempts"
        private const val PREF_LOCKOUT_UNTIL = "hesych_lockout_until"
        private const val PREF_SECRET_LOCK_UNTIL = "hesych_secret_lock_until"
    }

    init {
        _uiState.update {
            it.copy(
                failedAttempts = prefs.getInt(PREF_FAILED_ATTEMPTS, 0),
                lockoutUntilMs = prefs.getLong(PREF_LOCKOUT_UNTIL, 0L),
                secretLockedUntilMs = prefs.getLong(PREF_SECRET_LOCK_UNTIL, 0L),
                biometricAvailable = HesychBiometric.canUseBiometric(application),
                biometricEnrolled = HesychBiometric.isBiometricEnrolledForVault(application)
            )
        }
        refreshInitState()
        viewModelScope.launch {
            dao.observeItems().collect { rows ->
                val dek = activeDek
                val decrypted = if (dek != null) {
                    withContext(Dispatchers.Default) {
                        rows.mapNotNull { row -> runCatching { repo.decryptRow(dek, row) }.getOrNull() }
                    }
                } else {
                    emptyList()
                }
                _uiState.update {
                    it.copy(
                        storedItemCount = rows.size,
                        items = decrypted
                    )
                }
            }
        }
    }

    fun refreshInitState() {
        viewModelScope.launch {
            val init = repo.isVaultInitialized()
            _uiState.update { it.copy(isInitialized = init) }
        }
    }

    fun setupMasterPassword(password: String, confirm: String) {
        if (_uiState.value.isBusy) return
        if (password.length < 8) {
            _uiState.update { it.copy(lockError = "Master password minimal 8 karakter") }
            return
        }
        if (password != confirm) {
            _uiState.update { it.copy(lockError = "Konfirmasi password tidak cocok") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lockError = null) }
            try {
                val dek = withContext(Dispatchers.Default) {
                    repo.setupMasterPassword(password)
                }
                activeDek = dek
                val items = repo.loadDecryptedItems(dek)
                _uiState.update {
                    it.copy(
                        isInitialized = true,
                        isUnlocked = true,
                        isBusy = false,
                        items = items,
                        lockError = null,
                        bannerMessage = "Vault Hesych siap digunakan (Argon2id + AES-256-GCM)"
                    )
                }
            } catch (err: Throwable) {
                _uiState.update {
                    it.copy(isBusy = false, lockError = "Gagal menyiapkan vault: ${err.message}")
                }
            }
        }
    }

    fun unlock(password: String) {
        if (_uiState.value.isBusy) return
        val now = System.currentTimeMillis()
        val lockoutUntil = _uiState.value.lockoutUntilMs
        if (lockoutUntil > now) {
            val mins = ((lockoutUntil - now) / 60_000L).coerceAtLeast(1L)
            _uiState.update {
                it.copy(lockError = "Terlalu banyak percobaan salah. Coba lagi dalam $mins menit.")
            }
            return
        }
        if (password.isEmpty()) {
            _uiState.update { it.copy(lockError = "Masukkan master password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, lockError = null) }
            val result = withContext(Dispatchers.Default) {
                repo.unlock(password)
            }
            when (result) {
                is HesychUnlockResult.Unlocked -> {
                    prefs.edit()
                        .putInt(PREF_FAILED_ATTEMPTS, 0)
                        .putLong(PREF_LOCKOUT_UNTIL, 0L)
                        .apply()
                    activeDek = result.dek
                    val items = repo.loadDecryptedItems(result.dek)
                    _uiState.update {
                        it.copy(
                            isInitialized = true,
                            isUnlocked = true,
                            isBusy = false,
                            failedAttempts = 0,
                            lockoutUntilMs = 0L,
                            items = items,
                            lockError = null,
                            bannerMessage = if (result.upgradedToArgon2id) {
                                "Vault di-upgrade ke Argon2id"
                            } else {
                                null
                            }
                        )
                    }
                }
                HesychUnlockResult.NotInitialized -> {
                    _uiState.update {
                        it.copy(
                            isInitialized = false,
                            isBusy = false,
                            lockError = "Vault belum diatur"
                        )
                    }
                }
                HesychUnlockResult.WrongPassword -> {
                    val nextFails = _uiState.value.failedAttempts + 1
                    val nextLockout = if (nextFails >= MAX_ATTEMPTS) {
                        val mult = (nextFails - MAX_ATTEMPTS + 1).coerceAtMost(6)
                        System.currentTimeMillis() + LOCKOUT_MS_BASE * mult
                    } else {
                        0L
                    }
                    prefs.edit()
                        .putInt(PREF_FAILED_ATTEMPTS, nextFails)
                        .putLong(PREF_LOCKOUT_UNTIL, nextLockout)
                        .apply()
                    val msg = if (nextLockout > 0L) {
                        "Password salah $nextFails×. Terkunci sementara 10 menit."
                    } else {
                        "Master password salah (percobaan $nextFails/$MAX_ATTEMPTS)"
                    }
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            failedAttempts = nextFails,
                            lockoutUntilMs = nextLockout,
                            lockError = msg
                        )
                    }
                }
            }
        }
    }

    fun lock() {
        activeDek = null
        _uiState.update {
            it.copy(
                isUnlocked = false,
                items = emptyList(),
                healthReport = null,
                isCheckingBreach = false,
                breachProgressLabel = null,
                lockError = null,
                searchQuery = ""
            )
        }
    }

    fun isSecretLocked(nowMs: Long = System.currentTimeMillis()): Boolean =
        _uiState.value.secretLockedUntilMs > nowMs

    fun setSecretLockMinutes(minutes: Int) {
        val until = if (minutes <= 0) 0L else System.currentTimeMillis() + minutes * 60_000L
        prefs.edit().putLong(PREF_SECRET_LOCK_UNTIL, until).apply()
        _uiState.update {
            it.copy(
                secretLockedUntilMs = until,
                bannerMessage = if (until > 0L) "Secret Lock aktif selama $minutes menit" else "Secret Lock dimatikan"
            )
        }
    }

    fun revealSecret(ciphertext: String): String {
        if (isSecretLocked()) return ""
        val dek = activeDek ?: return ""
        return runCatching { repo.revealSecret(dek, ciphertext) }.getOrDefault("")
    }

    fun generateTotpCode(encryptedSecret: String, timeSec: Long = System.currentTimeMillis() / 1000L): String? {
        if (isSecretLocked() || encryptedSecret.isBlank()) return null
        val dek = activeDek ?: return null
        return runCatching {
            val plain = repo.revealSecret(dek, encryptedSecret)
            if (!HesychTotp.isValidBase32(plain)) null
            else HesychTotp.generateTotp(plain, timeSec)
        }.getOrNull()
    }

    fun saveItem(
        id: Long = 0L,
        title: String,
        username: String,
        plainPassword: String,
        notes: String,
        color: Int,
        favorite: Boolean,
        category: HesychCategory,
        tags: List<String>,
        plainTotpSecret: String,
        plainCustomFields: List<Triple<String, String, String>>
    ) {
        val dek = activeDek ?: return
        viewModelScope.launch {
            repo.saveItem(
                dek = dek,
                id = id,
                title = title,
                username = username,
                plainPassword = plainPassword,
                notes = notes,
                color = color,
                favorite = favorite,
                category = category,
                tags = tags,
                plainTotpSecret = plainTotpSecret,
                plainCustomFields = plainCustomFields
            )
            _uiState.update {
                it.copy(bannerMessage = if (id > 0L) "Item vault diperbarui" else "Item baru disimpan")
            }
        }
    }

    fun toggleFavorite(item: HesychVaultItem) {
        val dek = activeDek ?: return
        viewModelScope.launch {
            val plainPw = runCatching { repo.revealSecret(dek, item.password) }.getOrDefault("")
            val plainTotp = runCatching { repo.revealSecret(dek, item.totpSecret) }.getOrDefault("")
            val plainFields = item.customFields.map { field ->
                val v = runCatching { repo.revealSecret(dek, field.value) }.getOrDefault("")
                Triple(field.name, v, field.type)
            }
            repo.saveItem(
                dek = dek,
                id = item.id,
                title = item.title,
                username = item.username,
                plainPassword = plainPw,
                notes = item.notes,
                color = item.color,
                favorite = !item.favorite,
                category = item.category,
                tags = item.tags,
                plainTotpSecret = plainTotp,
                plainCustomFields = plainFields,
                breachStatus = item.breachStatus,
                breachCheckedAt = item.breachCheckedAt
            )
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repo.deleteItem(id)
            _uiState.update { it.copy(bannerMessage = "Item dihapus dari vault") }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setCategoryFilter(category: HesychCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    fun setTagFilter(tag: String?) {
        _uiState.update { it.copy(selectedTag = tag) }
    }

    fun showBanner(message: String) {
        _uiState.update { it.copy(bannerMessage = message) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    suspend fun getPasswordHistory(itemId: Long): List<HesychHistoryEntity> = withContext(Dispatchers.IO) {
        dao.getHistoryForItem(itemId)
    }

    fun runHealthScan() {
        val dek = activeDek ?: return
        val items = _uiState.value.items
        viewModelScope.launch {
            val report = withContext(Dispatchers.Default) {
                HesychHealth.scanVaultHealth(items, dek)
            }
            _uiState.update { it.copy(healthReport = report) }
        }
    }

    fun runBreachCheck() {
        val dek = activeDek ?: return
        if (_uiState.value.isCheckingBreach) return
        val items = _uiState.value.items
        if (items.isEmpty()) {
            showBanner("Vault kosong, tidak ada akun untuk diperiksa")
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isCheckingBreach = true, breachProgressLabel = "0/${items.size}")
            }
            val summary = HesychBreachChecker.checkAllItems(
                httpClient = httpClient,
                dao = dao,
                dek = dek,
                items = items
            ) { done, total ->
                _uiState.update { it.copy(breachProgressLabel = "$done/$total") }
            }
            val updatedItems = repo.loadDecryptedItems(dek)
            val updatedReport = withContext(Dispatchers.Default) {
                HesychHealth.scanVaultHealth(updatedItems, dek)
            }
            _uiState.update {
                it.copy(
                    items = updatedItems,
                    healthReport = updatedReport,
                    isCheckingBreach = false,
                    breachProgressLabel = null,
                    bannerMessage = "Cek HIBP selesai: ${summary.breached} bocor, ${summary.safe} aman" +
                        if (summary.failed > 0) ", ${summary.failed} gagal" else ""
                )
            }
        }
    }

    fun enableBiometric(activity: FragmentActivity) {
        val dek = activeDek ?: return
        val raw = dek.encoded
        if (raw == null || raw.size != 32) {
            showBanner("Kunci vault tidak mendukung ekspor biometrik")
            return
        }
        HesychBiometric.enrollBiometricDek(
            activity = activity,
            rawDek = raw,
            onSuccess = {
                raw.fill(0)
                _uiState.update {
                    it.copy(
                        biometricEnrolled = true,
                        bannerMessage = "Buka kunci biometrik diaktifkan"
                    )
                }
            },
            onError = { msg ->
                raw.fill(0)
                showBanner(msg)
            }
        )
    }

    fun disableBiometric() {
        HesychBiometric.clearEnrollment(getApplication())
        _uiState.update {
            it.copy(
                biometricEnrolled = false,
                bannerMessage = "Buka kunci biometrik dimatikan"
            )
        }
    }

    fun unlockWithBiometric(activity: FragmentActivity) {
        if (_uiState.value.isBusy) return
        HesychBiometric.unlockWithBiometric(
            activity = activity,
            onUnlocked = { dek ->
                viewModelScope.launch {
                    prefs.edit()
                        .putInt(PREF_FAILED_ATTEMPTS, 0)
                        .putLong(PREF_LOCKOUT_UNTIL, 0L)
                        .apply()
                    activeDek = dek
                    val items = repo.loadDecryptedItems(dek)
                    _uiState.update {
                        it.copy(
                            isInitialized = true,
                            isUnlocked = true,
                            failedAttempts = 0,
                            lockoutUntilMs = 0L,
                            items = items,
                            lockError = null
                        )
                    }
                }
            },
            onError = { msg ->
                _uiState.update {
                    it.copy(
                        biometricEnrolled = HesychBiometric.isBiometricEnrolledForVault(getApplication()),
                        lockError = msg
                    )
                }
            }
        )
    }

    suspend fun exportBackupJson(customPassword: String?): String = withContext(Dispatchers.IO) {
        val dek = activeDek ?: error("Vault terkunci")
        val rows = dao.getAllItems()
        if (!customPassword.isNullOrBlank()) {
            HesychBackupCodec.exportCustomBackupJson(rows, dek, customPassword)
        } else {
            val env = dao.getMeta(HesychVaultRepository.META_PW_ENVELOPE)?.value
            HesychBackupCodec.exportMasterBackupJson(rows, env)
        }
    }

    suspend fun importBackupJson(
        rawJson: String,
        replaceAll: Boolean,
        customPassword: String?
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val dek = activeDek ?: error("Vault harus dibuka terlebih dahulu")
            val parsed = HesychBackupCodec.parseImportRows(rawJson, dek, customPassword)
            if (replaceAll) {
                dao.clearHistory()
                dao.clearItems()
                val toInsert = parsed.map { it.copy(id = 0L) }
                if (toInsert.isNotEmpty()) dao.upsertItems(toInsert)
                toInsert.size
            } else {
                val toInsert = parsed.map { it.copy(id = 0L) }
                if (toInsert.isNotEmpty()) dao.upsertItems(toInsert)
                toInsert.size
            }
        }
    }

    override fun onCleared() {
        activeDek = null
        super.onCleared()
    }
}

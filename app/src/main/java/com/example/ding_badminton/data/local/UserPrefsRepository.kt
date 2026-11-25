package com.example.ding_badminton.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ding_badminton.domain.model.BookingConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

class UserPrefsRepository(private val context: Context) {
    private val credentialFileStore = LoginCredentialFileStore(context)
    private val validationTempStore = LoginValidationTempStore(context)

    private object Keys {
        val hasLoginState = booleanPreferencesKey("has_login_state")
        val autoResumeEnabled = booleanPreferencesKey("auto_resume_enabled")
        val shouldResumeTask = booleanPreferencesKey("should_resume_task")
        val alarmScheduled = booleanPreferencesKey("alarm_scheduled")
        val userToken = stringPreferencesKey("user_token")
        val loginAccount = stringPreferencesKey("login_account")
        val loginPasswordEncrypted = stringPreferencesKey("login_password_encrypted")
        val rememberPassword = booleanPreferencesKey("remember_password")
        val campusName = stringPreferencesKey("cfg_campus_name")
        val venueName = stringPreferencesKey("cfg_venue_name")
        val courtNumber = intPreferencesKey("cfg_court_number")
        val targetDayOffset = intPreferencesKey("cfg_target_day_offset")
        val timeSlots = stringPreferencesKey("cfg_time_slots")
    }

    val hasLoginStateFlow: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[Keys.hasLoginState] ?: false }

    val latestConfigFlow: Flow<BookingConfig?> =
        context.userPrefsDataStore.data.map(::toConfigOrNull)

    val userTokenFlow: Flow<String?> =
        context.userPrefsDataStore.data.map { it[Keys.userToken] }

    val loginAccountFlow: Flow<String> =
        context.userPrefsDataStore.data.map { it[Keys.loginAccount].orEmpty() }

    val rememberPasswordFlow: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[Keys.rememberPassword] ?: false }

    val autoResumeEnabledFlow: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[Keys.autoResumeEnabled] ?: true }

    val shouldResumeTaskFlow: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[Keys.shouldResumeTask] ?: false }

    val alarmScheduledFlow: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[Keys.alarmScheduled] ?: false }

    suspend fun markLoginState(isReady: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.hasLoginState] = isReady
        }
    }

    suspend fun saveConfig(config: BookingConfig) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.campusName] = config.campusName
            prefs[Keys.venueName] = config.venueName
            prefs[Keys.courtNumber] = config.courtNumber
            prefs[Keys.targetDayOffset] = config.targetDayOffset
            prefs[Keys.timeSlots] = config.timeSlots.joinToString(",")
        }
    }

    suspend fun saveUserToken(token: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.userToken] = token
        }
    }

    suspend fun clearLoginState() {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.userToken] = ""
            prefs[Keys.hasLoginState] = false
        }
    }

    suspend fun saveLoginForm(
        account: String,
        password: String,
    ) {
        val encrypted = if (password.isNotBlank()) CredentialCrypto.encrypt(password) else ""
        credentialFileStore.save(account = account.trim(), passwordEncrypted = encrypted)
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.loginAccount] = account.trim()
            prefs[Keys.rememberPassword] = true
            prefs[Keys.loginPasswordEncrypted] = encrypted
        }
    }

    fun saveLoginValidationTemp(
        account: String,
        password: String,
    ) {
        val encrypted = if (password.isNotBlank()) CredentialCrypto.encrypt(password) else ""
        validationTempStore.save(account = account, passwordEncrypted = encrypted)
    }

    fun clearLoginValidationTemp() {
        validationTempStore.delete()
    }

    suspend fun getSavedPassword(): String {
        val encrypted = getSavedPasswordEncrypted()
        if (encrypted.isBlank()) return ""
        return runCatching { CredentialCrypto.decrypt(encrypted) }.getOrDefault("")
    }

    suspend fun getSavedPasswordEncrypted(): String {
        val fromFile = credentialFileStore.loadPasswordEncrypted()
        if (fromFile.isNotBlank()) return fromFile
        val fromPrefs =
            context.userPrefsDataStore.data
            .map { it[Keys.loginPasswordEncrypted].orEmpty() }
            .firstOrNull()
            .orEmpty()
        return fromPrefs
    }

    suspend fun getSavedAccount(): String {
        val fromFile = credentialFileStore.loadAccount()
        if (fromFile.isNotBlank()) return fromFile
        return context.userPrefsDataStore.data
            .map { it[Keys.loginAccount].orEmpty() }
            .firstOrNull()
            .orEmpty()
    }

    suspend fun saveAutoResumeEnabled(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.autoResumeEnabled] = enabled
        }
    }

    suspend fun saveShouldResumeTask(enabled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.shouldResumeTask] = enabled
        }
    }

    suspend fun saveAlarmScheduled(scheduled: Boolean) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.alarmScheduled] = scheduled
        }
    }

    suspend fun clearConfig() {
        context.userPrefsDataStore.edit { prefs ->
            prefs[Keys.campusName] = ""
            prefs[Keys.venueName] = ""
            prefs[Keys.courtNumber] = 1
            prefs[Keys.targetDayOffset] = 2
            prefs[Keys.timeSlots] = ""
            prefs[Keys.shouldResumeTask] = false
            prefs[Keys.alarmScheduled] = false
        }
    }

    private fun toConfigOrNull(prefs: Preferences): BookingConfig? {
        val campus = prefs[Keys.campusName] ?: return null
        val venue = prefs[Keys.venueName] ?: ""
        val court = prefs[Keys.courtNumber] ?: 1
        val offset = prefs[Keys.targetDayOffset] ?: 2
        val slotRaw = prefs[Keys.timeSlots].orEmpty()
        val slots = slotRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return BookingConfig(
            campusName = campus,
            venueName = venue,
            courtNumber = court,
            timeSlots = slots,
            targetDayOffset = offset,
        )
    }
}

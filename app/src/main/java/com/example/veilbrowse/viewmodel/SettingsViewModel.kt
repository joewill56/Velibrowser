package com.example.veilbrowse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.veilbrowse.data.model.PrivacyProfileType
import com.example.veilbrowse.data.model.PrivacySettingsState
import com.example.veilbrowse.data.model.SearchEngine
import com.example.veilbrowse.data.repository.PrivacySettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = PrivacySettingsRepository(application)
    val settings: StateFlow<PrivacySettingsState> = settingsRepo.settings

    fun setBlockTrackers(enabled: Boolean) {
        settingsRepo.updateSettings { it.copy(blockTrackers = enabled) }
    }

    fun setBlockThirdPartyCookies(enabled: Boolean) {
        settingsRepo.updateSettings { it.copy(blockThirdPartyCookies = enabled) }
    }

    fun setClearDataOnExit(enabled: Boolean) {
        settingsRepo.updateSettings { it.copy(clearDataOnExit = enabled) }
    }

    fun setWebrtcProtection(enabled: Boolean) {
        settingsRepo.updateSettings { it.copy(webrtcProtection = enabled) }
    }

    fun setSendDoNotTrack(enabled: Boolean) {
        settingsRepo.updateSettings { it.copy(sendDoNotTrack = enabled) }
    }

    fun setPrivacyProfile(profile: PrivacyProfileType) {
        settingsRepo.updateSettings { it.copy(privacyProfile = profile) }
    }

    fun setSearchEngine(engine: SearchEngine) {
        settingsRepo.updateSettings { it.copy(defaultSearchEngine = engine) }
    }

    fun setJavaScriptEnabled(enabled: Boolean) {
        settingsRepo.updateSettings { it.copy(javaScriptEnabled = enabled) }
    }

    fun setBlockPopups(enabled: Boolean) {
        settingsRepo.updateSettings { it.copy(blockPopups = enabled) }
    }

    fun setEnforceHttps(enabled: Boolean) {
        settingsRepo.updateSettings { it.copy(enforceHttps = enabled) }
    }
}

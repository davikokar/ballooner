package com.ballooner.data.settings

import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.TextSizeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [SettingsRepository] for tests. */
class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val settings = MutableStateFlow(initial)

    override fun observeSettings(): Flow<AppSettings> = settings.asStateFlow()

    override suspend fun setDefaultFont(font: BalloonFont) {
        settings.update { it.copy(defaultFont = font) }
    }

    override suspend fun setHideFontSelector(hide: Boolean) {
        settings.update { it.copy(hideFontSelector = hide) }
    }

    override suspend fun setTextSizeMode(mode: TextSizeMode) {
        settings.update { it.copy(textSizeMode = mode) }
    }
}

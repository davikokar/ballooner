package com.ballooner.data.settings

import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.TextSizeMode
import kotlinx.coroutines.flow.Flow

/** Stores and observes app-wide user preferences. */
interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setDefaultFont(font: BalloonFont)
    suspend fun setHideFontSelector(hide: Boolean)
    suspend fun setTextSizeMode(mode: TextSizeMode)
}

package com.ballooner.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.ballooner.data.billing.TipJar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tipJar: TipJar,
) : ViewModel() {

    val thanks: SharedFlow<Unit> = tipJar.thanks

    /** The activity is handed straight to Play's sheet and never held onto. */
    fun buyCoffee(activity: Activity) = tipJar.buyCoffee(activity)
}

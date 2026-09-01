package com.ballooner.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ballooner.R
import com.ballooner.domain.model.AppSettings
import com.ballooner.domain.model.BalloonFont
import com.ballooner.domain.model.TextSizeMode
import com.ballooner.ui.project.label
import com.ballooner.ui.theme.balloonerTopAppBarColors

private enum class SettingsDialog { TEXT, ABOUT, PRIVACY, TERMS }

internal val selectableDefaultFonts = BalloonFont.entries.filterNot { it == BalloonFont.DEFAULT }

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDefaultFontChange = viewModel::setDefaultFont,
        onHideFontSelectorChange = viewModel::setHideFontSelector,
        onTextSizeModeChange = viewModel::setTextSizeMode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onDefaultFontChange: (BalloonFont) -> Unit,
    onHideFontSelectorChange: (Boolean) -> Unit,
    onTextSizeModeChange: (TextSizeMode) -> Unit,
) {
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = balloonerTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_general))
            SettingsRow(Icons.Default.Settings, stringResource(R.string.settings_change_language)) {
                launchIntent(context, Intent(Settings.ACTION_LOCALE_SETTINGS))
            }
            SettingsRow(Icons.Default.Edit, stringResource(R.string.settings_text)) {
                dialog = SettingsDialog.TEXT
            }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_app))
            SettingsRow(Icons.Default.Share, stringResource(R.string.settings_share_app)) {
                val text = context.getString(R.string.share_app_text, context.packageName)
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                launchIntent(context, Intent.createChooser(sendIntent, null))
            }
            SettingsRow(Icons.Default.Star, stringResource(R.string.settings_rate_app)) {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                if (!launchIntent(context, marketIntent)) {
                    launchIntent(
                        context,
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")),
                    )
                }
            }
            SettingsRow(Icons.Default.Email, stringResource(R.string.settings_customer_support)) {
                val email = context.getString(R.string.support_email_address)
                val subject = Uri.encode(context.getString(R.string.support_email_subject))
                if (!launchIntent(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email?subject=$subject")))) {
                    Toast.makeText(context, R.string.error_no_email_app, Toast.LENGTH_SHORT).show()
                }
            }
            SettingsRow(Icons.Default.Info, stringResource(R.string.settings_about)) {
                dialog = SettingsDialog.ABOUT
            }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_community_support))
            SettingsRow(Icons.Default.Favorite, stringResource(R.string.settings_buy_coffee)) {
                launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(BUY_ME_A_COFFEE_URL)))
            }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_legal))
            SettingsRow(Icons.Default.Lock, stringResource(R.string.settings_privacy)) {
                dialog = SettingsDialog.PRIVACY
            }
            SettingsRow(Icons.Default.Info, stringResource(R.string.settings_terms)) {
                dialog = SettingsDialog.TERMS
            }
        }
    }

    when (dialog) {
        SettingsDialog.TEXT -> TextSettingsDialog(
            settings = uiState.settings,
            onDefaultFontChange = onDefaultFontChange,
            onHideFontSelectorChange = onHideFontSelectorChange,
            onTextSizeModeChange = onTextSizeModeChange,
            onDismiss = { dialog = null },
        )
        SettingsDialog.ABOUT -> InformationDialog(
            title = stringResource(R.string.settings_about),
            message = stringResource(R.string.about_message) + "\n\n" +
                stringResource(R.string.about_version, versionName),
            onDismiss = { dialog = null },
        )
        SettingsDialog.PRIVACY -> InformationDialog(
            title = stringResource(R.string.settings_privacy),
            message = stringResource(R.string.privacy_policy_text),
            onDismiss = { dialog = null },
        )
        SettingsDialog.TERMS -> InformationDialog(
            title = stringResource(R.string.settings_terms),
            message = stringResource(R.string.terms_text),
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TextSettingsDialog(
    settings: AppSettings,
    onDefaultFontChange: (BalloonFont) -> Unit,
    onHideFontSelectorChange: (Boolean) -> Unit,
    onTextSizeModeChange: (TextSizeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 560.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.settings_text)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingControl(
                    title = stringResource(R.string.default_text_font),
                    subtitle = stringResource(R.string.default_text_font_description),
                ) {
                    FontDropdown(selected = settings.defaultFont, onSelect = onDefaultFontChange)
                }
                SwitchRow(
                    title = stringResource(R.string.hide_font_selector),
                    subtitle = stringResource(R.string.hide_font_selector_description),
                    checked = settings.hideFontSelector,
                    onCheckedChange = onHideFontSelectorChange,
                )
                SettingControl(
                    title = stringResource(R.string.text_size),
                    subtitle = stringResource(R.string.text_size_description),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = settings.textSizeMode == TextSizeMode.MANUAL,
                            onClick = { onTextSizeModeChange(TextSizeMode.MANUAL) },
                            label = { Text(stringResource(R.string.manual)) },
                        )
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = settings.textSizeMode == TextSizeMode.AUTO,
                            onClick = { onTextSizeModeChange(TextSizeMode.AUTO) },
                            label = { Text(stringResource(R.string.auto)) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun SettingControl(title: String, subtitle: String, control: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = subtitle,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        control()
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun FontDropdown(selected: BalloonFont, onSelect: (BalloonFont) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(selected.label(), modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .heightIn(max = 320.dp),
        ) {
            selectableDefaultFonts.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label()) },
                    onClick = {
                        onSelect(entry)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun InformationDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

private fun launchIntent(context: android.content.Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}

private const val BUY_ME_A_COFFEE_URL = "https://www.buymeacoffee.com/"

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(
        uiState = SettingsUiState(AppSettings()),
        onNavigateBack = {},
        onDefaultFontChange = {},
        onHideFontSelectorChange = {},
        onTextSizeModeChange = {},
    )
}

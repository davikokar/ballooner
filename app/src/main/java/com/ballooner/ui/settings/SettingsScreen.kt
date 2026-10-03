package com.ballooner.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ballooner.R
import com.ballooner.data.settings.LocaleHelper
import com.ballooner.ui.theme.balloonerTopAppBarColors
import java.util.Locale

private enum class SettingsDialog { LANGUAGE, ABOUT, PRIVACY, TERMS }

@Composable
fun SettingsRoute(onNavigateBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.thanks.collect {
            Toast.makeText(context, R.string.billing_thanks, Toast.LENGTH_LONG).show()
        }
    }
    SettingsScreen(onNavigateBack = onNavigateBack, onBuyCoffee = viewModel::buyCoffee)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit, onBuyCoffee: (Activity) -> Unit = {}) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
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
                dialog = SettingsDialog.LANGUAGE
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
                activity?.let(onBuyCoffee)
            }
            HorizontalDivider()

            SectionHeader(stringResource(R.string.settings_legal))
            SettingsRow(Icons.Default.Lock, stringResource(R.string.settings_privacy)) {
                // The summary dialog stands in when the device has no browser to read the full text in.
                if (!launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))) {
                    dialog = SettingsDialog.PRIVACY
                }
            }
            SettingsRow(Icons.Default.Info, stringResource(R.string.settings_terms)) {
                if (!launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(TERMS_URL)))) {
                    dialog = SettingsDialog.TERMS
                }
            }
        }
    }

    when (dialog) {
        SettingsDialog.LANGUAGE -> LanguageDialog(
            current = LocaleHelper.languageTag(context),
            onChoose = { tag ->
                dialog = null
                if (tag != LocaleHelper.languageTag(context)) {
                    LocaleHelper.setLanguageTag(context, tag)
                    // Everything already on screen was built in the old language.
                    activity?.recreate()
                }
            },
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

/**
 * The languages the app is translated into, each written in its own language, plus the device's
 * own choice.
 */
@Composable
private fun LanguageDialog(current: String, onChoose: (String) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(current) }
    val tags = listOf(LocaleHelper.SYSTEM_LANGUAGE) + LocaleHelper.supportedLanguageTags
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_change_language)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .selectableGroup(),
            ) {
                tags.forEach { tag ->
                    LanguageRow(
                        label = languageName(tag),
                        selected = tag == chosen,
                        onSelect = { chosen = tag },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onChoose(chosen) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

/** A language names itself: "Deutsch", not "German", so it is readable whatever the app is set to. */
@Composable
private fun languageName(tag: String): String {
    if (tag.isEmpty()) return stringResource(R.string.language_system_default)
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
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

/** A composition's context is wrapped more than once, so the activity is found by unwrapping it. */
private tailrec fun android.content.Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val PRIVACY_POLICY_URL = "https://davikokar.github.io/android-docs/ballooner/privacy.html"
private const val TERMS_URL = "https://davikokar.github.io/android-docs/ballooner/terms.html"

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(onNavigateBack = {})
}

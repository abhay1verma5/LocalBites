package com.localbites.core.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.localbites.core.ui.R

@Composable
fun LanguageSwitcher(
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val currentLocale = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocale.isEmpty) {
        "en"
    } else {
        currentLocale[0]?.language ?: "en"
    }

    val displayLabel = if (currentLang == "hi") {
        stringResource(R.string.language_hindi)
    } else {
        stringResource(R.string.language_english)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = stringResource(R.string.switch_language),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = displayLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "English",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (currentLang == "en") FontWeight.Bold else FontWeight.Normal,
                        )
                        if (currentLang == "en") {
                            Text(
                                text = "\u2713",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                onClick = {
                    expanded = false
                    if (currentLang != "en") {
                        val localeList = LocaleListCompat.forLanguageTags("en")
                        AppCompatDelegate.setApplicationLocales(localeList)
                    }
                },
            )
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "\u0939\u093F\u0902\u0926\u0940",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (currentLang == "hi") FontWeight.Bold else FontWeight.Normal,
                        )
                        if (currentLang == "hi") {
                            Text(
                                text = "\u2713",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
                onClick = {
                    expanded = false
                    if (currentLang != "hi") {
                        val localeList = LocaleListCompat.forLanguageTags("hi")
                        AppCompatDelegate.setApplicationLocales(localeList)
                    }
                },
            )
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * The "rate us" card.
 *
 * Store ratings are the strongest organic discovery signal the app can earn (the stores rank
 * better-rated apps higher, and Cafe Bazaar even bars apps under 3 stars from search ads), but a
 * static Settings row is easy to ignore. This asks once, after the player has actually been used,
 * and offers a clean "later" / "never" so it never becomes nagging.
 */
@Composable
fun RatePromptDialog(
    onRateNow: () -> Unit,
    onLater: () -> Unit,
    onNever: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        modifier = Modifier.testTag("rate_prompt_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.rate_prompt_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.rate_prompt_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.rate_prompt_why),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onNever,
                    modifier = Modifier.testTag("rate_prompt_never")
                ) {
                    Text(
                        text = stringResource(R.string.rate_prompt_never),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onRateNow,
                modifier = Modifier.testTag("rate_prompt_yes")
            ) {
                Text(stringResource(R.string.rate_prompt_yes), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLater,
                modifier = Modifier.testTag("rate_prompt_later")
            ) {
                Text(stringResource(R.string.rate_prompt_later))
            }
        }
    )
}

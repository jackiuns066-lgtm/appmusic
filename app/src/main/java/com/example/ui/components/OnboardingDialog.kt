package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int
)

/**
 * First-run tour.
 *
 * A new user decides within seconds whether an app is worth keeping, and retention is one of the
 * signals the stores use when ranking search results. Three short cards point at the features that
 * make Novo different from every other player (offline + no ads, the pro widget, the equalizer and
 * lock-screen controls) instead of leaving the user to discover them.
 */
@Composable
fun OnboardingDialog(onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pages = remember {
        listOf(
            OnboardingPage(
                Icons.Default.Headphones,
                R.string.onboarding_1_title,
                R.string.onboarding_1_desc
            ),
            OnboardingPage(
                Icons.Default.AutoAwesome,
                R.string.onboarding_2_title,
                R.string.onboarding_2_desc
            ),
            OnboardingPage(
                Icons.Default.GraphicEq,
                R.string.onboarding_3_title,
                R.string.onboarding_3_desc
            )
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Dialog(
        onDismissRequest = { /* the tour is dismissed only through its own buttons */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_dialog")
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) { index ->
                    val page = pages[index]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = stringResource(page.titleRes),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(page.descriptionRes),
                            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Page indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.forEachIndexed { index, _ ->
                        val selected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (selected) 22.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.testTag("onboarding_skip")
                    ) {
                        Text(stringResource(R.string.onboarding_skip))
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (isLastPage) {
                                onFinish()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.testTag("onboarding_next")
                    ) {
                        Text(
                            stringResource(
                                if (isLastPage) R.string.onboarding_start else R.string.onboarding_next
                            )
                        )
                    }
                }

                // Small brand line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.poster_brand_line),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.poster_tagline),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

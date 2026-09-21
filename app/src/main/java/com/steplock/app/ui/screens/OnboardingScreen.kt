package com.steplock.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steplock.app.R
import com.steplock.app.ui.components.IconTile
import com.steplock.app.ui.components.PrimaryButton
import com.steplock.app.ui.components.SlDivider
import com.steplock.app.ui.components.SlIcons
import com.steplock.app.ui.components.SlPanel
import com.steplock.app.ui.theme.SlColor
import com.steplock.app.ui.theme.SlDimen
import com.steplock.app.ui.theme.SlText
import com.steplock.app.ui.theme.StepLockTheme

@Composable
fun OnboardingScreen(
    ctaText: String,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlColor.Background)
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp),
    ) {
        IconTile(
            icon = SlIcons.LockBadge,
            tint = SlColor.BrandDeep,
            background = SlColor.BrandTint,
            size = 72.dp,
            shape = RoundedCornerShape(24.dp),
            iconSize = 32.dp,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = SlText.AppTitle,
            color = SlColor.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_tagline),
            style = SlText.Tagline,
            color = SlColor.TextSecondary,
        )

        Spacer(Modifier.height(32.dp))
        SlPanel(
            shape = RoundedCornerShape(SlDimen.RadiusPanel),
            contentPadding = PaddingValues(20.dp),
        ) {
            UnlockRule(
                icon = SlIcons.Steps,
                title = stringResource(R.string.onboarding_steps_title),
                description = stringResource(R.string.onboarding_steps_desc),
            )
            SlDivider(modifier = Modifier.padding(vertical = 16.dp))
            UnlockRule(
                icon = SlIcons.Moon,
                title = stringResource(R.string.onboarding_sleep_title),
                description = stringResource(R.string.onboarding_sleep_desc),
            )
            SlDivider(modifier = Modifier.padding(vertical = 16.dp))
            UnlockRule(
                icon = SlIcons.Timer,
                title = stringResource(R.string.onboarding_pomodoro_title),
                description = stringResource(R.string.onboarding_pomodoro_desc),
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_caption),
            style = SlText.CaptionMedium,
            color = SlColor.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = ctaText, onClick = onCtaClick)
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.onboarding_privacy),
            style = SlText.Caption,
            color = SlColor.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UnlockRule(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconTile(
            icon = icon,
            tint = SlColor.BrandDeep,
            background = SlColor.SurfaceAlt,
            size = SlDimen.TouchTarget,
            shape = RoundedCornerShape(SlDimen.RadiusField),
            iconSize = 22.dp,
        )
        Column {
            Text(text = title, style = SlText.RowTitle, color = SlColor.TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(text = description, style = SlText.BodySm, color = SlColor.TextSecondary)
        }
    }
}

@Preview(widthDp = 412, heightDp = 892)
@Composable
private fun OnboardingScreenPreview() {
    StepLockTheme {
        OnboardingScreen(
            ctaText = stringResource(R.string.permission_cta_activity),
            onCtaClick = {},
        )
    }
}

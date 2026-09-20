package com.abrarshakhi.galva.common.ui.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.abrarshakhi.galva.common.ui.theme.GalvaDimens
import com.abrarshakhi.galva.core.permission.MediaAccess
import com.abrarshakhi.galva.core.permission.MediaPermissions

/**
 * Shows [content] once the app can read media, and a request screen until then.
 *
 * The gate reads the permission state synchronously on first composition rather than waiting for
 * the ViewModel to report it, so an already-granted app opens straight into the gallery instead of
 * flashing this screen for a frame. [onAccessChanged] still fires so the rest of the app can react.
 *
 * Access is re-checked on every resume, not just after the dialog, because the user can grant,
 * narrow or revoke it from system settings while the app is in the background.
 */
@Composable
fun MediaPermissionGate(
    onAccessChanged: (MediaAccess) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val currentOnAccessChanged = rememberUpdatedState(onAccessChanged)
    var access by remember { mutableStateOf(MediaPermissions.accessLevel(context)) }

    fun refreshAccess() {
        val latest = MediaPermissions.accessLevel(context)
        access = latest
        currentOnAccessChanged.value(latest)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshAccess()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshAccess() }

    if (access.canReadMedia) {
        content()
        return
    }

    val permissions = remember { MediaPermissions.required.toTypedArray() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(GalvaDimens.ScreenPadding * 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = "Galva needs access to your media",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = GalvaDimens.ItemSpacing),
        )
        Text(
            text = "Photos and videos are read from this device only. Nothing is uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = { launcher.launch(permissions) },
            modifier = Modifier.padding(top = GalvaDimens.SectionSpacing),
        ) {
            Text("Allow access")
        }
    }
}

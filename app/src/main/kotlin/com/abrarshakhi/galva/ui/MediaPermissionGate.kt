package com.abrarshakhi.galva.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.abrarshakhi.galva.core.designsystem.component.EmptyState
import com.abrarshakhi.galva.core.designsystem.component.Illustration
import com.abrarshakhi.galva.core.mediastore.MediaAccess
import com.abrarshakhi.galva.core.mediastore.MediaPermissions

@Composable
fun MediaPermissionGate(
    onAccessChanged: (MediaAccess) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val currentOnAccessChanged by rememberUpdatedState(onAccessChanged)
    var access by remember { mutableStateOf(MediaPermissions.accessLevel(context)) }

    fun refreshAccess() {
        val latest = MediaPermissions.accessLevel(context)
        access = latest
        currentOnAccessChanged(latest)
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshAccess() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshAccess() }

    if (access.canReadMedia) {
        content()
        return
    }

    val permissions = remember { MediaPermissions.required.toTypedArray() }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(modifier = Modifier.safeDrawingPadding()) {
            EmptyState(
                illustration = Illustration.EmptyGallery,
                title = "Your photos, right here",
                message = "Galva shows the photos and videos on this device. " +
                    "Nothing is uploaded, ever.",
                action = {
                    val height = ButtonDefaults.MediumContainerHeight
                    Button(
                        onClick = { launcher.launch(permissions) },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.heightIn(min = height),
                        contentPadding = ButtonDefaults.contentPaddingFor(height),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
                        )
                        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                        Text("Allow access", style = ButtonDefaults.textStyleFor(height))
                    }
                },
            )
        }
    }
}

package com.abrarshakhi.galva.core.ui.media

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.abrarshakhi.galva.core.ui.actions.ConsentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaDeleteLauncher internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val launchConsent: (IntentSenderRequest) -> Unit,
    private val reportResult: (confirmed: Boolean) -> Unit,
) {

    fun request(uris: List<String>) {
        if (uris.isEmpty()) return
        val parsed = uris.map(String::toUri)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pending = MediaStore.createDeleteRequest(context.contentResolver, parsed)
            launchConsent(IntentSenderRequest.Builder(pending.intentSender).build())
            return
        }

        scope.launch {
            val recovery = withContext(Dispatchers.IO) {
                runCatching {
                    parsed.forEach { context.contentResolver.delete(it, null, null) }
                    null
                }.getOrElse { error ->
                    (error as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender
                }
            }
            if (recovery == null) {
                reportResult(true)
            } else {
                launchConsent(IntentSenderRequest.Builder(recovery).build())
            }
        }
    }

    internal fun onConsentResult(resultCode: Int) {
        reportResult(resultCode == Activity.RESULT_OK)
    }
}

@Composable
fun rememberMediaDeleteLauncher(onResult: (confirmed: Boolean) -> Unit): MediaDeleteLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    val holder = remember { arrayOfNulls<MediaDeleteLauncher>(1) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        holder[0]?.onConsentResult(result.resultCode)
    }

    return remember(context, scope) {
        MediaDeleteLauncher(
            context = context,
            scope = scope,
            launchConsent = consentLauncher::launch,
            reportResult = { confirmed -> currentOnResult(confirmed) },
        ).also { holder[0] = it }
    }
}

@Composable
fun MediaConsentEffect(
    request: ConsentRequest?,
    onLaunched: (requestId: Long) -> Unit,
    onResult: (requestId: Long, confirmed: Boolean) -> Unit,
) {
    val currentRequest by rememberUpdatedState(request)
    val launcher = rememberMediaDeleteLauncher { confirmed ->
        currentRequest?.let { onResult(it.id, confirmed) }
    }
    if (request == null || request.launched) return
    LaunchedEffect(request.id) {
        launcher.request(request.uris)
        onLaunched(request.id)
    }
}

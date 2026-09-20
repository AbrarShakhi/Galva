package com.abrarshakhi.galva.core.media.ui

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaDeleteLauncher internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val launchConsent: (IntentSenderRequest) -> Unit,
    private val reportResult: (confirmed: Boolean, ids: List<Long>) -> Unit,
) {

    internal var pendingIds: List<Long> = emptyList()
        private set

    fun request(ids: List<Long>, uris: List<String>) {
        if (ids.isEmpty() || uris.isEmpty()) return
        pendingIds = ids
        val parsed = uris.map(String::toUri)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pending = android.provider.MediaStore.createDeleteRequest(
                context.contentResolver,
                parsed,
            )
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
                reportResult(true, pendingIds)
                pendingIds = emptyList()
            } else {
                launchConsent(IntentSenderRequest.Builder(recovery).build())
            }
        }
    }

    internal fun onConsentResult(resultCode: Int) {
        val ids = pendingIds
        pendingIds = emptyList()
        reportResult(resultCode == Activity.RESULT_OK, ids)
    }
}

@Composable
fun rememberMediaDeleteLauncher(
    onResult: (confirmed: Boolean, ids: List<Long>) -> Unit,
): MediaDeleteLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult = rememberUpdatedState(onResult)

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
            reportResult = { confirmed, ids -> currentOnResult.value(confirmed, ids) },
        ).also { holder[0] = it }
    }
}

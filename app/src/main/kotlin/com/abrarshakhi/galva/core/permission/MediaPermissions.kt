package com.abrarshakhi.galva.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** How much of the device's media the user has let the app see. */
enum class MediaAccess {
    /** Every photo and video is visible. */
    FULL,

    /** Android 14+ "Select photos": only user-picked items are visible. */
    PARTIAL,

    DENIED,
    ;

    val canReadMedia: Boolean get() = this != DENIED
}

/**
 * Resolves the right media permissions for the running OS version.
 *
 * Android 13 split storage access into per-type media permissions, and Android 14 added a partial
 * grant on top. Asking for the wrong set is silently denied, so the request list is derived from
 * [Build.VERSION.SDK_INT] in one place instead of at each call site.
 */
object MediaPermissions {

    val required: List<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
        )

        else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun accessLevel(context: Context): MediaAccess = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            val images = context.isGranted(Manifest.permission.READ_MEDIA_IMAGES)
            val video = context.isGranted(Manifest.permission.READ_MEDIA_VIDEO)
            val partial = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                context.isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            when {
                images && video -> MediaAccess.FULL
                images || video || partial -> MediaAccess.PARTIAL
                else -> MediaAccess.DENIED
            }
        }

        context.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE) -> MediaAccess.FULL

        else -> MediaAccess.DENIED
    }

    private fun Context.isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

package com.abrarshakhi.galva.core.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class MediaAccess {
    FULL, PARTIAL, DENIED;

    val canReadMedia: Boolean get() = this != DENIED
}

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
            val partial =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && context.isGranted(
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
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

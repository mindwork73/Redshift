@file:JvmName("VideoBackgroundKt")

package com.example.ui

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.BackgroundNavy
import com.example.ui.theme.BackgroundNavyDeep

/**
 * Looped video background using AndroidView + MediaPlayer.
 * Renders planet.mp4 from res/raw as a fullscreen looping background
 * with a gradient scrim overlay for text readability.
 */
@Composable
fun VideoBackground(
    videoResId: Int = R.raw.planet_bg,
    scrimOpacity: Float = 0.55f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // Video layer
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            val surface = Surface(surfaceTexture)
                            val player = MediaPlayer().apply {
                                setDataSource(
                                    ctx.resources.openRawResourceFd(videoResId).let { fd ->
                                        fd.fileDescriptor
                                    },
                                    0,
                                    ctx.resources.openRawResourceFd(videoResId).length
                                )
                                isLooping = true
                                setSurface(surface)
                                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                setOnPreparedListener { mp ->
                                    mp.start()
                                }
                                prepareAsync()
                            }
                            mediaPlayer = player
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {}

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            mediaPlayer?.release()
                            mediaPlayer = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = {}
        )

        // Gradient scrim overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BackgroundNavy.copy(alpha = scrimOpacity * 0.7f),
                            BackgroundNavy.copy(alpha = scrimOpacity * 0.3f),
                            BackgroundNavy.copy(alpha = scrimOpacity * 0.5f),
                            BackgroundNavyDeep.copy(alpha = scrimOpacity * 0.95f)
                        ),
                        colorStops = arrayOf(
                            0.0f to BackgroundNavy.copy(alpha = scrimOpacity * 0.7f),
                            0.3f to BackgroundNavy.copy(alpha = scrimOpacity * 0.2f),
                            0.6f to BackgroundNavy.copy(alpha = scrimOpacity * 0.5f),
                            1.0f to BackgroundNavyDeep.copy(alpha = scrimOpacity * 0.95f)
                        )
                    )
                )
        )
    }

    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }
}

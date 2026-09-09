@file:JvmName("VideoBackgroundKt")

package com.example.ui

import android.content.res.AssetFileDescriptor
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.theme.BackgroundNavy
import com.example.ui.theme.BackgroundNavyDeep

/**
 * Fullscreen looping video background based on a TextureView + MediaPlayer.
 * The video is muted and cropped to fill the screen.
 */
@Composable
fun VideoBackground(
    videoResId: Int = R.raw.planet_bg,
    scrimOpacity: Float = 0.55f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var surface by remember { mutableStateOf<Surface?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) {
                            val textureSurface = Surface(surfaceTexture)
                            surface = textureSurface

                            val afd: AssetFileDescriptor = ctx.resources.openRawResourceFd(videoResId)
                            val player = MediaPlayer().apply {
                                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                                afd.close()
                                setSurface(textureSurface)
                                isLooping = true
                                setVolume(0f, 0f)
                                setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                                setOnPreparedListener { prepared ->
                                    prepared.start()
                                }
                                prepareAsync()
                            }
                            mediaPlayer = player
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int
                        ) = Unit

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            mediaPlayer?.release()
                            mediaPlayer = null
                            surface?.release()
                            surface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = {}
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to BackgroundNavy.copy(alpha = scrimOpacity * 0.70f),
                            0.30f to BackgroundNavy.copy(alpha = scrimOpacity * 0.20f),
                            0.60f to BackgroundNavy.copy(alpha = scrimOpacity * 0.50f),
                            1.0f to BackgroundNavyDeep.copy(alpha = scrimOpacity * 0.95f)
                        )
                    )
                )
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            surface?.release()
        }
    }
}

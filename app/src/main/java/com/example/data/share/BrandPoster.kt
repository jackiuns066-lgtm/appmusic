package com.example.data.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.local.TrackEntity
import com.example.ui.components.AudioCoverLoader
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Builds a ready-to-post "now playing" poster (1080 x 1920, story format) with the album art, the
 * song title / artist and the Novo + webnovo.ir branding, then hands it to the Android share sheet.
 *
 * Posters are the cheapest organic reach the app has: every story a user posts carries the brand
 * to their whole follower list. Files are written to the cache directory and shared through the
 * FileProvider that is already declared in the manifest.
 */
object BrandPoster {

    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val COVER_SIZE = 720f
    private const val COVER_TOP = 320f
    private const val CORNER = 44f
    private const val MAX_TEXT_WIDTH = 900f

    private const val BACKGROUND_TOP = 0xFF0B0B12.toInt()
    private const val BACKGROUND_BOTTOM = 0xFF05050A.toInt()

    /**
     * Renders the poster and opens the share sheet. Returns false when nothing could be shared so
     * the caller can show an error message.
     */
    suspend fun share(
        context: Context,
        track: TrackEntity,
        accentColor: Int,
        chooserTitle: String,
        subject: String,
        text: String,
        brandLine: String,
        tagline: String
    ): Boolean = withContext(Dispatchers.IO) {
        val file = try {
            renderToFile(context, track, accentColor, brandLine, tagline)
        } catch (_: Throwable) {
            null
        } ?: return@withContext false

        val uri = try {
            FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", file)
        } catch (_: Throwable) {
            return@withContext false
        }

        withContext(Dispatchers.Main) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(
                    Intent.createChooser(sendIntent, chooserTitle)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
                true
            } catch (_: ActivityNotFoundException) {
                false
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun renderToFile(
        context: Context,
        track: TrackEntity,
        accentColor: Int,
        brandLine: String,
        tagline: String
    ): File? {
        val bitmap = buildPoster(context, track, accentColor, brandLine, tagline) ?: return null

        val directory = File(context.cacheDir, "posters").apply { mkdirs() }
        cleanUpOldPosters(directory)

        val file = File(directory, "novo_poster_${track.id.hashCode()}.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file
    }

    private fun cleanUpOldPosters(directory: File) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        try {
            directory.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff) file.delete()
            }
        } catch (_: Throwable) {
        }
    }

    private fun buildPoster(
        context: Context,
        track: TrackEntity,
        accentColor: Int,
        brandLine: String,
        tagline: String
    ): Bitmap? {
        val accent = if (isUsableColor(accentColor)) accentColor else 0xFFFFC107.toInt()
        val bitmap = try {
            Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) {
            return null
        }

        val canvas = Canvas(bitmap)
        drawBackground(canvas, accent)

        val left = (WIDTH - COVER_SIZE) / 2f
        val top = COVER_TOP
        val right = left + COVER_SIZE
        val bottom = top + COVER_SIZE
        drawCover(canvas, context, track, accent, left, top, right, bottom)

        drawTexts(canvas, accent, track, brandLine, tagline)
        return bitmap
    }

    private fun isUsableColor(color: Int): Boolean = Color.alpha(color) > 0

    private fun drawBackground(canvas: Canvas, accent: Int) {
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                0f,
                HEIGHT.toFloat(),
                intArrayOf(BACKGROUND_TOP, darken(accent, 0.22f), BACKGROUND_BOTTOM),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), background)

        // soft accent glow behind the artwork
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                WIDTH / 2f,
                COVER_TOP + COVER_SIZE / 2f,
                COVER_SIZE * 0.85f,
                intArrayOf(withAlpha(accent, 0.30f), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), glow)
    }

    private fun drawCover(
        canvas: Canvas,
        context: Context,
        track: TrackEntity,
        accent: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {
        val art = try {
            AudioCoverLoader.loadArtworkSync(context, track)
        } catch (_: Throwable) {
            null
        }

        // soft shadow
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000 }
        canvas.drawRoundRect(RectF(left + 8f, top + 22f, right + 8f, bottom + 22f), CORNER, CORNER, shadow)
        val shadowSoft = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40000000 }
        canvas.drawRoundRect(RectF(left - 4f, top + 10f, right + 4f, bottom + 10f), CORNER, CORNER, shadowSoft)

        val frame = RectF(left, top, right, bottom)
        val shape = Path().apply { addRoundRect(frame, CORNER, CORNER, Path.Direction.CW) }

        if (art != null && !art.isRecycled) {
            val square = squareCrop(art)
            val save = canvas.save()
            canvas.clipPath(shape)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isFilterBitmap = true
                isDither = true
            }
            canvas.drawBitmap(square, null, frame, paint)
            canvas.restoreToCount(save)
        } else {
            val placeholder = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(accent, 0.18f) }
            canvas.drawRoundRect(frame, CORNER, CORNER, placeholder)
            val note = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                textAlign = Paint.Align.CENTER
                textSize = 260f
            }
            canvas.drawText("\u266A", WIDTH / 2f, top + COVER_SIZE * 0.62f, note)
        }

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = withAlpha(accent, 0.85f)
        }
        canvas.drawRoundRect(frame, CORNER, CORNER, border)
    }

    private fun drawTexts(
        canvas: Canvas,
        accent: Int,
        track: TrackEntity,
        brandLine: String,
        tagline: String
    ) {
        val centerX = WIDTH / 2f

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 62f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            letterSpacing = 0.01f
        }
        drawCentered(canvas, track.title, titlePaint, centerX, 1215f)

        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            textSize = 44f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        drawCentered(canvas, track.artist, artistPaint, centerX, 1295f)

        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRoundRect(RectF(centerX - 70f, 1350f, centerX + 70f, 1358f), 4f, 4f, divider)

        val taglinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x99FFFFFF.toInt()
            textSize = 36f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        drawCentered(canvas, tagline, taglinePaint, centerX, 1435f, maxWidth = 860f)

        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 46f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.14f
        }
        drawCentered(canvas, brandLine, brandPaint, centerX, 1655f, maxWidth = 860f)

        val sitePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 44f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            letterSpacing = 0.06f
        }
        drawCentered(canvas, "webnovo.ir", sitePaint, centerX, 1725f, maxWidth = 860f)
    }

    private fun drawCentered(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        centerX: Float,
        baselineY: Float,
        maxWidth: Float = MAX_TEXT_WIDTH
    ) {
        val value = text.trim().ifEmpty { "-" }
        val fitted = TextUtils.ellipsize(value, paint, maxWidth, TextUtils.TruncateAt.END).toString()
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(fitted, centerX, baselineY, paint)
    }

    private fun squareCrop(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        if (size <= 0) return source
        if (size == source.width && size == source.height) return source
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2
        return try {
            Bitmap.createBitmap(source, x, y, size, size)
        } catch (_: Throwable) {
            source
        }
    }

    private fun darken(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color) * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    )

    private fun withAlpha(color: Int, alpha: Float): Int = Color.argb(
        (alpha.coerceIn(0f, 1f) * 255).toInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color)
    )
}

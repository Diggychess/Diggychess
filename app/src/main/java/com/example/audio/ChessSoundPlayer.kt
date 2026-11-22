package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.theme.LocalHapticsEnabled
import com.example.ui.theme.LocalSoundEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Low-latency SoundPool + acoustic wooden Staunton synthesis engine.
 * Pre-renders crisp physical wood-knock, capture-clack, castle double-tap,
 * check alert, pickup tap, wrong-move thud, and game-end chimes as 16-bit WAVs
 * in app cacheDir with an AudioTrack static fallback.
 */
object ChessSoundPlayer {
    internal const val SAMPLE_RATE = 22050
    private const val SFX_VERSION = "v4"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var soundPool: SoundPool? = null
    @Volatile private var initialized = false
    @Volatile private var moveId = 0
    @Volatile private var captureId = 0
    @Volatile private var captureCheckId = 0
    @Volatile private var castleId = 0
    @Volatile private var checkId = 0
    @Volatile private var checkmateId = 0
    @Volatile private var drawId = 0
    @Volatile private var pickupId = 0
    @Volatile private var wrongId = 0
    @Volatile private var gameEndId = 0
    private val loadedIds = mutableSetOf<Int>()

    internal val moveBuffer: ShortArray by lazy { synthesizeWoodMove() }
    internal val captureBuffer: ShortArray by lazy { synthesizeWoodCapture() }
    internal val captureCheckBuffer: ShortArray by lazy { synthesizeCaptureCheck() }
    internal val castleBuffer: ShortArray by lazy { synthesizeWoodCastle() }
    internal val checkBuffer: ShortArray by lazy { synthesizeCheckAlert() }
    internal val checkmateBuffer: ShortArray by lazy { synthesizeCheckmateFanfare() }
    internal val drawBuffer: ShortArray by lazy { synthesizeDrawCadence() }
    internal val pickupBuffer: ShortArray by lazy { synthesizePickupTap() }
    internal val wrongMoveBuffer: ShortArray by lazy { synthesizeWrongMoveThud() }
    internal val gameEndBuffer: ShortArray by lazy { checkmateBuffer }

    fun ensureInit(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            val appCtx = context.applicationContext
            scope.launch {
                runCatching {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    val pool = SoundPool.Builder()
                        .setMaxStreams(8)
                        .setAudioAttributes(attrs)
                        .build()
                    pool.setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            synchronized(loadedIds) { loadedIds.add(sampleId) }
                        }
                    }

                    fun loadWav(name: String, samples: ShortArray): Int {
                        val file = File(appCtx.cacheDir, "diggy_sfx_${SFX_VERSION}_$name.wav")
                        if (!file.exists() || file.length() < 64L) {
                            file.writeBytes(buildWavBytes(samples, SAMPLE_RATE))
                        }
                        return pool.load(file.absolutePath, 1)
                    }

                    moveId = loadWav("move", moveBuffer)
                    captureId = loadWav("capture", captureBuffer)
                    captureCheckId = loadWav("capture_check", captureCheckBuffer)
                    castleId = loadWav("castle", castleBuffer)
                    checkId = loadWav("check", checkBuffer)
                    checkmateId = loadWav("checkmate", checkmateBuffer)
                    drawId = loadWav("draw", drawBuffer)
                    pickupId = loadWav("pickup", pickupBuffer)
                    wrongId = loadWav("wrong", wrongMoveBuffer)
                    gameEndId = checkmateId
                    soundPool = pool
                }
            }
        }
    }

    fun playMove() = playCue(moveId, moveBuffer, volume = 0.85f)
    fun playCapture() = playCue(captureId, captureBuffer, volume = 0.96f)
    fun playCaptureCheck() = playCue(captureCheckId, captureCheckBuffer, volume = 0.96f)
    fun playCastle() = playCue(castleId, castleBuffer, volume = 0.88f)
    fun playCheck() = playCue(checkId, checkBuffer, volume = 0.90f)
    fun playCheckmate() = playCue(checkmateId, checkmateBuffer, volume = 0.95f)
    fun playDraw() = playCue(drawId, drawBuffer, volume = 0.84f)
    fun playPickup() = playCue(pickupId, pickupBuffer, volume = 0.36f)
    fun playWrong() = playCue(wrongId, wrongMoveBuffer, volume = 0.78f)
    fun playGameEnd() = playCheckmate()

    private fun playCue(poolSoundId: Int, fallbackSamples: ShortArray, volume: Float) {
        val pool = soundPool
        val isReady = pool != null && poolSoundId != 0 && synchronized(loadedIds) { poolSoundId in loadedIds }
        if (isReady && pool != null) {
            val streamId = runCatching {
                pool.play(poolSoundId, volume, volume, 1, 0, 1.0f)
            }.getOrDefault(0)
            if (streamId != 0) return
        }
        playFallbackAsync(fallbackSamples)
    }

    private fun playFallbackAsync(samples: ShortArray) {
        scope.launch {
            runCatching {
                val byteCount = samples.size * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(byteCount)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(samples, 0, samples.size)
                track.play()
                val playMs = (samples.size * 1000L) / SAMPLE_RATE + 40L
                kotlinx.coroutines.delay(playMs)
                track.stop()
                track.release()
            }
        }
    }

    internal fun buildWavBytes(samples: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
        val dataSize = samples.size * 2
        val totalSize = 44 + dataSize
        val bytes = ByteArray(totalSize)

        fun writeAscii(offset: Int, text: String) {
            for (i in text.indices) bytes[offset + i] = text[i].code.toByte()
        }

        fun writeIntLe(offset: Int, value: Int) {
            bytes[offset] = (value and 0xFF).toByte()
            bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
            bytes[offset + 2] = ((value ushr 16) and 0xFF).toByte()
            bytes[offset + 3] = ((value ushr 24) and 0xFF).toByte()
        }

        fun writeShortLe(offset: Int, value: Int) {
            bytes[offset] = (value and 0xFF).toByte()
            bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        }

        writeAscii(0, "RIFF")
        writeIntLe(4, 36 + dataSize)
        writeAscii(8, "WAVE")
        writeAscii(12, "fmt ")
        writeIntLe(16, 16)             // PCM chunk size
        writeShortLe(20, 1)            // PCM format
        writeShortLe(22, 1)            // Mono
        writeIntLe(24, sampleRate)
        writeIntLe(28, sampleRate * 2) // Byte rate
        writeShortLe(32, 2)            // Block align
        writeShortLe(34, 16)           // Bits per sample
        writeAscii(36, "data")
        writeIntLe(40, dataSize)

        var pos = 44
        for (s in samples) {
            val v = s.toInt()
            bytes[pos] = (v and 0xFF).toByte()
            bytes[pos + 1] = ((v ushr 8) and 0xFF).toByte()
            pos += 2
        }
        return bytes
    }

    private fun woodKnockSample(
        t: Double,
        noiseFiltered: Double,
        baseHz: Double,
        gain: Double,
        hardness: Double = 0.72
    ): Double {
        if (t < 0.0) return 0.0
        val attack = 1.0 - exp(-520.0 * t)
        val transientEnv = exp(-210.0 * t) * hardness
        val transient = (noiseFiltered * 0.42 + sin(2.0 * PI * (baseHz * 3.2) * t) * 0.30) * transientEnv
        val body1 = sin(2.0 * PI * baseHz * t) * exp(-44.0 * t)
        val body2 = 0.46 * sin(2.0 * PI * (baseHz * 1.62) * t) * exp(-68.0 * t)
        val body3 = 0.14 * sin(2.0 * PI * (baseHz * 2.55) * t) * exp(-105.0 * t)
        return (transient + body1 + body2 + body3) * attack * gain
    }

    private fun filteredNoiseStream(count: Int, seed: Int): DoubleArray {
        val rng = Random(seed)
        val out = DoubleArray(count)
        var prev1 = 0.0
        var prev2 = 0.0
        for (i in 0 until count) {
            val raw = rng.nextDouble(-1.0, 1.0)
            // Warm smoothed wood-grain noise without harsh hiss
            val lp = 0.35 * raw + 0.40 * prev1 + 0.25 * prev2
            out[i] = lp - prev2 * 0.28
            prev2 = prev1
            prev1 = lp
        }
        return out
    }

    private fun toShortPcm(samples: DoubleArray): ShortArray =
        ShortArray(samples.size) { i ->
            (samples[i].coerceIn(-0.98, 0.98) * Short.MAX_VALUE).toInt().toShort()
        }

    // Warm Chess.com-style felt-on-wood move tap (~75ms)
    private fun synthesizeWoodMove(): ShortArray {
        val count = (SAMPLE_RATE * 78) / 1000
        val noise = filteredNoiseStream(count, seed = 101)
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            woodKnockSample(t = t, noiseFiltered = noise[i], baseHz = 168.0, gain = 0.64, hardness = 0.68)
        }
        return toShortPcm(raw)
    }

    // Crisp double-wood clack with punchy boxwood resonance (~160ms)
    private fun synthesizeWoodCapture(): ShortArray {
        val count = (SAMPLE_RATE * 162) / 1000
        val noise = filteredNoiseStream(count, seed = 202)
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val strikePiece = woodKnockSample(
                t = t,
                noiseFiltered = noise[i],
                baseHz = 315.0,
                gain = 0.56,
                hardness = 1.15
            )
            val dt = t - 0.036
            val landResonance = if (dt >= 0.0) {
                val baseBody = woodKnockSample(
                    t = dt,
                    noiseFiltered = noise[i],
                    baseHz = 132.0,
                    gain = 0.84,
                    hardness = 0.98
                )
                val warmLowTail = sin(2.0 * PI * 116.0 * dt) * exp(-28.0 * dt) * (1.0 - exp(-220.0 * dt)) * 0.24
                baseBody + warmLowTail
            } else 0.0
            strikePiece + landResonance
        }
        return toShortPcm(raw)
    }

    // Double-wood capture clack flowing into warm brass check chime (~230ms)
    private fun synthesizeCaptureCheck(): ShortArray {
        val count = (SAMPLE_RATE * 230) / 1000
        val noise = filteredNoiseStream(count, seed = 252)
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val strikePiece = woodKnockSample(t = t, noiseFiltered = noise[i], baseHz = 310.0, gain = 0.52, hardness = 1.1)
            val landResonance = woodKnockSample(t = t - 0.034, noiseFiltered = noise[i], baseHz = 136.0, gain = 0.72, hardness = 0.92)
            val pingT = t - 0.042
            val ping = if (pingT >= 0.0) {
                val pingEnv = exp(-19.0 * pingT) * (1.0 - exp(-260.0 * pingT))
                (sin(2.0 * PI * 740.0 * pingT) * 0.55 +
                    sin(2.0 * PI * 1108.73 * pingT) * 0.35 +
                    sin(2.0 * PI * 1480.0 * pingT) * 0.15) * pingEnv * 0.36
            } else 0.0
            strikePiece + landResonance + ping
        }
        return toShortPcm(raw)
    }

    // Smooth two-piece castling tap-tap (~200ms)
    private fun synthesizeWoodCastle(): ShortArray {
        val count = (SAMPLE_RATE * 205) / 1000
        val noise = filteredNoiseStream(count, seed = 303)
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val kingTap = woodKnockSample(t = t, noiseFiltered = noise[i], baseHz = 166.0, gain = 0.58, hardness = 0.68)
            val rookTap = woodKnockSample(t = t - 0.084, noiseFiltered = noise[i], baseHz = 198.0, gain = 0.56, hardness = 0.74)
            kingTap + rookTap
        }
        return toShortPcm(raw)
    }

    // Ultra-soft felt pickup tap (~28ms)
    private fun synthesizePickupTap(): ShortArray {
        val count = (SAMPLE_RATE * 30) / 1000
        val noise = filteredNoiseStream(count, seed = 404)
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            woodKnockSample(t = t, noiseFiltered = noise[i], baseHz = 235.0, gain = 0.20, hardness = 0.35)
        }
        return toShortPcm(raw)
    }

    // Crisp wood placement + bright brass check chime (~180ms)
    private fun synthesizeCheckAlert(): ShortArray {
        val count = (SAMPLE_RATE * 185) / 1000
        val noise = filteredNoiseStream(count, seed = 505)
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val knock = woodKnockSample(t = t, noiseFiltered = noise[i], baseHz = 195.0, gain = 0.50, hardness = 1.1)
            val pingEnv = exp(-20.0 * t) * (1.0 - exp(-280.0 * t))
            val ping = (sin(2.0 * PI * 740.0 * t) * 0.55 +
                sin(2.0 * PI * 1108.73 * t) * 0.35 +
                sin(2.0 * PI * 1480.0 * t) * 0.15) * pingEnv * 0.38
            knock + ping
        }
        return toShortPcm(raw)
    }

    // Low muted wooden double-knock for illegal/wrong puzzle moves (~150ms)
    private fun synthesizeWrongMoveThud(): ShortArray {
        val count = (SAMPLE_RATE * 155) / 1000
        val noise = filteredNoiseStream(count, seed = 606)
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val first = woodKnockSample(t = t, noiseFiltered = noise[i], baseHz = 135.0, gain = 0.45, hardness = 0.5)
            val second = woodKnockSample(t = t - 0.062, noiseFiltered = noise[i], baseHz = 112.0, gain = 0.48, hardness = 0.45)
            first + second
        }
        return toShortPcm(raw)
    }

    // Decisive wood slam + warm brass fanfare chord for checkmate (~540ms)
    private fun synthesizeCheckmateFanfare(): ShortArray {
        val count = (SAMPLE_RATE * 540) / 1000
        val noise = filteredNoiseStream(count, seed = 707)
        fun warmBrassVoice(dt: Double, freq: Double, decay: Double): Double {
            if (dt <= 0.0) return 0.0
            val env = (1.0 - exp(-95.0 * dt)) * exp(-decay * dt)
            val fund = sin(2.0 * PI * freq * dt)
            val second = 0.28 * sin(2.0 * PI * (freq * 2.0) * dt)
            val third = 0.10 * sin(2.0 * PI * (freq * 3.0) * dt)
            return (fund + second + third) * env
        }
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val slam = woodKnockSample(t = t, noiseFiltered = noise[i], baseHz = 145.0, gain = 0.68, hardness = 1.25)
            val snap = woodKnockSample(t = t - 0.022, noiseFiltered = noise[i], baseHz = 230.0, gain = 0.36, hardness = 0.95)
            val c5 = warmBrassVoice(t - 0.015, 523.25, 5.2) * 0.22
            val e5 = warmBrassVoice(t - 0.068, 659.25, 5.0) * 0.21
            val g5 = warmBrassVoice(t - 0.120, 783.99, 4.6) * 0.22
            val c6 = warmBrassVoice(t - 0.175, 1046.50, 4.2) * 0.20
            val g4Warm = warmBrassVoice(t - 0.175, 392.00, 4.5) * 0.16
            slam + snap + c5 + e5 + g5 + c6 + g4Warm
        }
        return toShortPcm(raw)
    }

    // Neutral warm wooden two-note cadence for stalemate & draws (~340ms)
    private fun synthesizeDrawCadence(): ShortArray {
        val count = (SAMPLE_RATE * 340) / 1000
        val noise = filteredNoiseStream(count, seed = 808)
        fun warmWoodBell(dt: Double, freq: Double): Double {
            if (dt <= 0.0) return 0.0
            val env = (1.0 - exp(-140.0 * dt)) * exp(-8.8 * dt)
            return (sin(2.0 * PI * freq * dt) + 0.18 * sin(2.0 * PI * (freq * 2.0) * dt)) * env
        }
        val raw = DoubleArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val tap = woodKnockSample(t = t, noiseFiltered = noise[i], baseHz = 165.0, gain = 0.45, hardness = 0.72)
            val first = warmWoodBell(t - 0.020, 587.33) * 0.23
            val second = warmWoodBell(t - 0.125, 440.00) * 0.25
            tap + first + second
        }
        return toShortPcm(raw)
    }
}

class ChessFeedbackController(
    private val soundEnabled: Boolean,
    private val hapticsEnabled: Boolean,
    private val hapticFeedback: HapticFeedback
) {
    fun onMove(
        isCapture: Boolean = false,
        isCheck: Boolean = false,
        isCastle: Boolean = false,
        isCheckmate: Boolean = false,
        isDraw: Boolean = false,
        isGameOver: Boolean = false
    ) {
        val checkmateEvent = isCheckmate || (isGameOver && !isDraw)
        if (soundEnabled) {
            when {
                checkmateEvent -> ChessSoundPlayer.playCheckmate()
                isDraw -> ChessSoundPlayer.playDraw()
                isCheck && isCapture -> ChessSoundPlayer.playCaptureCheck()
                isCheck -> ChessSoundPlayer.playCheck()
                isCastle -> ChessSoundPlayer.playCastle()
                isCapture -> ChessSoundPlayer.playCapture()
                else -> ChessSoundPlayer.playMove()
            }
        }
        if (hapticsEnabled) {
            val type = if (isCapture || isCheck || checkmateEvent || isDraw) {
                HapticFeedbackType.LongPress
            } else {
                HapticFeedbackType.TextHandleMove
            }
            hapticFeedback.performHapticFeedback(type)
        }
    }

    fun onPickup() {
        if (soundEnabled) ChessSoundPlayer.playPickup()
        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun onWrongMove() {
        if (soundEnabled) ChessSoundPlayer.playWrong()
        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun onTap() {
        if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

@Composable
fun rememberChessFeedback(): ChessFeedbackController {
    val context = LocalContext.current
    val sound = LocalSoundEnabled.current
    val haptics = LocalHapticsEnabled.current
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(context) {
        ChessSoundPlayer.ensureInit(context)
    }
    return remember(sound, haptics, hapticFeedback) {
        ChessFeedbackController(sound, haptics, hapticFeedback)
    }
}

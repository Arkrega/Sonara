package com.sonara.music.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.sonara.music.utils.InnerTubeXPlayer
import java.io.IOException

@OptIn(UnstableApi::class)
internal class ChunkedRangeDataSource(
    private val upstream: DataSource,
    private val streamUrl: String,
    private val requestHeaders: Map<String, String>,
    private val requireBoundedRange: Boolean,
    private val useRangeChunks: Boolean,
    private val rangeChunkSizeBytes: Long,
    private val initialContentLengthBytes: Long?,
) : DataSource {

    class Factory(
        private val upstreamFactory: DataSource.Factory,
        private val playbackData: InnerTubeXPlayer.VideoPlaybackData,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            ChunkedRangeDataSource(
                upstream = upstreamFactory.createDataSource(),
                streamUrl = playbackData.streamUrl,
                requestHeaders = playbackData.streamHeaders,
                requireBoundedRange = playbackData.requireBoundedRange,
                useRangeChunks = playbackData.useRangeChunks,
                rangeChunkSizeBytes = playbackData.rangeChunkSizeBytes,
                initialContentLengthBytes = playbackData.contentLengthBytes,
            )
    }

    private var baseSpec: DataSpec? = null
    private var upstreamOpened = false
    private var chunkedMode = false
    private var nextPosition = 0L
    private var bytesReadTotal = 0L
    private var requestedLength = C.LENGTH_UNSET.toLong()
    private var knownTotalLength = C.LENGTH_UNSET.toLong()
    private var currentChunkRequestedLength = C.LENGTH_UNSET.toLong()
    private var currentChunkBytesRead = 0L

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        closeUpstreamQuietly()
        val resolvedSpec =
            dataSpec
                .withUri(streamUrl.toUri())
                .withRequestHeaders(dataSpec.httpRequestHeaders + requestHeaders)
        baseSpec = resolvedSpec
        nextPosition = resolvedSpec.position
        bytesReadTotal = 0L
        requestedLength = resolvedSpec.length
        knownTotalLength = initialContentLengthBytes?.takeIf { it > 0L } ?: C.LENGTH_UNSET.toLong()
        chunkedMode = (requireBoundedRange || useRangeChunks) && rangeChunkSizeBytes > 0L

        if (!chunkedMode) {
            val openedLength = upstream.open(resolvedSpec)
            upstreamOpened = true
            return openedLength
        }

        val openedFirstChunk = openNextChunk()
        if (!openedFirstChunk) {
            return 0L
        }

        return when {
            requestedLength != C.LENGTH_UNSET.toLong() -> {
                if (knownTotalLength != C.LENGTH_UNSET.toLong()) {
                    minOf(requestedLength, (knownTotalLength - resolvedSpec.position).coerceAtLeast(0L))
                } else {
                    requestedLength
                }
            }

            knownTotalLength != C.LENGTH_UNSET.toLong() -> {
                (knownTotalLength - resolvedSpec.position).coerceAtLeast(0L)
            }

            else -> C.LENGTH_UNSET.toLong()
        }
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (!chunkedMode) {
            return upstream.read(buffer, offset, length)
        }
        if (requestedLength != C.LENGTH_UNSET.toLong() && bytesReadTotal >= requestedLength) {
            return C.RESULT_END_OF_INPUT
        }

        while (true) {
            if (!upstreamOpened) {
                return C.RESULT_END_OF_INPUT
            }
            val readCount = upstream.read(buffer, offset, length)
            if (readCount != C.RESULT_END_OF_INPUT) {
                bytesReadTotal += readCount
                nextPosition += readCount
                currentChunkBytesRead += readCount
                return readCount
            }

            if (requestedLength != C.LENGTH_UNSET.toLong() && bytesReadTotal >= requestedLength) {
                return C.RESULT_END_OF_INPUT
            }
            if (knownTotalLength != C.LENGTH_UNSET.toLong() && nextPosition >= knownTotalLength) {
                return C.RESULT_END_OF_INPUT
            }
            if (currentChunkBytesRead <= 0L) {
                return C.RESULT_END_OF_INPUT
            }
            if (currentChunkRequestedLength != C.LENGTH_UNSET.toLong() && currentChunkBytesRead < currentChunkRequestedLength) {
                return C.RESULT_END_OF_INPUT
            }

            closeUpstreamQuietly()
            if (!openNextChunk()) {
                return C.RESULT_END_OF_INPUT
            }
        }
    }

    override fun getUri(): Uri? = upstream.uri ?: baseSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    @Throws(IOException::class)
    override fun close() {
        baseSpec = null
        if (upstreamOpened) {
            upstreamOpened = false
            upstream.close()
        }
    }

    @Throws(IOException::class)
    private fun openNextChunk(): Boolean {
        val spec = baseSpec ?: return false
        var chunkLength = rangeChunkSizeBytes
        if (requestedLength != C.LENGTH_UNSET.toLong()) {
            val remainingRequested = requestedLength - bytesReadTotal
            if (remainingRequested <= 0L) return false
            chunkLength = minOf(chunkLength, remainingRequested)
        }
        if (knownTotalLength != C.LENGTH_UNSET.toLong()) {
            val remainingTotal = knownTotalLength - nextPosition
            if (remainingTotal <= 0L) return false
            chunkLength = minOf(chunkLength, remainingTotal)
        }
        if (chunkLength <= 0L) return false

        val chunkSpec =
            spec
                .buildUpon()
                .setPosition(nextPosition)
                .setLength(chunkLength)
                .build()
        currentChunkRequestedLength = chunkLength
        currentChunkBytesRead = 0L
        upstream.open(chunkSpec)
        upstreamOpened = true
        if (knownTotalLength == C.LENGTH_UNSET.toLong()) {
            val header =
                upstream.responseHeaders.entries
                    .firstOrNull { it.key.equals("Content-Range", ignoreCase = true) }
                    ?.value
                    ?.firstOrNull()
            parseTotalLengthFromContentRange(header)?.let { parsed ->
                knownTotalLength = parsed
            }
        }
        return true
    }

    private fun closeUpstreamQuietly() {
        if (upstreamOpened) {
            upstreamOpened = false
            try {
                upstream.close()
            } catch (_: IOException) {
            }
        }
    }

    companion object {
        internal fun parseTotalLengthFromContentRange(contentRange: String?): Long? {
            if (contentRange.isNullOrBlank()) return null
            val slashIndex = contentRange.lastIndexOf('/')
            if (slashIndex == -1 || slashIndex >= contentRange.lastIndex) return null
            return contentRange.substring(slashIndex + 1).trim().toLongOrNull()?.takeIf { it > 0L }
        }
    }
}

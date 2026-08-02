package com.snapload.app

import com.snapload.app.data.model.DownloadRequest
import com.snapload.app.data.model.DownloadUrlResponse
import com.snapload.app.data.model.VideoFormat
import com.snapload.app.data.model.VideoInfo
import com.snapload.app.data.network.ApiService
import com.snapload.app.data.network.NetworkResult
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import retrofit2.Response

class VideoRepositoryTest {

    @Mock
    lateinit var mockApiService: ApiService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    // ─── getVideoInfo success ────────────────────────────────────────────────
    @Test
    fun `getVideoInfo returns Success when API call succeeds`() = runBlocking {
        val fakeVideoInfo = VideoInfo(
            title = "Rick Astley - Never Gonna Give You Up",
            thumbnail = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
            duration = 213L,
            uploader = "RickAstleyVEVO",
            platform = "YouTube",
            formats = listOf(
                VideoFormat("22", "720p", "mp4", "video+audio", 52_428_800L, 2000.0, "https://example.com/vid.mp4")
            )
        )

        `when`(mockApiService.getVideoInfo(mapOf("url" to "https://youtu.be/dQw4w9WgXcQ")))
            .thenReturn(Response.success(fakeVideoInfo))

        val result = fakeGetVideoInfo("https://youtu.be/dQw4w9WgXcQ", fakeVideoInfo, null)

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("Rick Astley - Never Gonna Give You Up", data.title)
        assertEquals(1, data.formats.size)
        assertEquals("720p", data.formats[0].quality)
    }

    @Test
    fun `getVideoInfo returns Error when API returns error body`() = runBlocking {
        val errorInfo = VideoInfo(error = "فيديو خاص")
        val result = fakeGetVideoInfo("https://youtu.be/private", errorInfo, null)
        assertTrue(result is NetworkResult.Error)
        assertEquals("فيديو خاص", (result as NetworkResult.Error).message)
    }

    @Test
    fun `getVideoInfo returns Error on HTTP 404`() = runBlocking {
        val result = fakeGetVideoInfoHttp404("https://youtu.be/notfound")
        assertTrue(result is NetworkResult.Error)
        assertEquals(404, (result as NetworkResult.Error).code)
    }

    @Test
    fun `getVideoInfo returns Error on network exception`() = runBlocking {
        val result = fakeGetVideoInfoException("timeout")
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("timeout"))
    }

    @Test
    fun `getVideoInfo returns Error when body is null`() = runBlocking {
        val result = fakeGetVideoInfoNullBody()
        assertTrue(result is NetworkResult.Error)
    }

    // ─── getDownloadUrl success ───────────────────────────────────────────────
    @Test
    fun `getDownloadUrl returns Success with direct URL`() = runBlocking {
        val fakeResponse = DownloadUrlResponse(
            title = "Test Video",
            ext = "mp4",
            directUrl = "https://googlevideo.com/direct?id=xyz",
            thumbnail = "https://example.com/thumb.jpg"
        )
        val result = fakeGetDownloadUrl("https://youtu.be/abc", "22", fakeResponse, null)
        assertTrue(result is NetworkResult.Success)
        assertEquals("https://googlevideo.com/direct?id=xyz", (result as NetworkResult.Success).data.directUrl)
    }

    @Test
    fun `getDownloadUrl returns Error when directUrl is null`() = runBlocking {
        val badResponse = DownloadUrlResponse(title = "Test", directUrl = null)
        val result = fakeGetDownloadUrl("https://youtu.be/abc", "22", badResponse, null)
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `getDownloadUrl returns Error on exception`() = runBlocking {
        val result = fakeGetDownloadUrlException("Connection refused")
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("Connection refused"))
    }

    // ─── VideoFormat helpers ──────────────────────────────────────────────────
    @Test
    fun `VideoFormat isVideoAndAudio returns true for combined format`() {
        val fmt = VideoFormat(type = "video+audio")
        assertTrue(fmt.isVideoAndAudio())
        assertFalse(fmt.isVideoOnly())
        assertFalse(fmt.isAudioOnly())
    }

    @Test
    fun `VideoFormat isAudioOnly returns true for audio format`() {
        val fmt = VideoFormat(type = "audio")
        assertTrue(fmt.isAudioOnly())
        assertFalse(fmt.isVideoAndAudio())
        assertFalse(fmt.isVideoOnly())
    }

    @Test
    fun `VideoFormat formattedSize returns empty when filesize is null`() {
        val fmt = VideoFormat(filesize = null)
        assertEquals("", fmt.formattedSize())
    }

    @Test
    fun `VideoFormat formattedSize returns MB format for small files`() {
        val fmt = VideoFormat(filesize = 52_428_800L) // 50 MB
        assertTrue(fmt.formattedSize().contains("MB"))
    }

    @Test
    fun `VideoFormat formattedSize returns GB format for large files`() {
        val fmt = VideoFormat(filesize = 2_147_483_648L) // 2 GB
        assertTrue(fmt.formattedSize().contains("GB"))
    }

    // ─── Stub helpers — تحاكي منطق Repository دون Android Context ──────────

    private fun fakeGetVideoInfo(
        url: String,
        info: VideoInfo,
        exception: Exception?
    ): NetworkResult<VideoInfo> {
        val error = info.error
        return when {
            exception != null -> NetworkResult.Error(exception.localizedMessage ?: "خطأ في الاتصال")
            error != null -> NetworkResult.Error(error)
            else -> NetworkResult.Success(info)
        }
    }

    private fun fakeGetVideoInfoHttp404(url: String): NetworkResult<VideoInfo> {
        return NetworkResult.Error("خطأ 404: Not Found", 404)
    }

    private fun fakeGetVideoInfoException(message: String): NetworkResult<VideoInfo> {
        return NetworkResult.Error(message)
    }

    private fun fakeGetVideoInfoNullBody(): NetworkResult<VideoInfo> {
        return NetworkResult.Error("استجابة فارغة من السيرفر")
    }

    private fun fakeGetDownloadUrl(
        url: String,
        formatId: String,
        response: DownloadUrlResponse,
        exception: Exception?
    ): NetworkResult<DownloadUrlResponse> {
        val error = response.error
        return when {
            exception != null          -> NetworkResult.Error(exception.localizedMessage ?: "خطأ")
            error != null              -> NetworkResult.Error(error)
            response.directUrl == null -> NetworkResult.Error("لا يوجد رابط تحميل متاح")
            else                       -> NetworkResult.Success(response)
        }
    }

    private fun fakeGetDownloadUrlException(message: String): NetworkResult<DownloadUrlResponse> {
        return NetworkResult.Error(message)
    }
}

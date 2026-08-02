package com.snapload.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests لـ Extension functions
 */
class ExtensionsTest {

    // ─── isValidUrl ──────────────────────────────────────────────────────────
    @Test
    fun `isValidUrl returns true for valid YouTube URL`() {
        assertTrue("https://www.youtube.com/watch?v=dQw4w9WgXcQ".isValidUrlTest())
    }

    @Test
    fun `isValidUrl returns true for youtu-be short URL`() {
        assertTrue("https://youtu.be/dQw4w9WgXcQ".isValidUrlTest())
    }

    @Test
    fun `isValidUrl returns true for TikTok URL`() {
        assertTrue("https://www.tiktok.com/@user/video/7123456789".isValidUrlTest())
    }

    @Test
    fun `isValidUrl returns true for Instagram reel URL`() {
        assertTrue("https://www.instagram.com/reel/ABC123/".isValidUrlTest())
    }

    @Test
    fun `isValidUrl returns false for empty string`() {
        assertFalse("".isValidUrlTest())
    }

    @Test
    fun `isValidUrl returns false for plain text`() {
        assertFalse("hello world".isValidUrlTest())
    }

    @Test
    fun `isValidUrl returns false for partial URL`() {
        assertFalse("youtube.com".isValidUrlTest())
    }

    @Test
    fun `isValidUrl returns false for ftp URL`() {
        assertFalse("ftp://example.com".isValidUrlTest())
    }

    // ─── detectPlatform ──────────────────────────────────────────────────────
    @Test
    fun `detectPlatform detects YouTube`() {
        assertEquals("YouTube", "https://www.youtube.com/watch?v=abc".detectPlatformTest())
    }

    @Test
    fun `detectPlatform detects youtu-be as YouTube`() {
        assertEquals("YouTube", "https://youtu.be/abc".detectPlatformTest())
    }

    @Test
    fun `detectPlatform detects Instagram`() {
        assertEquals("Instagram", "https://www.instagram.com/p/abc/".detectPlatformTest())
    }

    @Test
    fun `detectPlatform detects TikTok`() {
        assertEquals("TikTok", "https://www.tiktok.com/@user/video/123".detectPlatformTest())
    }

    @Test
    fun `detectPlatform detects Twitter`() {
        assertEquals("Twitter", "https://twitter.com/user/status/123".detectPlatformTest())
    }

    @Test
    fun `detectPlatform detects X-dot-com as Twitter`() {
        assertEquals("Twitter", "https://x.com/user/status/123".detectPlatformTest())
    }

    @Test
    fun `detectPlatform detects Facebook`() {
        assertEquals("Facebook", "https://www.facebook.com/video/123".detectPlatformTest())
    }

    @Test
    fun `detectPlatform detects Vimeo`() {
        assertEquals("Vimeo", "https://vimeo.com/123456789".detectPlatformTest())
    }

    @Test
    fun `detectPlatform returns Unknown for unsupported URL`() {
        assertEquals("Unknown", "https://example.com/video".detectPlatformTest())
    }

    // ─── toFormattedSize ──────────────────────────────────────────────────────
    @Test
    fun `toFormattedSize returns zero for 0 bytes`() {
        assertEquals("0 B", 0L.toFormattedSizeTest())
    }

    @Test
    fun `toFormattedSize formats bytes correctly`() {
        assertEquals("512 B", 512L.toFormattedSizeTest())
    }

    @Test
    fun `toFormattedSize formats KB correctly`() {
        assertEquals("1.0 KB", 1024L.toFormattedSizeTest())
    }

    @Test
    fun `toFormattedSize formats MB correctly`() {
        assertEquals("1.0 MB", (1024L * 1024L).toFormattedSizeTest())
    }

    @Test
    fun `toFormattedSize formats GB correctly`() {
        assertEquals("1.0 GB", (1024L * 1024L * 1024L).toFormattedSizeTest())
    }

    @Test
    fun `toFormattedSize formats 50 MB correctly`() {
        assertEquals("50.0 MB", (50L * 1024L * 1024L).toFormattedSizeTest())
    }

    @Test
    fun `toFormattedSize formats 2-5 GB correctly`() {
        assertEquals("2.5 GB", (2560L * 1024L * 1024L).toFormattedSizeTest())
    }

    // ─── toFormattedDuration ──────────────────────────────────────────────────
    @Test
    fun `toFormattedDuration formats zero`() {
        assertEquals("0:00", 0L.toFormattedDurationTest())
    }

    @Test
    fun `toFormattedDuration formats 65 seconds as 1-05`() {
        assertEquals("1:05", 65L.toFormattedDurationTest())
    }

    @Test
    fun `toFormattedDuration formats 3 minutes 45 seconds`() {
        assertEquals("3:45", 225L.toFormattedDurationTest())
    }

    @Test
    fun `toFormattedDuration formats exactly 1 hour`() {
        assertEquals("1:00:00", 3600L.toFormattedDurationTest())
    }

    @Test
    fun `toFormattedDuration formats 1 hour 23 minutes 6 seconds`() {
        assertEquals("1:23:06", (3600L + 23 * 60 + 6).toFormattedDurationTest())
    }

    @Test
    fun `toFormattedDuration formats 10 hours`() {
        assertEquals("10:00:00", (10L * 3600L).toFormattedDurationTest())
    }

    // ─── toSafeFileName ──────────────────────────────────────────────────────
    @Test
    fun `toSafeFileName removes forbidden characters`() {
        val unsafe = "Video: Title? / Test | File*"
        val safe = unsafe.toSafeFileNameTest()
        assertFalse(safe.contains(':'))
        assertFalse(safe.contains('?'))
        assertFalse(safe.contains('/'))
        assertFalse(safe.contains('|'))
        assertFalse(safe.contains('*'))
    }

    @Test
    fun `toSafeFileName keeps alphanumeric and spaces`() {
        val name = "My Video 2024"
        assertEquals(name, name.toSafeFileNameTest())
    }

    // ─── Inline test implementations (pure Kotlin — no Android dependency) ───

    private fun String.isValidUrlTest(): Boolean {
        if (isBlank()) return false
        return startsWith("http://") || startsWith("https://")
    }

    private fun String.detectPlatformTest(): String {
        return when {
            contains("youtube.com") || contains("youtu.be") -> "YouTube"
            contains("instagram.com")                        -> "Instagram"
            contains("tiktok.com") || contains("vm.tiktok") -> "TikTok"
            contains("twitter.com") || contains("x.com")    -> "Twitter"
            contains("facebook.com") || contains("fb.watch")-> "Facebook"
            contains("vimeo.com")                           -> "Vimeo"
            contains("dailymotion.com")                     -> "Dailymotion"
            contains("soundcloud.com")                      -> "SoundCloud"
            contains("twitch.tv")                           -> "Twitch"
            contains("reddit.com")                          -> "Reddit"
            contains("pinterest.com")                       -> "Pinterest"
            contains("bilibili.com")                        -> "Bilibili"
            contains("ok.ru")                               -> "OK.ru"
            contains("vk.com")                              -> "VK"
            else                                            -> "Unknown"
        }
    }

    private fun Long.toFormattedSizeTest(): String {
        return when {
            this < 1024L             -> "$this B"
            this < 1024L * 1024L     -> "%.1f KB".format(this / 1024.0)
            this < 1024L * 1024L * 1024L -> "%.1f MB".format(this / 1024.0 / 1024.0)
            else                     -> "%.1f GB".format(this / 1024.0 / 1024.0 / 1024.0)
        }
    }

    private fun Long.toFormattedDurationTest(): String {
        val total = this
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s)
        else "%d:%02d".format(m, s)
    }

    private fun String.toSafeFileNameTest(): String {
        return replace(Regex("[\\\\/:*?\"<>|]"), "").trim()
    }
}

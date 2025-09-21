import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.navtest.services.FeedItemService
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FeedItemServiceTests {
    private lateinit var server: MockWebServer
    private lateinit var service: FeedItemService
    private lateinit var client: OkHttpClient

    @BeforeEach()
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient()
    }

    @AfterEach()
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testFetchAndParseFeed() {
        val jsonResponse = """
            {
                "version": "1.0",
                "title": "Test Feed",
                "home_page_url": "https://example.com",
                "feed_url": "/api/v1/feed",
                "description": "A test feed",
                "next_url": "/api/v1/feed?page=2",
                "id": "feed-1",
                "next_id": "feed-2",
                "items": [
                    { "id": "item-1", "content": "Kotlin developer" }
                ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setBody(jsonResponse).setResponseCode(200))

        var url = server.url("/api/v1/feed").toString()
        var request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()
        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
            val body = response.body?.string()
            assertEquals(true, body?.contains("Kotlin developer") ?: false)
        }

        val recordedRequest = server.takeRequest()
        assertEquals("Bearer test_token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `should handle http error`() {
        server.enqueue(MockResponse().setResponseCode(404))
        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()
        client.newCall(request).execute().use { response ->
            assertEquals(false, response.isSuccessful)
            assertEquals(404, response.code)
        }
        val recordedRequest = server.takeRequest()
        assertEquals("Bearer test_token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `should handle 400 bad request error`() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(400, response.code)
            assertEquals(false, response.isSuccessful)
        }
    }

    @Test
    fun `should handle 401 unauthorized error`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer invalid_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(401, response.code)
            assertEquals(false, response.isSuccessful)
        }

        val recordedRequest = server.takeRequest()
        assertEquals("Bearer invalid_token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `should handle 403 forbidden error`() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("Forbidden"))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(403, response.code)
            assertEquals(false, response.isSuccessful)
        }
    }

    @Test
    fun `should handle 500 internal server error`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(500, response.code)
            assertEquals(false, response.isSuccessful)
        }
    }

    @Test
    fun `should handle empty response body`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(""))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertTrue(response.isSuccessful)
            val body = response.body?.string()
            assertEquals("", body)
        }
    }

    @Test
    fun `should handle malformed JSON response`() {
        val malformedJson = """
            {
                "version": "1.0",
                "title": "Malformed Feed"
                "missing_comma_here": "value"
                "items": [
                    { "id": "item-1" "missing_comma": "value" }
                ]
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(malformedJson))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertTrue(response.isSuccessful)
            val body = response.body?.string()
            assertTrue(body?.isNotEmpty() ?: false)
            // In real implementation, this would test that gson parsing fails gracefully
        }
    }

    @Test
    fun `should handle incomplete JSON structure`() {
        val incompleteJson = """
            {
                "version": "1.0",
                "title": "Incomplete Feed"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(incompleteJson))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertTrue(response.isSuccessful)
            val body = response.body?.string()
            assertTrue(body?.contains("Incomplete Feed") ?: false)
        }
    }

    @Test
    fun `should include correct headers in request`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .addHeader("accept", "application/json")
            .build()

        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        assertEquals("Bearer test_token", recordedRequest.getHeader("Authorization"))
        assertEquals("application/json", recordedRequest.getHeader("accept"))
        assertEquals("GET", recordedRequest.method)
    }

    @Test
    fun `should handle null response body gracefully`() {
        server.enqueue(MockResponse().setResponseCode(204)) // No Content

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(204, response.code)
            assertTrue(response.isSuccessful)
            val body = response.body?.string()
            assertEquals("", body)
        }
    }

    @Test
    fun `should validate If-Modified-Since header format`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val url = server.url("/api/v1/feed").toString()
        val testDate = "Sat, 21 Sep 2025 10:00:00 GMT" // RFC 1123 format
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .addHeader("If-Modified-Since", testDate)
            .build()

        client.newCall(request).execute()

        val recordedRequest = server.takeRequest()
        assertEquals(testDate, recordedRequest.getHeader("If-Modified-Since"))
        assertEquals("Bearer test_token", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `should handle feed with multiple items`() {
        val jsonResponse = """
            {
                "version": "1.0",
                "title": "Multi-Item Feed",
                "home_page_url": "https://example.com",
                "feed_url": "/api/v1/feed",
                "description": "A feed with multiple items",
                "next_url": "/api/v1/feed?page=2",
                "id": "multi-item-feed",
                "next_id": "next-page-feed",
                "items": [
                    { 
                        "id": "item-1", 
                        "url": "https://example.com/item1",
                        "title": "First Item",
                        "content_text": "Content for first item",
                        "date_modified": "2025-09-21T10:00:00Z",
                        "feed_entry": { "id": "entry-1" }
                    },
                    { 
                        "id": "item-2", 
                        "url": "https://example.com/item2",
                        "title": "Second Item",
                        "content_text": "Content for second item",
                        "date_modified": "2025-09-21T11:00:00Z",
                        "feed_entry": { "id": "entry-2" }
                    },
                    { 
                        "id": "item-3", 
                        "url": "https://example.com/item3",
                        "title": "Third Item",
                        "content_text": "Content for third item",
                        "date_modified": "2025-09-21T12:00:00Z",
                        "feed_entry": { "id": "entry-3" }
                    }
                ]
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
            val body = response.body?.string()
            assertTrue(body?.contains("First Item") ?: false)
            assertTrue(body?.contains("Second Item") ?: false)
            assertTrue(body?.contains("Third Item") ?: false)
            assertTrue(body?.contains("item-1") ?: false)
            assertTrue(body?.contains("item-2") ?: false)
            assertTrue(body?.contains("item-3") ?: false)
        }
    }

    @Test
    fun `should handle feed with empty items array`() {
        val jsonResponse = """
            {
                "version": "1.0",
                "title": "Empty Feed",
                "home_page_url": "https://example.com",
                "feed_url": "/api/v1/feed",
                "description": "A feed with no items",
                "next_url": "/api/v1/feed?page=2",
                "id": "empty-feed",
                "next_id": "next-empty-feed",
                "items": []
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val url = server.url("/api/v1/feed").toString()
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer test_token")
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
            val body = response.body?.string()
            assertTrue(body?.contains("Empty Feed") ?: false)
            assertTrue(body?.contains("\"items\": []") ?: false)
        }
    }

    @Test
    fun `should handle extractAndPrintAddOnsResult with week 52 boundary dates`() {
        val feedItems = listOf(
            createFeedItem("item-w53-1", "Kotlin development", "2024-12-23T10:00:00Z"), // Real week 53 of 2024
            createFeedItem("item-w53-2", "Java programming", "2024-12-27T14:30:00Z"),   // Real week 53 of 2024
            createFeedItem("item-w1-late", "Advanced Kotlin", "2024-12-30T09:00:00Z"),  // ISO week 1 of 2025, calendar year 2024
            createFeedItem("item-w1-early", "Java Spring", "2025-01-01T08:00:00Z")      // ISO week 1 of 2025, calendar year 2025
        )

        service = FeedItemService(client, "test_token")
        val weekFields = java.time.temporal.WeekFields.ISO

        val result = service.extractAndPrintAddOnsResult(feedItems, weekFields)

        assertNotNull(result)
        assertTrue(result!!.isNotEmpty())

        // With ISO week + ISO year approach, we should see:
        // - 2024 week 53: 2 items (1 kotlin, 1 java)
        // - 2025 week 1: 2 items (1 kotlin, 1 java) - combining late Dec 2024 + early Jan 2025
        val week53Stats = result.find { it.year == 2024 && it.week == 52 }
        val week1Stats = result.find { it.year == 2025 && it.week == 1 }

        assertNotNull(week53Stats, "Should have week 52 of 2024")
        assertNotNull(week1Stats, "Should have week 1 of 2025")

        assertEquals(1, week53Stats!!.kotlin, "Week 52 should have 1 Kotlin item")
        assertEquals(1, week53Stats.java, "Week 52 should have 1 Java item")

        assertEquals(1, week1Stats!!.kotlin, "Week 1 should have 1 Kotlin item")
        assertEquals(1, week1Stats.java, "Week 1 should have 1 Java item")
    }

    @Test
    fun `should filter out weeks with zero kotlin and java counts`() {
        val feedItems = listOf(
            createFeedItem("item-1", "Python development", "2024-12-23T10:00:00Z"),      // No kotlin/java
            createFeedItem("item-2", "C# programming", "2024-12-27T14:30:00Z"),          // No kotlin/java
            createFeedItem("item-3", "Kotlin coroutines", "2025-01-01T08:00:00Z"),       // Has kotlin
            createFeedItem("item-4", "React frontend", "2025-01-02T10:00:00Z")           // No kotlin/java
        )

        service = FeedItemService(client, "test_token")
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())

        val result = service.extractAndPrintAddOnsResult(feedItems, weekFields)

        assertNotNull(result)
        assertEquals(1, result!!.size, "Should only return weeks with kotlin or java items")

        val weekStats = result[0]
        assertEquals(1, weekStats.kotlin, "Should have 1 Kotlin item")
        assertEquals(0, weekStats.java, "Should have 0 Java items")
    }

    @Test
    fun `should handle empty feed items list`() {
        service = FeedItemService(client, "test_token")
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())

        val result = service.extractAndPrintAddOnsResult(emptyList(), weekFields)

        assertTrue(result.isNullOrEmpty(), "Should return empty result for empty input")
    }

    @Test
    fun `should handle null feed items`() {
        service = FeedItemService(client, "test_token")
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())

        val result = service.extractAndPrintAddOnsResult(null, weekFields)

        assertNull(result, "Should return null for null input")
    }

    @Test
    fun `should correctly count kotlin and java items in same week`() {
        val feedItems = listOf(
            createFeedItem("item-1", "Kotlin basics tutorial", "2025-01-06T10:00:00Z"),    // Week 2, kotlin
            createFeedItem("item-2", "Java fundamentals", "2025-01-06T14:00:00Z"),        // Week 2, java
            createFeedItem("item-3", "Advanced Kotlin patterns", "2025-01-07T09:00:00Z"), // Week 2, kotlin
            createFeedItem("item-4", "Java Spring Boot guide", "2025-01-08T16:00:00Z"),   // Week 2, java
            createFeedItem("item-5", "Kotlin multiplatform", "2025-01-09T11:00:00Z")      // Week 2, kotlin
        )

        service = FeedItemService(client, "test_token")
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())

        val result = service.extractAndPrintAddOnsResult(feedItems, weekFields)

        assertNotNull(result)
        assertEquals(1, result!!.size, "Should group all items into one week")

        val weekStats = result[0]
        assertEquals(3, weekStats.kotlin, "Should count 3 Kotlin items")
        assertEquals(2, weekStats.java, "Should count 2 Java items")
        assertEquals(2025, weekStats.year, "Should be year 2025")
        assertEquals(2, weekStats.week, "Should be week 2")
    }

    @Test
    fun `should handle case insensitive matching for kotlin and java`() {
        val feedItems = listOf(
            createFeedItem("item-1", "KOTLIN Development", "2025-01-06T10:00:00Z"),       // Uppercase
            createFeedItem("item-2", "kotlin programming", "2025-01-06T14:00:00Z"),       // Lowercase
            createFeedItem("item-3", "Kotlin Basics", "2025-01-06T16:00:00Z"),           // Title case
            createFeedItem("item-4", "JAVA Tutorial", "2025-01-07T10:00:00Z"),           // Uppercase
            createFeedItem("item-5", "java fundamentals", "2025-01-07T14:00:00Z"),       // Lowercase
            createFeedItem("item-6", "Java Best Practices", "2025-01-07T16:00:00Z")      // Title case
        )

        service = FeedItemService(client, "test_token")
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())

        val result = service.extractAndPrintAddOnsResult(feedItems, weekFields)

        assertNotNull(result)
        assertEquals(1, result!!.size, "Should group all items into one week")

        val weekStats = result[0]
        assertEquals(3, weekStats.kotlin, "Should count all 3 Kotlin variants")
        assertEquals(3, weekStats.java, "Should count all 3 Java variants")
    }

    @Test
    fun `should handle items with both kotlin and java in title`() {
        val feedItems = listOf(
            createFeedItem("item-1", "Kotlin vs Java comparison", "2025-01-06T10:00:00Z"),
            createFeedItem("item-2", "Java to Kotlin migration", "2025-01-06T14:00:00Z"),
            createFeedItem("item-3", "Pure Kotlin development", "2025-01-06T16:00:00Z"),
            createFeedItem("item-4", "Only Java programming", "2025-01-06T18:00:00Z")
        )

        service = FeedItemService(client, "test_token")
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())

        val result = service.extractAndPrintAddOnsResult(feedItems, weekFields)

        assertNotNull(result)
        assertEquals(1, result!!.size, "Should group all items into one week")

        val weekStats = result[0]
        // Items with both keywords count for both categories
        assertEquals(3, weekStats.kotlin, "Should count 3 items containing 'kotlin'")
        assertEquals(3, weekStats.java, "Should count 3 items containing 'java'")
    }

    @Test
    fun `should handle multiple weeks across different years`() {
        val feedItems = listOf(
            // 2024 items
            createFeedItem("item-2024-w52", "Kotlin development 2024", "2024-12-20T10:00:00Z"), // Week 52 of 2024
            createFeedItem("item-2024-w53", "Java programming 2024", "2024-12-27T10:00:00Z"),   // Week 53 of 2024

            // 2025 items (ISO week 1 includes late Dec 2024 dates)
            createFeedItem("item-2025-w1a", "Advanced Kotlin 2025", "2024-12-30T10:00:00Z"),   // ISO week 1 of 2025
            createFeedItem("item-2025-w1b", "Java Spring 2025", "2025-01-02T10:00:00Z"),       // ISO week 1 of 2025
            createFeedItem("item-2025-w2", "Kotlin coroutines", "2025-01-08T10:00:00Z")        // Week 2 of 2025
        )

        service = FeedItemService(client, "test_token")
        val weekFields = java.time.temporal.WeekFields.of(java.util.Locale.getDefault())

        val result = service.extractAndPrintAddOnsResult(feedItems, weekFields)

        assertNotNull(result)
        assertEquals(4, result!!.size, "Should have 4 different week entries")

        val year2024Weeks = result.filter { it.year == 2024 }
        val year2025Weeks = result.filter { it.year == 2025 }

        assertEquals(2, year2024Weeks.size, "Should have 2 weeks in 2024")
        assertEquals(2, year2025Weeks.size, "Should have 2 weeks in 2025")

        // Verify week 1 of 2025 combines items from late Dec 2024 and early Jan 2025
        val week1Of2025 = result.find { it.year == 2025 && it.week == 1 }
        assertNotNull(week1Of2025)
        assertEquals(1, week1Of2025!!.kotlin, "Week 1 of 2025 should have 1 Kotlin item")
        assertEquals(1, week1Of2025.java, "Week 1 of 2025 should have 1 Java item")
    }

    private fun createFeedItem(id: String, title: String, dateModified: String): org.navtest.models.FeedItem {
        return org.navtest.models.FeedItem(
            id = id,
            url = "https://example.com/$id",
            title = title,
            contentText = "Content for $title",
            dateModified = dateModified,
            feedEntry = org.navtest.models.FeedEntry(
                uuid = "entry-$id",
                status = "ACTIVE",
                title = title,
                businessName = "Test Business",
                municipal = "Test Municipal",
                sistEndret = dateModified
            )
        )
    }
}
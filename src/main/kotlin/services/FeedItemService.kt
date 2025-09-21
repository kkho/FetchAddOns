package org.navtest.services

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.navtest.models.FeedItem
import org.navtest.models.FeedPage
import org.navtest.models.WeekStats
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields


class FeedItemService(private val client: OkHttpClient, private val bearerToken: String) : FeedService {
    companion object {
        private val gson: Gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        private const val url = "https://pam-stilling-feed.nav.no/api/v1/feed"
    }

    override fun getFeedByModifiedSince(lastModifiedSince: ZonedDateTime): FeedPage? {
        val rfc1123Date = lastModifiedSince.format(DateTimeFormatter.RFC_1123_DATE_TIME)
        val request = buildRequest("If-Modified-Since", rfc1123Date, null)
        return RequestFeeds(request)
    }

    override fun getFeedById(id: String): FeedPage? {
        val request = buildRequest(null, null, "/$id")
        return RequestFeeds(request)
    }

    override fun extractAndPrintAddOnsResult(
        feedItems: List<FeedItem>?,
        weekFields: WeekFields
    ): List<WeekStats>? {
        val stats = feedItems?.map { item ->
            val date = ZonedDateTime.parse(item.dateModified)
            val week = date.get(weekFields.weekOfWeekBasedYear())
            val year = date.get(weekFields.weekBasedYear())
            Triple(year, week, item)
        }
            ?.groupBy { Pair(it.first, it.second) }
            ?.map { (yearWeek, group) ->
                val kotlinCount = group.count { it.third.title.contains("kotlin", ignoreCase = true) }
                val javaCount = group.count { it.third.title.contains("java", ignoreCase = true) }
                WeekStats(yearWeek.first, yearWeek.second, kotlinCount, javaCount)
            }?.filter { it.kotlin > 0 || it.java > 0 }

        return stats;
    }

    private fun buildRequest(
        headerName: String?,
        value: String?,
        path: String?
    ): Request {
        val request = Request
            .Builder().url(FeedItemService.Companion.url + if (!path.isNullOrEmpty()) path else "")
            .addHeader("Authorization", "Bearer $bearerToken")
            .addHeader("accept", "application/json")

        if (!headerName.isNullOrEmpty() && !value.isNullOrEmpty()) {
            request.addHeader(headerName, value)
        }

        return request.build()
    }

    private fun RequestFeeds(request: Request): FeedPage? {
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()

                    if (body != null) {
                        return gson.fromJson(body, FeedPage::class.java)
                    } else {
                        println("Empty response body")
                        return null
                    }

                } else {
                    println("Http Error: ${response.code}")
                    null
                }
            }

        } catch (ex: Exception) {
            println("Exception: ${ex.message}")
            null
        }
    }
}
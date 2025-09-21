package org.navtest

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import org.navtest.models.FeedItem
import org.navtest.services.FeedItemService
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.*

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please include access token")
    }

    val gson = GsonBuilder().setPrettyPrinting().create()
    val token = args.getOrNull(0) ?: error("Missing access token!")

    val client = OkHttpClient()
    val feedService = FeedItemService(client, token)

    val localDate = LocalDate.now()
    val utcStartOfDay: ZonedDateTime = localDate.atStartOfDay(ZoneOffset.UTC)
    val startDate = utcStartOfDay.minusWeeks(26)
    val weekFields = java.time.temporal.WeekFields.ISO

    val feedPageByDateSinceSixMonthsAgo = feedService.getFeedByModifiedSince(startDate)

    if (feedPageByDateSinceSixMonthsAgo == null || feedPageByDateSinceSixMonthsAgo.items.isEmpty()) {
        println("No Result found")
    } else {

        val feedItems = mutableListOf<FeedItem>()
        val items = feedPageByDateSinceSixMonthsAgo.items.filter { it.title.contains("kotlin", ignoreCase = true) || it.title.contains("java", ignoreCase = true) }
        feedItems.addAll(items)

        var nextPage: String? = feedPageByDateSinceSixMonthsAgo.nextId

        while (nextPage != null) {
            val fetchNextPage = feedService.getFeedById(nextPage)
            val nextFeedItems = fetchNextPage?.items?.filter {
                it.title.contains("kotlin", ignoreCase = true) ||
                        it.title.contains(
                            "java", ignoreCase = true
                        )
            }

            if (!nextFeedItems.isNullOrEmpty()) {
                feedItems.addAll(nextFeedItems)
            }

            nextPage = fetchNextPage?.nextId
        }

        val stats = feedService.extractAndPrintAddOnsResult(feedItems, weekFields)
        if (stats != null && stats.isNotEmpty()) {
            println(gson.toJson(HashMap(stats.groupBy { it.year })))
        } else {
            println("No Result found")
        }
    }
}
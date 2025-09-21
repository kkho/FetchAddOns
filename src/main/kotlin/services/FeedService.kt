package org.navtest.services

import org.navtest.models.FeedItem
import org.navtest.models.FeedPage
import org.navtest.models.WeekStats
import java.time.ZonedDateTime
import java.time.temporal.WeekFields

interface FeedService {
    fun getFeedByModifiedSince(lastModifiedSince: ZonedDateTime): FeedPage?
    fun getFeedById(id: String): FeedPage?
    fun extractAndPrintAddOnsResult(feedItems: List<FeedItem>?, weekFields: WeekFields): List<WeekStats>?
}
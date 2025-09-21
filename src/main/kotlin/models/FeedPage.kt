package org.navtest.models

data class FeedPage(
    val version: String,
    val title: String,
    val homePageUrl: String,
    val feedUrl: String,
    val description: String,
    val nextUrl: String,
    val id: String,
    val nextId: String,
    val items: List<FeedItem>
)
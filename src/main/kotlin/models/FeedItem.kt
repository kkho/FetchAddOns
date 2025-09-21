package org.navtest.models

data class FeedItem(val id: String,val url: String,val title: String,val contentText: String,val dateModified: String, val feedEntry: FeedEntry)
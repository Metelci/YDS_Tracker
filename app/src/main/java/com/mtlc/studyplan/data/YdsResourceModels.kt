package com.mtlc.studyplan.data

/**
 * Represents an external YDS study resource (video, podcast, article, etc.)
 */
data class YdsResource(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val type: ResourceType,
    val duration: String? = null, // e.g., "15 dk", "25 min"
    val language: String = "tr", // tr (Turkish) or en (English)
    val source: String = "" // e.g., "ÖSYM", "YouTube", "Podcast Platform"
)

/**
 * Types of YDS study resources available
 */
enum class ResourceType {
    VIDEO,          // YouTube videos, video tutorials
    PODCAST,        // Audio content, podcast episodes
    ARTICLE,        // Blog posts, study guides, articles
    OFFICIAL_GUIDE  // Official ÖSYM resources and guides
}

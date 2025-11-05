package com.runanywhere.startup_hackathon20

/**
 * Data models for StudyChamp - AI Study Companion
 */

// Represents a learning resource (video, article, etc.)
data class StudyResource(
    val title: String,
    val url: String,
    val type: ResourceType = ResourceType.ARTICLE
)

enum class ResourceType {
    VIDEO, ARTICLE, PDF, INTERACTIVE, OTHER
}

// Represents a chapter/topic in the learning journey
data class StudyChapter(
    val title: String,
    val story: String,
    val resources: List<StudyResource> = emptyList(),
    val isCompleted: Boolean = false
)

// The complete study journey
data class StudyJourney(
    val subject: String,
    val topics: String,
    val intro: String,
    val chapters: List<StudyChapter>,
    val closing: String = "",
    val currentChapterIndex: Int = 0
)

// Message types for the UI
sealed class StudyMessage {
    data class UserInput(val text: String) : StudyMessage()
    data class IntroMessage(val text: String) : StudyMessage()
    data class ChapterContent(val chapter: StudyChapter, val chapterNumber: Int) : StudyMessage()
    data class ClosingMessage(val text: String) : StudyMessage()
    data class StreamingAI(val text: String) : StudyMessage()
    data class LearningOptions(
        val subject: String,
        val topics: String,
        val options: List<LearningOption>
    ) : StudyMessage()
}

// Different learning styles students can choose
data class LearningOption(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String
)

// Learning style options
object LearningStyles {
    val STORY = LearningOption(
        id = "story",
        title = "Learn with Stories",
        description = "Understand through fun analogies and real-world examples",
        emoji = "📖"
    )

    val RESOURCES = LearningOption(
        id = "resources",
        title = "Show Me Resources",
        description = "Get YouTube videos, practice sites, and articles",
        emoji = "📚"
    )

    val DEFINITION = LearningOption(
        id = "definition",
        title = "Give Me Definitions",
        description = "Clear, concise explanations of key concepts",
        emoji = "📝"
    )

    val ROADMAP = LearningOption(
        id = "roadmap",
        title = "Show Learning Path",
        description = "Step-by-step guide from beginner to advanced",
        emoji = "🗺️"
    )

    fun getAll() = listOf(STORY, RESOURCES, DEFINITION, ROADMAP)
}

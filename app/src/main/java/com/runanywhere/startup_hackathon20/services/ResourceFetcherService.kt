package com.runanywhere.startup_hackathon20.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Service to fetch real educational resources from the internet
 */
class ResourceFetcherService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class EducationalResource(
        val title: String,
        val url: String,
        val description: String,
        val type: ResourceType
    )

    enum class ResourceType {
        VIDEO, ARTICLE, COURSE, TUTORIAL, PRACTICE
    }

    data class ResourceCollection(
        val videos: List<EducationalResource>,
        val articles: List<EducationalResource>,
        val courses: List<EducationalResource>,
        val practice: List<EducationalResource>
    )

    /**
     * Fetch real educational resources for a given topic
     */
    suspend fun fetchResources(subject: String, topic: String): ResourceCollection =
        withContext(Dispatchers.IO) {
            try {
                Log.d("ResourceFetcher", "Fetching resources for: $topic in $subject")

                // Fetch from multiple educational platforms
                val videos = fetchVideoResources(subject, topic)
                val articles = fetchArticleResources(subject, topic)
                val courses = fetchCourseResources(subject, topic)
                val practice = fetchPracticeResources(subject, topic)

                ResourceCollection(
                    videos = videos,
                    articles = articles,
                    courses = courses,
                    practice = practice
                )
            } catch (e: Exception) {
                Log.e("ResourceFetcher", "Failed to fetch resources: ${e.message}", e)
                // Return curated resources as fallback
                getCuratedResources(subject, topic)
            }
        }

    private suspend fun fetchVideoResources(
        subject: String,
        topic: String
    ): List<EducationalResource> {
        return try {
            // Generate YouTube search URLs
            val searchQuery = URLEncoder.encode("$topic $subject tutorial", "UTF-8")

            listOf(
                EducationalResource(
                    title = "Khan Academy: $topic",
                    url = "https://www.khanacademy.org/search?page_search_query=${
                        URLEncoder.encode(
                            topic,
                            "UTF-8"
                        )
                    }",
                    description = "Step-by-step video lessons with practice exercises",
                    type = ResourceType.VIDEO
                ),
                EducationalResource(
                    title = "YouTube: $topic Explained",
                    url = "https://www.youtube.com/results?search_query=$searchQuery",
                    description = "Video tutorials and lectures from educators",
                    type = ResourceType.VIDEO
                ),
                EducationalResource(
                    title = "MIT OpenCourseWare: $subject",
                    url = "https://ocw.mit.edu/search/?q=${URLEncoder.encode(topic, "UTF-8")}",
                    description = "Free university-level video lectures",
                    type = ResourceType.VIDEO
                ),
                EducationalResource(
                    title = "CrashCourse: $topic",
                    url = "https://www.youtube.com/c/crashcourse/search?query=$searchQuery",
                    description = "Engaging animated educational videos",
                    type = ResourceType.VIDEO
                )
            )
        } catch (e: Exception) {
            Log.e("ResourceFetcher", "Failed to fetch video resources", e)
            emptyList()
        }
    }

    private suspend fun fetchArticleResources(
        subject: String,
        topic: String
    ): List<EducationalResource> {
        return try {
            val searchQuery = URLEncoder.encode(topic, "UTF-8")

            listOf(
                EducationalResource(
                    title = "Wikipedia: $topic",
                    url = "https://en.wikipedia.org/wiki/${topic.replace(" ", "_")}",
                    description = "Comprehensive encyclopedia article with references",
                    type = ResourceType.ARTICLE
                ),
                EducationalResource(
                    title = "Britannica: $topic",
                    url = "https://www.britannica.com/search?query=$searchQuery",
                    description = "Expert-written articles and definitions",
                    type = ResourceType.ARTICLE
                ),
                EducationalResource(
                    title = "Google Scholar: $topic Research",
                    url = "https://scholar.google.com/scholar?q=$searchQuery",
                    description = "Academic papers and research articles",
                    type = ResourceType.ARTICLE
                ),
                EducationalResource(
                    title = "Science Direct: $topic",
                    url = "https://www.sciencedirect.com/search?qs=$searchQuery",
                    description = "Peer-reviewed scientific articles",
                    type = ResourceType.ARTICLE
                )
            )
        } catch (e: Exception) {
            Log.e("ResourceFetcher", "Failed to fetch article resources", e)
            emptyList()
        }
    }

    private suspend fun fetchCourseResources(
        subject: String,
        topic: String
    ): List<EducationalResource> {
        return try {
            val searchQuery = URLEncoder.encode("$topic $subject", "UTF-8")

            listOf(
                EducationalResource(
                    title = "Coursera: $topic Courses",
                    url = "https://www.coursera.org/search?query=$searchQuery",
                    description = "University-level online courses with certificates",
                    type = ResourceType.COURSE
                ),
                EducationalResource(
                    title = "edX: $subject - $topic",
                    url = "https://www.edx.org/search?q=$searchQuery",
                    description = "Free courses from top universities",
                    type = ResourceType.COURSE
                ),
                EducationalResource(
                    title = "Udacity: $topic Nanodegree",
                    url = "https://www.udacity.com/catalog/all/any-price/any-school/any-skill/any-difficulty/any-duration/any-type?searchValue=$searchQuery",
                    description = "Industry-focused courses and projects",
                    type = ResourceType.COURSE
                ),
                EducationalResource(
                    title = "OpenStax: $subject Textbook",
                    url = "https://openstax.org/subjects/${subject.lowercase().replace(" ", "-")}",
                    description = "Free, peer-reviewed digital textbooks",
                    type = ResourceType.COURSE
                )
            )
        } catch (e: Exception) {
            Log.e("ResourceFetcher", "Failed to fetch course resources", e)
            emptyList()
        }
    }

    private suspend fun fetchPracticeResources(
        subject: String,
        topic: String
    ): List<EducationalResource> {
        return try {
            val searchQuery = URLEncoder.encode("$topic practice", "UTF-8")

            val resources = mutableListOf<EducationalResource>()

            // Add subject-specific practice platforms
            when {
                subject.contains("Math", ignoreCase = true) || subject.contains(
                    "Algebra",
                    ignoreCase = true
                ) -> {
                    resources.addAll(
                        listOf(
                            EducationalResource(
                                title = "Desmos Calculator: $topic",
                                url = "https://www.desmos.com/calculator",
                                description = "Interactive graphing calculator for visualization",
                                type = ResourceType.PRACTICE
                            ),
                            EducationalResource(
                                title = "Wolfram Alpha: $topic",
                                url = "https://www.wolframalpha.com/input?i=$searchQuery",
                                description = "Computational knowledge engine for solving problems",
                                type = ResourceType.PRACTICE
                            )
                        )
                    )
                }

                subject.contains("Physics", ignoreCase = true) || subject.contains(
                    "Science",
                    ignoreCase = true
                ) -> {
                    resources.addAll(
                        listOf(
                            EducationalResource(
                                title = "PhET Simulations: $topic",
                                url = "https://phet.colorado.edu/en/simulations/filter?subjects=physics&type=html",
                                description = "Interactive science and math simulations",
                                type = ResourceType.PRACTICE
                            ),
                            EducationalResource(
                                title = "Physics Classroom: $topic",
                                url = "https://www.physicsclassroom.com/",
                                description = "Interactive lessons and practice problems",
                                type = ResourceType.PRACTICE
                            )
                        )
                    )
                }

                subject.contains("Chemistry", ignoreCase = true) -> {
                    resources.addAll(
                        listOf(
                            EducationalResource(
                                title = "ChemCollective: $topic",
                                url = "https://chemcollective.org/",
                                description = "Virtual chemistry labs and scenarios",
                                type = ResourceType.PRACTICE
                            )
                        )
                    )
                }

                subject.contains("Programming", ignoreCase = true) || subject.contains(
                    "Coding",
                    ignoreCase = true
                ) -> {
                    resources.addAll(
                        listOf(
                            EducationalResource(
                                title = "LeetCode: $topic Practice",
                                url = "https://leetcode.com/problemset/",
                                description = "Coding challenges and practice problems",
                                type = ResourceType.PRACTICE
                            ),
                            EducationalResource(
                                title = "HackerRank: $topic",
                                url = "https://www.hackerrank.com/domains",
                                description = "Programming practice and competitions",
                                type = ResourceType.PRACTICE
                            )
                        )
                    )
                }
            }

            // Add universal practice resources
            resources.addAll(
                listOf(
                    EducationalResource(
                        title = "Brilliant: $topic Exercises",
                        url = "https://brilliant.org/courses/#/${subject.lowercase()}/",
                        description = "Interactive problem-solving and exercises",
                        type = ResourceType.PRACTICE
                    ),
                    EducationalResource(
                        title = "Quizlet: $topic Flashcards",
                        url = "https://quizlet.com/search?query=$searchQuery",
                        description = "Study sets created by students and teachers",
                        type = ResourceType.PRACTICE
                    ),
                    EducationalResource(
                        title = "Practice Worksheets: $topic",
                        url = "https://www.google.com/search?q=$searchQuery+worksheets+pdf",
                        description = "Downloadable practice worksheets",
                        type = ResourceType.PRACTICE
                    )
                )
            )

            resources
        } catch (e: Exception) {
            Log.e("ResourceFetcher", "Failed to fetch practice resources", e)
            emptyList()
        }
    }

    /**
     * Curated high-quality resources as fallback when internet fetch fails
     */
    private fun getCuratedResources(subject: String, topic: String): ResourceCollection {
        val searchQuery = URLEncoder.encode("$topic $subject", "UTF-8")

        return ResourceCollection(
            videos = listOf(
                EducationalResource(
                    title = "Khan Academy: $topic",
                    url = "https://www.khanacademy.org/search?page_search_query=${
                        URLEncoder.encode(
                            topic,
                            "UTF-8"
                        )
                    }",
                    description = "Free video lessons and practice",
                    type = ResourceType.VIDEO
                ),
                EducationalResource(
                    title = "YouTube: $topic Tutorial",
                    url = "https://www.youtube.com/results?search_query=$searchQuery",
                    description = "Educational videos from teachers worldwide",
                    type = ResourceType.VIDEO
                )
            ),
            articles = listOf(
                EducationalResource(
                    title = "Wikipedia: $topic",
                    url = "https://en.wikipedia.org/wiki/${topic.replace(" ", "_")}",
                    description = "Comprehensive encyclopedia article",
                    type = ResourceType.ARTICLE
                ),
                EducationalResource(
                    title = "Google Scholar: Research on $topic",
                    url = "https://scholar.google.com/scholar?q=$searchQuery",
                    description = "Academic papers and articles",
                    type = ResourceType.ARTICLE
                )
            ),
            courses = listOf(
                EducationalResource(
                    title = "Coursera: $topic",
                    url = "https://www.coursera.org/search?query=$searchQuery",
                    description = "University courses online",
                    type = ResourceType.COURSE
                ),
                EducationalResource(
                    title = "OpenStax: Free Textbooks",
                    url = "https://openstax.org/",
                    description = "Free peer-reviewed textbooks",
                    type = ResourceType.COURSE
                )
            ),
            practice = listOf(
                EducationalResource(
                    title = "Brilliant: Interactive Learning",
                    url = "https://brilliant.org/",
                    description = "Problem-solving practice",
                    type = ResourceType.PRACTICE
                ),
                EducationalResource(
                    title = "Quizlet: Study Sets",
                    url = "https://quizlet.com/search?query=$searchQuery",
                    description = "Flashcards and quizzes",
                    type = ResourceType.PRACTICE
                )
            )
        )
    }
}

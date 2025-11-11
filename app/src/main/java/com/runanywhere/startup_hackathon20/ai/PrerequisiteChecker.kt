package com.runanywhere.startup_hackathon20.ai

import kotlinx.serialization.Serializable

/**
 * Prerequisite Knowledge Checker
 * Identifies foundational concepts needed before learning a topic
 * and assesses learner's readiness
 */

@Serializable
data class PrerequisiteKnowledge(
    val topicId: String,
    val topicName: String,
    val prerequisites: List<PrerequisiteConcept>,
    val estimatedDifficulty: String = "moderate" // "easy", "moderate", "hard", "advanced"
)

@Serializable
data class PrerequisiteConcept(
    val conceptId: String,
    val conceptName: String,
    val description: String,
    val importance: String = "essential", // "essential", "recommended", "helpful"
    val assessmentQuestion: String,
    val correctAnswer: String,
    val explanation: String
)

@Serializable
data class PrerequisiteAssessment(
    val topicName: String,
    val totalPrerequisites: Int,
    val masteredPrerequisites: Int,
    val readinessScore: Float, // 0.0 - 1.0
    val recommendation: String,
    val gapsIdentified: List<String>,
    val suggestedLearningPath: List<String>
)

class PrerequisiteChecker {

    /**
     * Get prerequisites for a given topic
     */
    fun getPrerequisites(subject: String, topic: String): PrerequisiteKnowledge? {
        val topicKey = "${subject.lowercase()}_${topic.lowercase()}"
        return prerequisiteDatabase[topicKey] ?: inferPrerequisites(subject, topic)
    }

    /**
     * Generate prerequisite assessment questions
     */
    fun generateAssessmentPrompt(
        subject: String,
        topic: String,
        mentorPersonality: MentorPersonality
    ): String {
        val prerequisites = getPrerequisites(subject, topic)

        return if (prerequisites != null && prerequisites.prerequisites.isNotEmpty()) {
            buildString {
                appendLine("Before we dive into $topic, let's check what you already know!")
                appendLine()
                appendLine("I'll ask you ${prerequisites.prerequisites.size} quick questions about the foundation concepts.")
                appendLine("This helps me teach you better! Don't worry - there are no wrong answers here. 😊")
                appendLine()
                appendLine("Ready? Let's begin!")
            }
        } else {
            "Great! Let's start learning $topic in $subject together!"
        }
    }

    /**
     * Assess readiness based on prerequisite answers
     */
    fun assessReadiness(
        topicName: String,
        prerequisites: List<PrerequisiteConcept>,
        userAnswers: Map<String, Boolean> // conceptId -> isCorrect
    ): PrerequisiteAssessment {
        val totalPrereqs = prerequisites.size
        val masteredCount = userAnswers.count { it.value }
        val readinessScore = if (totalPrereqs > 0) masteredCount.toFloat() / totalPrereqs else 1f

        val gapsIdentified = prerequisites
            .filter { userAnswers[it.conceptId] == false }
            .map { it.conceptName }

        val recommendation = when {
            readinessScore >= 0.8f ->
                "Excellent! You have a strong foundation. You're ready to learn $topicName! 🎉"

            readinessScore >= 0.6f ->
                "Good start! I'll review some basics as we go to strengthen your foundation. 👍"

            readinessScore >= 0.4f ->
                "Let's build your foundation first! I'll teach you the prerequisites before diving into $topicName. 📚"

            else ->
                "No worries! We'll start with the basics and work our way up together. Everyone starts somewhere! 💪"
        }

        val suggestedPath = when {
            readinessScore < 0.6f -> {
                listOf("Review: ${gapsIdentified.joinToString(", ")}") +
                        listOf("Practice exercises", "Then: $topicName")
            }

            readinessScore < 0.8f -> {
                listOf("Quick refresher on: ${gapsIdentified.joinToString(", ")}") +
                        listOf("Learn: $topicName")
            }

            else -> {
                listOf("Learn: $topicName", "Advanced concepts", "Practice & master")
            }
        }

        return PrerequisiteAssessment(
            topicName = topicName,
            totalPrerequisites = totalPrereqs,
            masteredPrerequisites = masteredCount,
            readinessScore = readinessScore,
            recommendation = recommendation,
            gapsIdentified = gapsIdentified,
            suggestedLearningPath = suggestedPath
        )
    }

    /**
     * Infer prerequisites using AI for unknown topics
     */
    private fun inferPrerequisites(subject: String, topic: String): PrerequisiteKnowledge {
        // Fallback: create basic prerequisite structure
        return PrerequisiteKnowledge(
            topicId = "${subject}_${topic}",
            topicName = topic,
            prerequisites = listOf(
                PrerequisiteConcept(
                    conceptId = "basic_${topic}",
                    conceptName = "Basic understanding of $subject",
                    description = "Fundamental concepts in $subject",
                    importance = "essential",
                    assessmentQuestion = "Do you have basic knowledge of $subject?",
                    correctAnswer = "yes",
                    explanation = "Having basic $subject knowledge helps you understand $topic better."
                )
            ),
            estimatedDifficulty = "moderate"
        )
    }

    companion object {
        /**
         * Predefined prerequisite knowledge base
         */
        private val prerequisiteDatabase = mapOf(
            // Physics - Newton's Laws
            "physics_newton's laws" to PrerequisiteKnowledge(
                topicId = "physics_newtons_laws",
                topicName = "Newton's Laws",
                prerequisites = listOf(
                    PrerequisiteConcept(
                        conceptId = "force_concept",
                        conceptName = "What is Force?",
                        description = "Understanding what force means",
                        importance = "essential",
                        assessmentQuestion = "Can you describe what a force is?",
                        correctAnswer = "A push or pull on an object",
                        explanation = "Force is a push or pull that can change an object's motion."
                    ),
                    PrerequisiteConcept(
                        conceptId = "mass_concept",
                        conceptName = "What is Mass?",
                        description = "Understanding mass",
                        importance = "essential",
                        assessmentQuestion = "What does mass measure?",
                        correctAnswer = "Amount of matter in an object",
                        explanation = "Mass is the amount of matter in an object, measured in kilograms."
                    ),
                    PrerequisiteConcept(
                        conceptId = "acceleration_concept",
                        conceptName = "What is Acceleration?",
                        description = "Understanding acceleration",
                        importance = "essential",
                        assessmentQuestion = "What is acceleration?",
                        correctAnswer = "Change in velocity over time",
                        explanation = "Acceleration is how quickly velocity changes, measured in m/s²."
                    )
                ),
                estimatedDifficulty = "moderate"
            ),

            // Physics - Gravitation
            "physics_gravitation" to PrerequisiteKnowledge(
                topicId = "physics_gravitation",
                topicName = "Gravitation",
                prerequisites = listOf(
                    PrerequisiteConcept(
                        conceptId = "force_understanding",
                        conceptName = "Force",
                        description = "Understanding forces",
                        importance = "essential",
                        assessmentQuestion = "What is a force?",
                        correctAnswer = "A push or pull",
                        explanation = "Gravity is a type of force."
                    ),
                    PrerequisiteConcept(
                        conceptId = "mass_understanding",
                        conceptName = "Mass",
                        description = "Understanding mass",
                        importance = "essential",
                        assessmentQuestion = "What is mass?",
                        correctAnswer = "Amount of matter",
                        explanation = "Gravitational force depends on mass."
                    ),
                    PrerequisiteConcept(
                        conceptId = "distance_concept",
                        conceptName = "Distance",
                        description = "Understanding distance",
                        importance = "recommended",
                        assessmentQuestion = "What is distance?",
                        correctAnswer = "Space between two objects",
                        explanation = "Gravitational force depends on distance between objects."
                    )
                ),
                estimatedDifficulty = "moderate"
            ),

            // Math - Algebra
            "math_algebra" to PrerequisiteKnowledge(
                topicId = "math_algebra",
                topicName = "Algebra",
                prerequisites = listOf(
                    PrerequisiteConcept(
                        conceptId = "arithmetic_operations",
                        conceptName = "Basic Arithmetic",
                        description = "Addition, subtraction, multiplication, division",
                        importance = "essential",
                        assessmentQuestion = "What is 5 × 3 + 2?",
                        correctAnswer = "17",
                        explanation = "You need to know basic operations for algebra."
                    ),
                    PrerequisiteConcept(
                        conceptId = "fractions",
                        conceptName = "Fractions",
                        description = "Understanding fractions",
                        importance = "essential",
                        assessmentQuestion = "What is 1/2 + 1/4?",
                        correctAnswer = "3/4",
                        explanation = "Fractions appear frequently in algebra."
                    ),
                    PrerequisiteConcept(
                        conceptId = "order_of_operations",
                        conceptName = "Order of Operations (PEMDAS)",
                        description = "Knowing which operations to do first",
                        importance = "essential",
                        assessmentQuestion = "In what order should you solve (2 + 3) × 4?",
                        correctAnswer = "Parentheses first, then multiply",
                        explanation = "Order of operations is crucial in algebra."
                    )
                ),
                estimatedDifficulty = "easy"
            ),

            // Math - Calculus
            "math_calculus" to PrerequisiteKnowledge(
                topicId = "math_calculus",
                topicName = "Calculus",
                prerequisites = listOf(
                    PrerequisiteConcept(
                        conceptId = "algebra_mastery",
                        conceptName = "Algebra",
                        description = "Solving equations and understanding variables",
                        importance = "essential",
                        assessmentQuestion = "Can you solve: 2x + 5 = 15?",
                        correctAnswer = "x = 5",
                        explanation = "Calculus builds heavily on algebra."
                    ),
                    PrerequisiteConcept(
                        conceptId = "functions",
                        conceptName = "Functions",
                        description = "Understanding f(x) notation",
                        importance = "essential",
                        assessmentQuestion = "If f(x) = 2x + 1, what is f(3)?",
                        correctAnswer = "7",
                        explanation = "Calculus is all about functions and their rates of change."
                    ),
                    PrerequisiteConcept(
                        conceptId = "graphs",
                        conceptName = "Graphing",
                        description = "Reading and plotting graphs",
                        importance = "recommended",
                        assessmentQuestion = "What does the slope of a line represent?",
                        correctAnswer = "Rate of change",
                        explanation = "Understanding graphs helps visualize calculus concepts."
                    )
                ),
                estimatedDifficulty = "hard"
            ),

            // Chemistry - Chemical Reactions
            "chemistry_chemical reactions" to PrerequisiteKnowledge(
                topicId = "chemistry_reactions",
                topicName = "Chemical Reactions",
                prerequisites = listOf(
                    PrerequisiteConcept(
                        conceptId = "atoms_molecules",
                        conceptName = "Atoms and Molecules",
                        description = "Basic structure of matter",
                        importance = "essential",
                        assessmentQuestion = "What is the smallest unit of an element?",
                        correctAnswer = "Atom",
                        explanation = "Chemical reactions involve atoms rearranging."
                    ),
                    PrerequisiteConcept(
                        conceptId = "elements",
                        conceptName = "Elements",
                        description = "Understanding chemical elements",
                        importance = "essential",
                        assessmentQuestion = "What is an element?",
                        correctAnswer = "A pure substance made of one type of atom",
                        explanation = "Elements combine in chemical reactions."
                    ),
                    PrerequisiteConcept(
                        conceptId = "chemical_symbols",
                        conceptName = "Chemical Symbols",
                        description = "Reading chemical formulas",
                        importance = "recommended",
                        assessmentQuestion = "What does H₂O represent?",
                        correctAnswer = "Water (2 hydrogen, 1 oxygen)",
                        explanation = "You need to read chemical formulas to understand reactions."
                    )
                ),
                estimatedDifficulty = "moderate"
            )
        )
    }
}

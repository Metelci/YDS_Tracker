package com.mtlc.studyplan.reading

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class IntRangeSerializerTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes week range from array`() {
        val payload = """
            {"id":"sample","title":"Sample","content":"Sentence one. Sentence two.","difficulty":"A2","estimatedTime":5,"topics":["topic"],"vocabularyFocus":["word"],"grammarPatterns":["pattern"],"wordCount":40,"averageSentenceLength":10.0,"complexityScore":0.3,"weekAppropriate":[1,3],"comprehensionQuestions":[],"sourceType":"CURATED"}
        """.trimIndent()

        val content = json.decodeFromString<ReadingContent>(payload)

        assertEquals(1..3, content.weekAppropriate)
    }

    @Test
    fun `serializes int range as array`() {
        val rangeJson = json.encodeToString(IntRangeSerializer, 2..5)
        assertEquals("[2,5]", rangeJson)
    }
}

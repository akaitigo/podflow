package com.akaitigo.podflow.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SocialLinkConverterTest {

    private val converter = SocialLinkListConverter()

    @Test
    fun `convertToDatabaseColumn serializes list to JSON`() {
        val links = listOf(
            SocialLink("https://twitter.com/guest"),
            SocialLink("https://linkedin.com/in/guest"),
        )
        val json = converter.convertToDatabaseColumn(links)
        assertEquals(
            """[{"url":"https://twitter.com/guest"},{"url":"https://linkedin.com/in/guest"}]""",
            json,
        )
    }

    @Test
    fun `convertToDatabaseColumn returns null for empty list`() {
        assertNull(converter.convertToDatabaseColumn(emptyList()))
    }

    @Test
    fun `convertToDatabaseColumn returns null for null input`() {
        assertNull(converter.convertToDatabaseColumn(null))
    }

    @Test
    fun `convertToEntityAttribute deserializes JSON to list`() {
        val json = """[{"url":"https://twitter.com/guest"}]"""
        val links = converter.convertToEntityAttribute(json)
        assertEquals(1, links.size)
        assertEquals("https://twitter.com/guest", links[0].url)
    }

    @Test
    fun `convertToEntityAttribute returns empty list for null`() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty())
    }

    @Test
    fun `convertToEntityAttribute returns empty list for blank string`() {
        assertTrue(converter.convertToEntityAttribute("  ").isEmpty())
    }

    @Test
    fun `roundtrip preserves all links`() {
        val original = listOf(
            SocialLink("https://twitter.com/a"),
            SocialLink("https://github.com/b"),
            SocialLink("https://linkedin.com/in/c"),
        )
        val json = converter.convertToDatabaseColumn(original)
        val restored = converter.convertToEntityAttribute(json)
        assertEquals(original, restored)
    }
}

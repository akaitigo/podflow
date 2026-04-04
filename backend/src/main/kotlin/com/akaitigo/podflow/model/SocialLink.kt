package com.akaitigo.podflow.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * A single social media profile URL.
 *
 * @property url A fully-qualified URL (https scheme expected).
 *
 * [@JsonCreator] is required because Jackson (without the Kotlin module) cannot introspect
 * the primary constructor of a Kotlin data class automatically.
 */
data class SocialLink @JsonCreator constructor(
    @param:JsonProperty("url") val url: String,
)

private val MAPPER = ObjectMapper()
private val SOCIAL_LINK_LIST_TYPE = object : TypeReference<List<SocialLink>>() {}

/** JPA AttributeConverter that serializes a list of [SocialLink] as a JSON array in TEXT column. */
@Converter
class SocialLinkListConverter : AttributeConverter<List<SocialLink>, String> {

    override fun convertToDatabaseColumn(attribute: List<SocialLink>?): String? {
        if (attribute.isNullOrEmpty()) {
            return null
        }
        return MAPPER.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(dbData: String?): List<SocialLink> {
        if (dbData.isNullOrBlank()) {
            return emptyList()
        }
        return MAPPER.readValue(dbData, SOCIAL_LINK_LIST_TYPE)
    }
}

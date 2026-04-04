package com.akaitigo.podflow.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.jboss.logging.Logger

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

    private val log: Logger = Logger.getLogger(SocialLinkListConverter::class.java)

    override fun convertToDatabaseColumn(attribute: List<SocialLink>?): String? {
        if (attribute.isNullOrEmpty()) {
            return null
        }
        return MAPPER.writeValueAsString(attribute)
    }

    /**
     * Deserializes the JSON array stored in the DB back to a list of [SocialLink].
     *
     * If the stored value is malformed JSON (e.g., due to manual DB edits or migration bugs),
     * logs a warning and returns an empty list rather than propagating an exception that
     * would crash the entire query result set.
     */
    override fun convertToEntityAttribute(dbData: String?): List<SocialLink> {
        if (dbData.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            MAPPER.readValue(dbData, SOCIAL_LINK_LIST_TYPE)
        } catch (e: JacksonException) {
            log.warnf(e, "Failed to deserialize social_links JSON; returning empty list. Raw value: %s", dbData)
            emptyList()
        }
    }
}

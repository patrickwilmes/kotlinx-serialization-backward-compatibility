/*
* Copyright (c) 2024, Patrick Wilmes <p.wilmes89@gmail.com>
* All rights reserved.
*
* SPDX-License-Identifier: BSD-2-Clause
*/
package com.bit.lake

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double

sealed interface DocumentPositionDto {
    val datevRevenueAccountNumber: String?
    val taxRate: TaxRate
}

@Serializable
data class CreditNoteDocumentPositionDto(
    override val datevRevenueAccountNumber: String? = null,
    override val taxRate: TaxRate,
) : DocumentPositionDto

@Serializable(with = DocumentDtoSerializer::class)
data class RevenueDocumentPositionDto(
    override val datevRevenueAccountNumber: String? = null,
    override val taxRate: TaxRate,
) : DocumentPositionDto

@Serializable
data class TaxRate(val amount: Double, val countryCode: String)

class DocumentDtoSerializer : KSerializer<RevenueDocumentPositionDto> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("DocumentPositionDto") {
            element(
                "datevRevenueAccountNumber",
                descriptor = String.serializer().descriptor.nullable,
            )
            element("taxRate", JsonElement.serializer().descriptor)
        }

    override fun serialize(
        encoder: Encoder,
        value: RevenueDocumentPositionDto
    ) {
        val compositeOutput = encoder.beginStructure(descriptor)
        compositeOutput.encodeStringElement(descriptor, 0, value.datevRevenueAccountNumber ?: "")
        compositeOutput.encodeSerializableElement(
            descriptor,
            1,
            TaxRate.serializer(),
            value.taxRate
        )
        compositeOutput.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): RevenueDocumentPositionDto {
        val dec = decoder.beginStructure(descriptor)
        var datevRevenueAccountNumber: String? = null
        var taxRate: TaxRate? = null

        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> datevRevenueAccountNumber = dec.decodeStringElement(descriptor, index)
                1 -> {
                    val jsonElement =
                        dec.decodeSerializableElement(descriptor, index, JsonElement.serializer())
                    taxRate = if (jsonElement is JsonPrimitive) {
                        TaxRate(jsonElement.double, "DE")
                    } else {
                        Json.decodeFromJsonElement(TaxRate.serializer(), jsonElement)
                    }
                }

                else -> throw SerializationException("Unknown index $index")
            }
        }

        dec.endStructure(descriptor)
        return RevenueDocumentPositionDto(
            datevRevenueAccountNumber
                ?: throw SerializationException("Missing value for otherField"),
            taxRate ?: throw SerializationException("Missing value for taxRate")
        )
    }

}

val json = Json {}

fun main() {
    val stringV1 = "{\"datevRevenueAccountNumber\":\"Hello\",\"taxRate\":12.0}"
    val stringV2 =
        "{\"datevRevenueAccountNumber\":\"Hello\",\"taxRate\":{ \"amount\": 12.0, \"countryCode\": \"DE\"}}"
    println(json.decodeFromString<RevenueDocumentPositionDto>(stringV1))
    println(json.decodeFromString<RevenueDocumentPositionDto>(stringV2))
}

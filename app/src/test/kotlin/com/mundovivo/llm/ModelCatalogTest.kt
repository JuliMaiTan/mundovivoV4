package com.mundovivo.llm

import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {

    @Test
    fun nonPlaceholderSha256HashesAreValidLowercaseHex() {
        val hashRegex = Regex("^[0-9a-f]{64}$")

        ModelCatalog.getSupportedModels()
            .filterNot { it.sha256.startsWith("PLACEHOLDER_") }
            .forEach { model ->
                assertTrue(
                    "${model.id.value} has invalid SHA256: ${model.sha256}",
                    model.sha256.matches(hashRegex)
                )
            }
    }
}

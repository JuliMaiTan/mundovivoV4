package com.mundovivo.util

import java.util.UUID

/**
 * Gerador de IDs únicos para entidades do jogo
 */
object Ids {
    fun generate(prefix: String): String = "${prefix}_${UUID.randomUUID()}"

    fun worldId() = generate("world")
    fun storyId() = generate("story")
    fun entityId() = generate("entity")
    fun npcId() = generate("npc")
    fun locationId() = generate("loc")
    fun eventId() = generate("event")
    fun turnId() = generate("turn")
}

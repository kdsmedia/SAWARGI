package com.altomedia.sawargi.data

import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Direct-message (chat / messenger) repository.
 * Conversations store users in the ordered pair (user_a < user_b) so a single,
 * unique conversation row exists between any two people.
 */
class ChatRepository(private val social: SocialRepository) {

    private fun pair(a: String, b: String): Pair<String, String> =
        if (a < b) a to b else b to a

    suspend fun getOrCreateConversation(me: String, other: String): Result<Long> = runCatching {
        val (a, b) = pair(me, other)
        val existing = Supabase.postgrest.from("conversations")
            .select(Columns.raw("id, user_a, user_b")) {
                filter { eq("user_a", a) }
                filter { eq("user_b", b) }
                limit(1)
            }
            .decodeList<Conversation>()
        if (existing.isNotEmpty()) return@runCatching existing.first().id!!
        val body = buildJsonObject {
            put("user_a", a)
            put("user_b", b)
        }
        Supabase.postgrest.from("conversations")
            .insert(body)
            .decodeSingle<Conversation>()
            .id!!
    }

    /** Conversations the current user participates in, newest message first. */
    suspend fun fetchConversations(me: String): Result<List<Conversation>> = runCatching {
        Supabase.postgrest.from("conversations")
            .select(Columns.raw("*")) {
                filter { or { eq("user_a", me) } }
                order("last_message_at", Order.DESCENDING)
                limit(200)
            }
            .decodeList<Conversation>() +
            Supabase.postgrest.from("conversations")
                .select(Columns.raw("*")) {
                    filter { or { eq("user_b", me) } }
                    order("last_message_at", Order.DESCENDING)
                    limit(200)
                }
                .decodeList<Conversation>()
                .filter { c -> c.userA != me }
    }

    suspend fun fetchMessages(conversationId: Long): Result<List<Message>> = runCatching {
        Supabase.postgrest.from("messages")
            .select(Columns.raw("*")) {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.ASCENDING)
                limit(500)
            }
            .decodeList<Message>()
    }

    suspend fun sendMessage(senderId: String, conversationId: Long, text: String): Result<Message> = runCatching {
        val body = buildJsonObject {
            put("conversation_id", conversationId)
            put("sender_id", senderId)
            put("text", text)
            put("type", "text")
            put("media_type", "none")
            put("seen", false)
        }
        Supabase.postgrest.from("messages")
            .insert(body)
            .decodeSingle<Message>()
    }

    suspend fun fetchProfile(userId: String): Profile? = social.fetchProfile(userId).getOrNull()
}
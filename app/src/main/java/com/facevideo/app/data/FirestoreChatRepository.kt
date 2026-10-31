package com.facevideo.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Timestamp

data class RemoteChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val createdAt: Timestamp?
)

/**
 * Firestore data layer for one-to-one and group conversations.
 * A chat document must exist and include the authenticated users in participantIds.
 */
class FirestoreChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun createChat(
        participantIds: List<String>,
        title: String? = null,
        onComplete: (chatId: String?, error: Exception?) -> Unit
    ) {
        val members = participantIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        require(members.size in 2..50) { "A chat must have between 2 and 50 participants." }

        val reference = firestore.collection("chats").document()
        val data = hashMapOf<String, Any>(
            "participantIds" to members,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastMessage" to "",
            "lastMessageAt" to FieldValue.serverTimestamp()
        )
        if (!title.isNullOrBlank()) data["title"] = title.trim().take(80)

        reference.set(data)
            .addOnSuccessListener { onComplete(reference.id, null) }
            .addOnFailureListener { error -> onComplete(null, error) }
    }

    fun observeMessages(
        chatId: String,
        onMessages: (List<RemoteChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val senderId = document.getString("senderId") ?: return@mapNotNull null
                    val text = document.getString("text") ?: return@mapNotNull null
                    RemoteChatMessage(
                        id = document.id,
                        senderId = senderId,
                        text = text,
                        createdAt = document.getTimestamp("createdAt")
                    )
                }
                onMessages(messages)
            }
    }

    fun sendText(
        chatId: String,
        senderId: String,
        text: String,
        onComplete: (Exception?) -> Unit
    ) {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty() && cleanText.length <= 4000) {
            "Message must contain between 1 and 4000 characters."
        }

        val data = mapOf(
            "senderId" to senderId,
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(data)
            .addOnSuccessListener { onComplete(null) }
            .addOnFailureListener { error -> onComplete(error) }
    }
}

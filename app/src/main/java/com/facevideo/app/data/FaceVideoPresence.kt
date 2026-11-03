package com.facevideo.app.data

import kotlinx.serialization.Serializable

/**
 * Small Realtime Presence payload used for live online/offline indicators.
 * The channel key is unique per connection so several devices can be online
 * for the same account at the same time.
 */
@Serializable
data class FaceVideoPresence(
    val userId: String
)

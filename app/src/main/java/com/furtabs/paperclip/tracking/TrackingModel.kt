package com.furtabs.paperclip.tracking

data class TrackingResponse(
    val tracking_code: String?,
    val events: List<TrackingEvent>?,
    val estimatedDeliveryDate: String? = null
)

data class TrackingEvent(
    val date: String?,
    val time: String?,
    val location: String?,
    val status: String?,
    val subStatus: List<String>?
)
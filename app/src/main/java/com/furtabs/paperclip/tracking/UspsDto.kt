package com.furtabs.paperclip.tracking

import com.google.gson.annotations.SerializedName

// Minimal model for USPS JSON tracking response (possibly adjusted per API format)
data class UspsTrackingResponse(
    @SerializedName("trackingNumber") val trackingNumber: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("events") val events: List<UspsTrackingEvent>?,
    @SerializedName("trackingEvents") val trackingEvents: List<UspsTrackingEvent>?
)

data class UspsTrackingEvent(
    @SerializedName("eventDate") val eventDate: String?,
    @SerializedName("eventTime") val eventTime: String?,
    @SerializedName("eventTimestamp") val eventTimestamp: String?,
    @SerializedName("eventLocation") val eventLocation: String?,
    @SerializedName("eventCity") val eventCity: String?,
    @SerializedName("eventState") val eventState: String?,
    @SerializedName("eventZIPCode") val eventZIPCode: String?,
    @SerializedName("eventDescription") val eventDescription: String?,
    @SerializedName("eventType") val eventType: String?,
    @SerializedName("description") val description: String?
)

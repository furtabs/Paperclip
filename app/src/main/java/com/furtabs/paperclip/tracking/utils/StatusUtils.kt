package com.furtabs.paperclip.tracking.utils

fun String?.isDeliveredStatus(): Boolean {
    if (this == null) return false
    val normalized = this.lowercase().trim()

    val deliveryKeywords = listOf(
        "entregue",
        "entrega realizada",
        "delivered"
    )

    return deliveryKeywords.any { normalized.contains(it) }
}
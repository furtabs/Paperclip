package com.furtabs.paperclip.tracking.utils

import com.furtabs.paperclip.R

enum class TrackingCarrier(val nameRes: Int) {
    USPS(R.string.carrier_usps) {
        override fun matches(code: String): Boolean {
            val normalized = code
                .replace(Regex("[^A-Za-z0-9]"), "")
                .uppercase()
                .trim()

            val isStartsWith9 = normalized.startsWith("9")
            if (isStartsWith9) return true

            val isEndsWithUs = normalized.endsWith("US")
            if (isEndsWithUs) return true

            return false
        }
    },
    UNKNOWN(R.string.unknown) {
        override fun matches(code: String) = false
    };

    abstract fun matches(code: String): Boolean

    companion object {
        fun fromCode(code: String): TrackingCarrier {
            val clean = code
                .replace(Regex("[^A-Za-z0-9]"), "")
                .uppercase()
                .trim()

            if (clean.isEmpty()) return UNKNOWN
            if (clean.startsWith("9")) return USPS
            if (clean.endsWith("US")) return USPS

            return entries.find { it.matches(clean) } ?: UNKNOWN
        }
    }
}
package com.example.ztemeshmanager

object RouterDefaults {
    const val HV3601P = "HV3601P_V93_FPT"
    const val F6601P = "F6601P_VT9310P2N7"

    fun urlFor(modelKey: String?): String =
        if (modelKey == F6601P) "https://192.168.1.1/" else "https://192.168.1.11/"

    fun displayName(modelKey: String?): String =
        if (modelKey == F6601P) "F6601P" else "HV3601P v9.3"

    fun isDefaultUrl(url: String?, modelKey: String?): Boolean {
        fun normalized(value: String?): String = value.orEmpty().trim().trimEnd('/').lowercase()
        return normalized(url) == normalized(urlFor(modelKey))
    }
}

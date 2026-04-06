package com.example.smartalarm.platform.alarm

import com.example.smartalarm.R

object AlarmSoundCatalog {

    data class AlarmSound(
        val key: String,
        val displayName: String,
        val resId: Int
    )

    fun getAvailableSounds(): List<AlarmSound> {
        return R.raw::class.java.declaredFields
            .mapNotNull { field ->
                try {
                    val resId = field.getInt(null)
                    val key = field.name
                    AlarmSound(
                        key = key,
                        displayName = key
                            .split("_")
                            .joinToString(" ") { part ->
                                part.replaceFirstChar { char ->
                                    if (char.isLowerCase()) char.titlecase() else char.toString()
                                }
                            },
                        resId = resId
                    )
                } catch (_: Exception) {
                    null
                }
            }
            .sortedBy { it.displayName }
    }

    fun getSoundByKey(key: String): AlarmSound? {
        return getAvailableSounds().firstOrNull { it.key == key }
    }

    fun getDefaultSound(): AlarmSound? {
        return getAvailableSounds().firstOrNull()
    }
}
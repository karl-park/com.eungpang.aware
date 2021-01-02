package com.eungpang.applocker.domain.item

import java.io.Serializable

interface Item : Serializable {
    val name: String
    val recentLaunchDateTime: Long
    val totalLaunchTimeInSeconds: Long
    val packageName: String
    val logoUrl: String

    companion object {
        const val KEY_SERIALIZABLE = "eungpang_timechecker_serializable_key"
    }
}
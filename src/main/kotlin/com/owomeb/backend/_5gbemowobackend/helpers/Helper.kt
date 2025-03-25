package com.owomeb.backend._5gbemowobackend.helpers

import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseRepository
import org.springframework.stereotype.Component

@Component
class Helper(
    private val baseRepository: BaseRepository,
    private val appPathsConfig: AppPathsConfig
){
    fun setPathByURL(baseURL: String): String {
        val base = baseRepository.findBySourceUrl(baseURL)
        if (base != null) {
            return appPathsConfig.getNormDirectory(base.id.toString())
        }
        return ""
    }
}
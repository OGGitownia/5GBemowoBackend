package com.owomeb.backend._5gbemowobackend.random

import com.owomeb.backend._5gbemowobackend.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.appControllers.BaseRepository
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
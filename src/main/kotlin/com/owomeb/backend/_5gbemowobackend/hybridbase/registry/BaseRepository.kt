package com.owomeb.backend._5gbemowobackend.hybridbase.registry

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BaseRepository : JpaRepository<BaseEntity, Long> {
    fun findBySourceUrl(sourceUrl: String): BaseEntity?
}

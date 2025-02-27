package com.owomeb.backend._5gbemowobackend.baseCreators

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import java.time.LocalDateTime

@Document(indexName = "fragments")
data class FragmentEntity(
    @Id val id: String,
    val text: String,
    val embedding: List<Float>,
    val containsTable: Boolean,
    val containsImage: Boolean,
    val length: Int,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

package com.example.keyfairy.feature_reports.data.mapper

import com.example.keyfairy.feature_reports.data.remote.dto.response.PosturalErrorItem
import com.example.keyfairy.feature_reports.data.remote.dto.response.PosturalErrorResponse
import com.example.keyfairy.feature_reports.domain.model.PosturalError
import com.example.keyfairy.feature_reports.domain.model.PosturalErrorList

object PosturalErrorMapper {

    fun toDomain(dto: PosturalErrorItem): PosturalError {
        return PosturalError(
            minSecInit = dto.minSecInit,
            minSecEnd = dto.minSecEnd,
            explication = dto.explication
        )
    }

    fun toDomain(dto: PosturalErrorResponse): PosturalErrorList {
        return PosturalErrorList(
            numErrors = dto.numErrors,
            errors = dto.errors.map { toDomain(it) }
        )
    }
}
package com.example.keyfairy.feature_reports.data.mapper

import com.example.keyfairy.feature_reports.data.remote.dto.response.MusicalErrorItem
import com.example.keyfairy.feature_reports.data.remote.dto.response.MusicalErrorResponse
import com.example.keyfairy.feature_reports.domain.model.MusicalError
import com.example.keyfairy.feature_reports.domain.model.MusicalErrorList

object MusicalErrorMapper {
    fun toDomain(dto: MusicalErrorItem): MusicalError {
        return MusicalError(
            min_sec = dto.min_sec,
            note_played = dto.note_played,
            note_correct = dto.note_correct,
        )
    }

    fun toDomain(dto: MusicalErrorResponse): MusicalErrorList {
        return MusicalErrorList(
            numErrors = dto.numErrors,
            errors = dto.errors.map { toDomain(it) }
        )
    }
}
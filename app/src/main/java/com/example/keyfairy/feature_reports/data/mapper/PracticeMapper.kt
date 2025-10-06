package com.example.keyfairy.feature_reports.data.mapper

import com.example.keyfairy.feature_reports.data.remote.dto.response.PracticeItemDto
import com.example.keyfairy.feature_reports.data.remote.dto.response.PracticeResponseDto
import com.example.keyfairy.feature_reports.domain.model.PracticeItem
import com.example.keyfairy.feature_reports.domain.model.PracticeList

object PracticeMapper {

    fun toDomain(dto: PracticeItemDto): PracticeItem {
        return PracticeItem(
            practiceId = dto.practiceId,
            scale = dto.scale,
            scaleType = dto.scaleType,
            duration = dto.duration,
            bpm = dto.bpm,
            figure = dto.figure,
            octaves = dto.octaves,
            date = dto.date,
            time = dto.time,
            state = dto.state,
            localVideoUrl = dto.localVideoUrl,
            pdfUrl = dto.pdfUrl
        )
    }

    fun toDomain(dto: PracticeResponseDto): PracticeList {
        return PracticeList(
            numPractices = dto.numPractices,
            practices = dto.practices.map { toDomain(it) }
        )
    }
}

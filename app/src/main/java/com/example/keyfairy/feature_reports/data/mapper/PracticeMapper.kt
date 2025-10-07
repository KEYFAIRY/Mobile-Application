package com.example.keyfairy.feature_reports.data.mapper

import com.example.keyfairy.feature_reports.data.remote.dto.response.PracticeItem
import com.example.keyfairy.feature_reports.data.remote.dto.response.PracticeResponse
import com.example.keyfairy.feature_reports.domain.model.Practice
import com.example.keyfairy.feature_reports.domain.model.PracticeList

object PracticeMapper {

    fun toDomain(dto: PracticeItem): Practice {
        return Practice(
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

    fun toDomain(dto: PracticeResponse): PracticeList {
        return PracticeList(
            numPractices = dto.numPractices,
            practices = dto.practices.map { toDomain(it) }
        )
    }
}

package com.example.keyfairy.feature_reports.domain.model

data class PosturalErrorList(
    val numErrors: Int,
    val errors: List<PosturalError>
)

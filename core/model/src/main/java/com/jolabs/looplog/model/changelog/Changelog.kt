package com.jolabs.looplog.model.changelog

data class Changelog(
    val versionCode: Long,
    val versionName: String,
    val title: String,
    val notes: List<String>,
    val detailsUrl: String?,
)

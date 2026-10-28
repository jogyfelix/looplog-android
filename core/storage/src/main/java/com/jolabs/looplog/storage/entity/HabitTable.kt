package com.jolabs.looplog.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class HabitTable(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long
    )
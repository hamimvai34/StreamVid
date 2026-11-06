package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val handle: String,
    val title: String,
    val description: String,
    val avatarUrl: String,
    val category: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "videos",
    indices = [Index(value = ["channelId"])]
)
data class VideoEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val channelTitle: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val publishedAt: Long,
    val publishedText: String,
    val durationText: String = "HD"
)

@Entity(tableName = "admin_settings")
data class AdminSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

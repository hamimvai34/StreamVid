package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamDao {
    @Query("SELECT * FROM channels ORDER BY createdAt DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY createdAt DESC")
    suspend fun getChannelsOnce(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannelById(channelId: String)

    @Query("DELETE FROM videos WHERE channelId = :channelId")
    suspend fun deleteVideosByChannel(channelId: String)

    @Transaction
    suspend fun deleteChannelAndVideos(channelId: String) {
        deleteVideosByChannel(channelId)
        deleteChannelById(channelId)
    }

    @Query("SELECT * FROM videos WHERE channelId IN (SELECT id FROM channels) ORDER BY publishedAt DESC")
    fun getAllWhitelistedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE channelId = :channelId ORDER BY publishedAt DESC")
    fun getVideosByChannel(channelId: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Query("SELECT * FROM admin_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AdminSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AdminSettingEntity)
}

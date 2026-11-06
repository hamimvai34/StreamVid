package com.example.data.repository

import com.example.data.local.AdminSettingEntity
import com.example.data.local.ChannelEntity
import com.example.data.local.StreamDao
import com.example.data.local.VideoEntity
import com.example.data.remote.ResolvedChannelResult
import com.example.data.remote.YouTubeChannelResolver
import com.example.data.remote.YouTubeFetchMethod
import kotlinx.coroutines.flow.Flow

class StreamRepository(
    private val dao: StreamDao,
    private val resolver: YouTubeChannelResolver = YouTubeChannelResolver()
) {
    val channelsFlow: Flow<List<ChannelEntity>> = dao.getAllChannels()
    val whitelistedVideosFlow: Flow<List<VideoEntity>> = dao.getAllWhitelistedVideos()

    companion object {
        private const val KEY_ADMIN_PIN = "admin_pin_code"
        private const val KEY_FETCH_METHOD = "youtube_fetch_method"
        private const val KEY_SEEDED = "initial_channels_seeded"
        const val DEFAULT_ADMIN_PIN = "1234"
    }

    suspend fun ensureInitialDataSeeded() {
        val seeded = dao.getSetting(KEY_SEEDED)
        if (seeded != null) return

        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val initialChannels = listOf(
            ChannelEntity(
                id = "UCBJycsmduvYEL83R_U4JriQ",
                handle = "@mkbhd",
                title = "Marques Brownlee",
                description = "Quality Tech Videos, Hardware Engineering Breakdowns, and Consumer Electronics Reviews.",
                avatarUrl = "https://yt3.googleusercontent.com/lkH37D712tiyphnu0Id0D5MwwQ7IRuwgQLVD05iMXlDWO-kDHut3uI4MgIEAQ9StK0qOST7fiA=s176-c-k-c0x00ffffff-no-rj",
                category = "Technology",
                url = "https://www.youtube.com/@mkbhd",
                createdAt = now - 3 * dayMs,
                lastSyncedAt = now
            ),
            ChannelEntity(
                id = "UCsXVk37bltHxD1rDPwtNM8Q",
                handle = "@kurzgesagt",
                title = "Kurzgesagt – In a Nutshell",
                description = "Animated science, astrophysics, biology, and philosophical deep dives.",
                avatarUrl = "https://ui-avatars.com/api/?name=Kurzgesagt&background=0284C7&color=fff&size=256",
                category = "Science & Space",
                url = "https://www.youtube.com/@kurzgesagt",
                createdAt = now - 2 * dayMs,
                lastSyncedAt = now
            ),
            ChannelEntity(
                id = "UCYO_jab_esuFRV4b17AJtAw",
                handle = "@3blue1brown",
                title = "3Blue1Brown",
                description = "Visualizing mathematics, neural networks, calculus, and deep learning architectures.",
                avatarUrl = "https://ui-avatars.com/api/?name=3B1B&background=10B981&color=fff&size=256",
                category = "Math & AI",
                url = "https://www.youtube.com/@3blue1brown",
                createdAt = now - dayMs,
                lastSyncedAt = now
            )
        )

        val initialVideos = listOf(
            VideoEntity(
                id = "dtp6b76pMak",
                channelId = "UCBJycsmduvYEL83R_U4JriQ",
                channelTitle = "Marques Brownlee",
                title = "Apple Vision Pro Review: Tomorrow's Ideas, Today's Tech!",
                description = "An exhaustive analysis of spatial computing, micro-OLED displays, pass-through latency, eye tracking precision, and where mixed reality headsets go next.",
                thumbnailUrl = "https://i.ytimg.com/vi/dtp6b76pMak/hqdefault.jpg",
                publishedAt = now - 1 * dayMs,
                publishedText = "1d ago",
                durationText = "37:24"
            ),
            VideoEntity(
                id = "aircAruvnKk",
                channelId = "UCYO_jab_esuFRV4b17AJtAw",
                channelTitle = "3Blue1Brown",
                title = "But what is a neural network? | Deep Learning Chapter 1",
                description = "A visual and intuitive introduction to neural networks, hidden layers, weights, biases, and gradient descent optimization from first principles.",
                thumbnailUrl = "https://i.ytimg.com/vi/aircAruvnKk/hqdefault.jpg",
                publishedAt = now - 2 * dayMs,
                publishedText = "2d ago",
                durationText = "19:13"
            ),
            VideoEntity(
                id = "JhHMJCUmq28",
                channelId = "UCsXVk37bltHxD1rDPwtNM8Q",
                channelTitle = "Kurzgesagt – In a Nutshell",
                title = "Quantum Computers Explained – Limits of Human Technology",
                description = "How qubits, quantum superposition, interference, and entanglement unlock exponential speedups in cryptography and molecular simulation.",
                thumbnailUrl = "https://i.ytimg.com/vi/JhHMJCUmq28/hqdefault.jpg",
                publishedAt = now - 3 * dayMs,
                publishedText = "3d ago",
                durationText = "07:19"
            ),
            VideoEntity(
                id = "wjZofJX0v4M",
                channelId = "UCYO_jab_esuFRV4b17AJtAw",
                channelTitle = "3Blue1Brown",
                title = "But what is a GPT? Visual Intro to Transformers",
                description = "Breaking down the transformer architecture, token embeddings, key-query-value self-attention, and how large language models reason.",
                thumbnailUrl = "https://i.ytimg.com/vi/wjZofJX0v4M/hqdefault.jpg",
                publishedAt = now - 4 * dayMs,
                publishedText = "4d ago",
                durationText = "27:14"
            ),
            VideoEntity(
                id = "ulCdoCfw-bY",
                channelId = "UCsXVk37bltHxD1rDPwtNM8Q",
                channelTitle = "Kurzgesagt – In a Nutshell",
                title = "How to Build a Dyson Sphere – The Ultimate Megastructure",
                description = "Step-by-step planetary engineering guide to surrounding a star with a solar collector swarm to power an interstellar civilization.",
                thumbnailUrl = "https://i.ytimg.com/vi/ulCdoCfw-bY/hqdefault.jpg",
                publishedAt = now - 5 * dayMs,
                publishedText = "5d ago",
                durationText = "09:50"
            ),
            VideoEntity(
                id = "0e3GPea1Tyg",
                channelId = "UCBJycsmduvYEL83R_U4JriQ",
                channelTitle = "Marques Brownlee",
                title = "Smartphone Awards: Best Cameras, Battery & Design",
                description = "Evaluating the best flagship and value smartphones across camera sensor performance, battery efficiency, display brightness, and industrial design.",
                thumbnailUrl = "https://i.ytimg.com/vi/0e3GPea1Tyg/hqdefault.jpg",
                publishedAt = now - 6 * dayMs,
                publishedText = "6d ago",
                durationText = "24:15"
            )
        )

        dao.insertChannels(initialChannels)
        dao.insertVideos(initialVideos)
        dao.setSetting(AdminSettingEntity(KEY_ADMIN_PIN, DEFAULT_ADMIN_PIN))
        dao.setSetting(AdminSettingEntity(KEY_FETCH_METHOD, YouTubeFetchMethod.AUTO_CASCADE.id))
        dao.setSetting(AdminSettingEntity(KEY_SEEDED, "true"))
    }

    suspend fun getSavedFetchMethod(): YouTubeFetchMethod {
        val raw = dao.getSetting(KEY_FETCH_METHOD)?.value
        return YouTubeFetchMethod.fromId(raw)
    }

    suspend fun setSavedFetchMethod(method: YouTubeFetchMethod) {
        dao.setSetting(AdminSettingEntity(KEY_FETCH_METHOD, method.id))
    }

    suspend fun verifyAdminPin(enteredPin: String): Boolean {
        val stored = dao.getSetting(KEY_ADMIN_PIN)?.value ?: DEFAULT_ADMIN_PIN
        return enteredPin.trim() == stored
    }

    suspend fun updateAdminPin(newPin: String) {
        if (newPin.trim().length >= 4) {
            dao.setSetting(AdminSettingEntity(KEY_ADMIN_PIN, newPin.trim()))
        }
    }

    suspend fun addOrSyncChannelByLink(
        linkOrHandle: String,
        category: String,
        method: YouTubeFetchMethod
    ): Result<ResolvedChannelResult> {
        val resolved = resolver.resolveAndFetchChannel(linkOrHandle, category, method)
        return resolved.map { result ->
            dao.insertChannel(result.channel)
            dao.insertVideos(result.videos)
            result
        }
    }

    suspend fun addDirectVideosToChannel(
        channel: ChannelEntity,
        videoOrPlaylistUrl: String
    ): Result<List<VideoEntity>> {
        val res = resolver.resolveSingleVideoForChannel(channel, videoOrPlaylistUrl)
        return res.map { videos ->
            dao.insertVideos(videos)
            videos
        }
    }

    suspend fun syncSingleChannel(
        channel: ChannelEntity,
        method: YouTubeFetchMethod
    ): Result<ResolvedChannelResult> {
        val input = if (channel.id.startsWith("UC") && !channel.id.startsWith("UC_")) {
            channel.id
        } else {
            channel.handle.ifBlank { channel.url }
        }
        val res = resolver.resolveAndFetchChannel(input, channel.category, method)
        return res.map { data ->
            val updatedChannel = channel.copy(lastSyncedAt = System.currentTimeMillis())
            dao.insertChannel(updatedChannel)
            if (data.videos.isNotEmpty()) {
                dao.insertVideos(
                    data.videos.map { it.copy(channelId = channel.id, channelTitle = channel.title) }
                )
            }
            data
        }
    }

    suspend fun updateChannelDetails(
        channelId: String,
        newTitle: String,
        newCategory: String,
        newDescription: String
    ) {
        val existing = dao.getChannelById(channelId) ?: return
        val updated = existing.copy(
            title = newTitle.trim().ifBlank { existing.title },
            category = newCategory.trim().ifBlank { existing.category },
            description = newDescription.trim().ifBlank { existing.description }
        )
        dao.updateChannel(updated)
    }

    suspend fun deleteChannel(channelId: String) {
        dao.deleteChannelAndVideos(channelId)
    }

    suspend fun syncAllChannels(method: YouTubeFetchMethod): Pair<Int, String> {
        val channels = dao.getChannelsOnce()
        var syncedCount = 0
        var lastEngineReport = method.title
        for (channel in channels) {
            val input = if (channel.id.startsWith("UC") && !channel.id.startsWith("UC_")) {
                channel.id
            } else {
                channel.handle.ifBlank { channel.url }
            }
            val res = resolver.resolveAndFetchChannel(input, channel.category, method)
            res.onSuccess { data ->
                dao.insertChannel(
                    channel.copy(
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
                if (data.videos.isNotEmpty()) {
                    dao.insertVideos(
                        data.videos.map { it.copy(channelId = channel.id, channelTitle = channel.title) }
                    )
                }
                lastEngineReport = data.engineReport
                syncedCount++
            }
        }
        return syncedCount to lastEngineReport
    }
}

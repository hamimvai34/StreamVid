# StreamVault: Distraction-Free Curated Channel & Audio/Video Hub

A native Android application built with Jetpack Compose featuring dedicated User and Admin modes for curating YouTube channels, strictly isolated playback (zero external recommendations or navigation to unapproved channels), a built-in **"Play as Audio"** mode, offline local persistence, and Gemini 4 Argon high-thinking video intelligence.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Updated with Your Latest Requirements**:
> 1. **Strict Whitelisted Content Only (Zero Recommendations / No External Channel Visits)**: The embedded player locks down navigation using native overlay controls and strict URL interception (`shouldOverrideUrlLoading`), blocking YouTube end-screen recommendations, related video popups, and channel profile links. Users can *only* watch or listen to videos from channels explicitly added by the Admin.
> 2. **Dedicated "Play as Audio" Mode**: Every video can be played in **Video Mode** or **Audio-Only Mode** with a single tap, featuring a clean podcast-style audio player, playback speed controls, 15s skip buttons, and a persistent bottom mini-player.
> 3. **Ultra User-Friendly & Intuitive UI**: Designed for effortless, clutter-free navigation with large 48dp+ touch targets, clear action buttons ("Watch Video" vs. "Listen Audio" directly on cards), and high-contrast visual clarity.

- **Confirmed: Native Android with Jetpack Compose**: Full Android M3 native implementation with smooth edge-to-edge layout, responsive cards, and intuitive navigation.
- **Confirmed: Offline Admin & User Authentication**: Local role switching with an admin passcode protection dialog. Admins add, edit, or delete channel links; normal users have a locked-down, read-only viewing and listening experience.
- **Confirmed: Accurate Channel Link Resolution**: Support for any YouTube channel link format (`@handle`, `/channel/UC...`, custom URLs), extracting channel metadata and latest video uploads into the local Room cache.
- **Confirmed: Modern Streaming Dark Aesthetic**: High-contrast dark theme with intuitive cards, clear typography, and zero visual clutter.
- **Confirmed: Gemini 4 Argon High-Thinking Mode**: Integrated AI video summarizer and key-takeaway analyzer using `gemini-4-argon` with `ThinkingLevel.HIGH`.

---

### 1. Overview & Core Concept

- **What It Does**: StreamVault provides a strictly curated, distraction-free media hub. Admins add approved YouTube channels, and the app fetches and caches their uploaded videos. Users browse only those approved channels and their videos—with zero algorithmic recommendations, zero external links, and no way to wander onto other YouTube channels. Users can seamlessly switch between watching the video or listening in **Audio Mode**.
- **Target Audience / Persona**: Focused learners, families, educators, and listeners who want curated channel content with audio playback and zero rabbit-hole distractions.
- **Key Value**: 100% whitelisted content isolation, built-in audio-only listening mode, zero cloud dependency, and high-thinking AI summaries.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **User Mode (Distraction-Free Browsing, Watching & Listening)**:
   - **Home & Channel Filter**: Launches into a clean, user-friendly catalog showing a Featured Upload Hero card, horizontal **Approved Channel Pills** (tap a channel to see only its videos, or "All Channels"), and a fast search bar.
   - **User-Friendly Video Cards**: Each card clearly displays the thumbnail, duration/date, title, channel badge, and two direct action buttons:
     - **Watch Video** (opens full isolated video player)
     - **Play Audio** (starts audio-only mode immediately)
   - **Strictly Isolated Video Player**:
     - Plays only the selected video using a sandboxed player with custom native Jetpack Compose playback controls (Play/Pause, Seekbar, ±15s Skip, Speed selector, Audio/Video mode switch, Fullscreen toggle).
     - Blocks all YouTube logos, related video carousels, end-screen cards, and external links so the user can never navigate to another channel or external website.
     - Below the player, displays ONLY other uploaded videos from the **same approved channel**.
   - **"Play as Audio" Mode & Persistent Mini-Player**:
     - Tapping **Play as Audio** (from any video card or inside the player) switches to a dedicated, battery-friendly Audio Player view with artwork, waveform/progress scrubber, ±15s skip controls, and playback speed (0.75x to 2.0x).
     - Minimizing the player docks a **Persistent Bottom Mini-Player Bar**, allowing the user to keep listening to the audio stream while browsing other videos in the whitelisted catalog or reading AI summaries.

2. **Admin Mode (Curation & Channel Management)**:
   - Top bar shield/lock icon opens the **Admin Passcode Dialog** (default PIN `1234`, customizable by Admin).
   - **Admin Dashboard**: Displays all approved channels with video counts, last sync timestamp, a "Sync All" button, and an **"Add Channel"** button.
   - **Add Channel Flow**: Admin pastes any YouTube channel URL or `@handle`. The app resolves the channel ID, previews the channel name and avatar, fetches its uploaded videos, and saves them to the local Room database.
   - Admin can edit channel titles/categories, refresh individual channel uploads, or delete a channel (which automatically removes all of its cached videos).

3. **High-Thinking AI Video Insights (`gemini-4-argon`)**:
   - Inside the player screen, an **"AI Summary & Key Takeaways"** tab invokes `gemini-4-argon` with `ThinkingLevel.HIGH` to generate structured study notes, key points, and action items for the current video.

#### Visual Identity & Theme
- **Aesthetic Direction**: Ultra-clean, accessible dark streaming & podcast UI with generous spacing, unmistakable button labels, and zero clutter.
- **Color Palette**:
  - `Background`: Deep Obsidian (`#0B0E14`) and Soft Charcoal (`#121620`)
  - `Surface / Cards`: Elevated Slate (`#1A202C`) with clear borders (`#2D3748`)
  - `Primary Accent (Video)`: Vibrant Ruby (`#E50914` / `#FF334B`)
  - `Audio Mode Accent`: Warm Emerald / Teal (`#10B981`) to visually distinguish Audio-Only playback at a glance
  - `AI Thinking Accent`: Electric Cyan (`#00E5FF`)
  - `Text Hierarchy`: High-contrast Crisp White (`#F8FAFC`) titles and readable Slate (`#CBD5E1`) body text
- **Typography & Layout**:
  - Clear, legible typography with comfortable line heights and large tap areas (minimum 48.dp x 48.dp on all buttons).
  - Simple 2-tab or single-screen layout with zero confusing sub-menus.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Strict Content Sandbox (No Recommendations or External Channel Visits)**:
  - *Chosen Approach*: We intercept all touch interactions on the YouTube iframe surface and control playback exclusively via the YouTube IFrame JS bridge (`playVideo()`, `pauseVideo()`, `seekTo()`, `setPlaybackRate()`) paired with native Jetpack Compose controls. In addition, a custom `WebViewClient` blocks every navigation request (`shouldOverrideUrlLoading` returns `true` for all external URLs) and hides end-screen overlays the moment playback nears completion.
  - *Why*: Standard YouTube embeds show "More videos" from random channels when paused or ended, and allow clicking the channel watermark to leave the app. Intercepting touch events and driving the player via the JS bridge guarantees 100% isolation to only the admin's whitelisted videos.

- **Decision 2: Native-Controlled "Play as Audio" Mode**:
  - *Chosen Approach*: Keep the lightweight audio-stream bridge active in a hidden/1px background container when in Audio Mode while rendering a full native Compose Audio Player UI and a persistent bottom Mini-Player bar across screens.
  - *Why*: Gives users instantaneous, glitch-free switching between Video and Audio at the exact same timestamp without re-buffering, plus background-style browsing inside the app.

- **Decision 3: 100% Offline Local Database (Room) & Keyless Feed Resolution**:
  - *Chosen Approach*: Persist all approved channels, cached videos, and admin passcodes locally in Room SQLite, and fetch channel uploads via YouTube's public XML/HTML feed resolver.
  - *Why*: Works out of the box with zero cloud setup or API key barriers.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             MainActivity                                    │
│            (Edge-to-Edge, Dark Theme, Persistent Audio Mini-Player)         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│  User Catalog Hub   │     │ Isolated MediaView  │     │  Admin Dashboard    │
│ - Channel Pills     │     │ - Sandboxed Video   │     │ - Passcode Gate     │
│ - Search & Filter   │     │ - Audio-Only Mode   │     │ - Add Channel Link  │
│ - Direct Play Video │     │ - Native Controls   │     │ - Edit / Remove     │
│   or Audio Cards    │     │ - Same-Channel List │     │ - Sync Feeds        │
└──────────┬──────────┘     └──────────┬──────────┘     └──────────┬──────────┘
           │                           │                           │
           └───────────────────────────┼───────────────────────────┘
                                       ▼
                     ┌───────────────────────────────────┐
                     │         StreamViewModel           │
                     │  - Whitelisted Channels & Videos  │
                     │  - PlaybackController State       │
                     │    (Video vs Audio, Mini-Player)  │
                     │  - Admin Auth Session State       │
                     │  - Gemini 4 Argon High-Thinking   │
                     └─────────────────┬─────────────────┘
                                       │
                    ┌──────────────────┴──────────────────┐
                    ▼                                     ▼
        ┌───────────────────────┐             ┌───────────────────────┐
        │   ChannelRepository   │             │   GeminiAiService     │
        │ - Room DAO Storage    │             │ - gemini-4-argon      │
        │ - Channel ID Resolver │             │ - ThinkingLevel.HIGH  │
        │ - Uploads Feed Parser │             │ - Video Study Notes   │
        └───────────┬───────────┘             └───────────────────────┘
                    ▼
        ┌───────────────────────┐
        │     AppDatabase       │
        │ - ChannelEntity       │
        │ - VideoEntity         │
        │ - AdminSettingEntity  │
        └───────────────────────┘
```

#### Data Entities & Playback State
1. **`ChannelEntity`**:
   - `id`: String (Primary Key, YouTube channel ID)
   - `handle`: String
   - `title`: String
   - `description`: String
   - `avatarUrl`: String
   - `category`: String
   - `createdAt`: Long
   - `lastSyncedAt`: Long

2. **`VideoEntity`**:
   - `id`: String (Primary Key, YouTube video ID)
   - `channelId`: String (Indexed Foreign Key — cascades on channel delete)
   - `channelTitle`: String
   - `title`: String
   - `description`: String
   - `thumbnailUrl`: String
   - `publishedAt`: Long
   - `publishedText`: String

3. **`PlaybackState`**:
   - `activeVideo`: `VideoEntity?`
   - `isAudioOnly`: Boolean (toggles between Video Player and Audio-Only Mode)
   - `isMinimized`: Boolean (docks into the persistent bottom Mini-Player)
   - `isPlaying`: Boolean
   - `currentPositionSec`: Float
   - `durationSec`: Float
   - `playbackSpeed`: Float (`0.75f`, `1.0f`, `1.25f`, `1.5f`, `2.0f`)

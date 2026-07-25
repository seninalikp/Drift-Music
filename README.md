# SoundStream - Spotify-Style SoundCloud Music Web App

SoundStream is a production-grade, Spotify-inspired web music app built with React, TypeScript, Tailwind CSS, Zustand, and native HTML5 Audio. It integrates with SoundCloud's search API and provides a rich Demo Mode with high-quality streamable tracks out of the box.

---

## 🏗️ Architecture Overview

- **State Management**: **Zustand** global store (`src/store/usePlayerStore.ts`) manages atomic audio playback state, queue, volume, history, liked tracks, Client ID, and modals.
- **Shared Audio Core**: **Headless Audio Controller** (`src/components/AudioEngine.tsx`) wraps the native HTML5 `<audio>` element as a singleton listener. It coordinates audio ticks and queue transitions without re-rendering unrelated UI sub-trees.
- **Isolated API Layer**: `src/lib/soundcloud.ts` isolates all SoundCloud REST calls and fallback demo data. Swapping data providers is a single-file edit.
- **Persistence**: Client ID, API Mode, liked tracks, and listening history are automatically persisted in `localStorage`.

---

## ⚡ SoundCloud API Limitation Notice

SoundCloud's public API (`api.soundcloud.com`) requires an active Client ID to resolve and stream track media URLs. Public client IDs can expire or encounter CORS restrictions.

To ensure a seamless experience:
1. **Demo Mode (Default)**: Pre-loaded with high-quality streamable tracks across top genres (Synthwave, Lo-Fi, Cyberpunk, Deep House, Ambient).
2. **Live SoundCloud API Mode**: Enter your custom SoundCloud Client ID in the Settings panel or First-Launch Modal. The app validates the key in real-time and fallback warnings guide the user if a track stream is restricted.

---

## 🚀 Getting Started

### Installation & Execution
```bash
# Install dependencies
npm install

# Start local development server on port 3000
npm run dev
```

Open `http://localhost:3000` in your browser.

---

## 🎹 Keyboard Shortcuts

- **`Space`**: Play / Pause
- **`Shift + →`**: Next track
- **`Shift + ←`**: Previous track
- **`→`**: Seek forward 5s
- **`←`**: Seek backward 5s
- **`M`**: Toggle Mute
- **`L`**: Toggle Like on current track
- **`F`**: Toggle Fullscreen player
- **`Esc`**: Minimize player or close modals

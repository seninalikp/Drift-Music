import { create } from 'zustand';
import { PlayerStore, Track, Playlist, UserProfile, RepeatMode, ApiMode, ActiveTab } from '../types';
import { MOCK_TRACKS } from '../data/mockTracks';
import { setAsset, getAsset } from '../lib/idb';

const LOCAL_STORAGE_LIKED_KEY = 'soundstream_liked_tracks_v1';
const LOCAL_STORAGE_CLIENT_ID_KEY = 'soundstream_client_id_v1';
const LOCAL_STORAGE_MODE_KEY = 'soundstream_api_mode_v1';
const LOCAL_STORAGE_SEEN_MODAL_KEY = 'soundstream_seen_modal_v1';
const LOCAL_STORAGE_HISTORY_KEY = 'soundstream_history_v1';
const LOCAL_STORAGE_VOLUME_KEY = 'soundstream_volume_v1';
const LOCAL_STORAGE_PLAYLISTS_KEY = 'soundstream_playlists_v1';
const LOCAL_STORAGE_USER_NAME_KEY = 'soundstream_user_name_v1';
const LOCAL_STORAGE_USER_BIO_KEY = 'soundstream_user_bio_v1';

// Initial default playlist
const DEFAULT_INITIAL_PLAYLISTS: Playlist[] = [
  {
    id: 'pl-default-1',
    name: 'Chill Synth Essentials',
    description: 'High energy electronic beats and soothing synthwave rhythms.',
    coverUrl: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80',
    tracks: [MOCK_TRACKS[0], MOCK_TRACKS[1], MOCK_TRACKS[2]],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

// Load initial values from localStorage
const loadInitialLiked = (): Track[] => {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_LIKED_KEY);
    return raw ? JSON.parse(raw) : [MOCK_TRACKS[0], MOCK_TRACKS[1]];
  } catch {
    return [MOCK_TRACKS[0], MOCK_TRACKS[1]];
  }
};

const loadInitialHistory = (): Track[] => {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_HISTORY_KEY);
    return raw ? JSON.parse(raw) : [MOCK_TRACKS[0], MOCK_TRACKS[1], MOCK_TRACKS[2]];
  } catch {
    return [MOCK_TRACKS[0]];
  }
};

const loadInitialPlaylists = (): Playlist[] => {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_PLAYLISTS_KEY);
    return raw ? JSON.parse(raw) : DEFAULT_INITIAL_PLAYLISTS;
  } catch {
    return DEFAULT_INITIAL_PLAYLISTS;
  }
};

const loadInitialClientId = (): string => {
  return localStorage.getItem(LOCAL_STORAGE_CLIENT_ID_KEY) || '';
};

const loadInitialApiMode = (): ApiMode => {
  const stored = localStorage.getItem(LOCAL_STORAGE_MODE_KEY);
  return (stored as ApiMode) || 'demo';
};

const loadInitialSeenModal = (): boolean => {
  return localStorage.getItem(LOCAL_STORAGE_SEEN_MODAL_KEY) === 'true';
};

const loadInitialVolume = (): number => {
  const vol = localStorage.getItem(LOCAL_STORAGE_VOLUME_KEY);
  return vol !== null ? parseFloat(vol) : 0.8;
};

const savePlaylistsToStorage = (playlists: Playlist[]) => {
  try {
    localStorage.setItem(LOCAL_STORAGE_PLAYLISTS_KEY, JSON.stringify(playlists));
  } catch {
    // ignore
  }
};

export const usePlayerStore = create<PlayerStore>((set, get) => ({
  // Initial state
  currentTrack: MOCK_TRACKS[0],
  isPlaying: false,
  currentTime: 0,
  duration: MOCK_TRACKS[0].duration,
  volume: loadInitialVolume(),
  isMuted: false,
  repeatMode: 'off',
  shuffle: false,

  queue: MOCK_TRACKS,
  queueIndex: 0,

  userProfile: {
    displayName: localStorage.getItem(LOCAL_STORAGE_USER_NAME_KEY) || 'SoundStream Producer',
    avatarUrl: '',
    bio: localStorage.getItem(LOCAL_STORAGE_USER_BIO_KEY) || 'Music enthusiast & audio creator',
  },

  playlists: loadInitialPlaylists(),
  likedTracks: loadInitialLiked(),
  history: loadInitialHistory(),

  clientId: loadInitialClientId(),
  apiMode: loadInitialApiMode(),
  hasSeenClientIdModal: loadInitialSeenModal(),

  activeTab: 'home',
  isFullscreenOpen: false,
  isSettingsOpen: false,
  isProfileOpen: false,
  isQueueOpen: false,
  isAddToPlaylistOpen: false,
  trackToAddToPlaylist: null,
  selectedPlaylistIdForView: null,
  searchQuery: '',
  audioError: null,

  // Actions
  playTrack: (track: Track, newQueue?: Track[]) => {
    const currentQueue = newQueue || get().queue;
    const existingIndex = currentQueue.findIndex((t) => t.id === track.id);
    const updatedQueue = existingIndex !== -1 ? currentQueue : [track, ...currentQueue];
    const index = existingIndex !== -1 ? existingIndex : 0;

    // Update history
    const history = get().history.filter((t) => t.id !== track.id);
    const updatedHistory = [track, ...history].slice(0, 30);
    try {
      localStorage.setItem(LOCAL_STORAGE_HISTORY_KEY, JSON.stringify(updatedHistory));
    } catch {
      // Ignore
    }

    set({
      currentTrack: track,
      queue: updatedQueue,
      queueIndex: index,
      isPlaying: true,
      currentTime: 0,
      duration: track.duration || 180,
      history: updatedHistory,
      audioError: null,
    });
  },

  togglePlayPause: () => {
    const { isPlaying, currentTrack } = get();
    if (!currentTrack) {
      if (get().queue.length > 0) {
        get().playTrack(get().queue[0]);
      }
      return;
    }
    set({ isPlaying: !isPlaying });
  },

  pause: () => set({ isPlaying: false }),
  resume: () => set({ isPlaying: true }),

  seek: (time: number) => {
    set({ currentTime: time });
  },

  setVolume: (volume: number) => {
    const clamped = Math.max(0, Math.min(1, volume));
    try {
      localStorage.setItem(LOCAL_STORAGE_VOLUME_KEY, clamped.toString());
    } catch {
      // ignore
    }
    set({ volume: clamped, isMuted: clamped === 0 });
  },

  toggleMute: () => {
    const { isMuted } = get();
    set({ isMuted: !isMuted });
  },

  nextTrack: () => {
    const { queue, queueIndex, repeatMode, shuffle } = get();
    if (queue.length === 0) return;

    if (repeatMode === 'one') {
      set({ currentTime: 0, isPlaying: true });
      return;
    }

    let nextIdx = queueIndex + 1;
    if (shuffle) {
      nextIdx = Math.floor(Math.random() * queue.length);
    } else if (nextIdx >= queue.length) {
      if (repeatMode === 'all') {
        nextIdx = 0;
      } else {
        set({ isPlaying: false });
        return;
      }
    }

    const nextTrk = queue[nextIdx];
    if (nextTrk) {
      get().playTrack(nextTrk, queue);
    }
  },

  previousTrack: () => {
    const { queue, queueIndex, currentTime } = get();
    if (currentTime > 3) {
      set({ currentTime: 0 });
      return;
    }

    if (queue.length === 0) return;
    const prevIdx = queueIndex - 1 < 0 ? queue.length - 1 : queueIndex - 1;
    const prevTrk = queue[prevIdx];
    if (prevTrk) {
      get().playTrack(prevTrk, queue);
    }
  },

  setRepeatMode: (mode: RepeatMode) => set({ repeatMode: mode }),
  toggleShuffle: () => set((state) => ({ shuffle: !state.shuffle })),

  // User Profile
  updateUserProfile: async (profileUpdate) => {
    const current = get().userProfile;
    const updated = { ...current, ...profileUpdate, updatedAt: new Date().toISOString() };

    if (profileUpdate.displayName) {
      localStorage.setItem(LOCAL_STORAGE_USER_NAME_KEY, profileUpdate.displayName);
    }
    if (profileUpdate.bio !== undefined) {
      localStorage.setItem(LOCAL_STORAGE_USER_BIO_KEY, profileUpdate.bio);
    }
    if (profileUpdate.avatarUrl !== undefined) {
      // Store in IndexedDB to avoid localStorage capacity limits
      await setAsset('user_profile_avatar_v1', profileUpdate.avatarUrl);
    }

    set({ userProfile: updated });
  },

  loadUserProfile: async () => {
    const storedAvatar = await getAsset('user_profile_avatar_v1');
    if (storedAvatar) {
      set((state) => ({
        userProfile: {
          ...state.userProfile,
          avatarUrl: storedAvatar,
        },
      }));
    }
  },

  // Playlist Actions
  createPlaylist: (name, description, coverUrl) => {
    const newPlaylist: Playlist = {
      id: `pl-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      name: name.trim() || 'My Playlist',
      description: description?.trim() || '',
      coverUrl:
        coverUrl ||
        'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80',
      tracks: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    const updatedPlaylists = [newPlaylist, ...get().playlists];
    savePlaylistsToStorage(updatedPlaylists);
    set({ playlists: updatedPlaylists });
    return newPlaylist;
  },

  renamePlaylist: (id, name, description) => {
    const updated = get().playlists.map((pl) => {
      if (pl.id === id) {
        return {
          ...pl,
          name: name.trim() || pl.name,
          description: description !== undefined ? description.trim() : pl.description,
          updatedAt: new Date().toISOString(),
        };
      }
      return pl;
    });

    savePlaylistsToStorage(updated);
    set({ playlists: updated });
  },

  deletePlaylist: (id) => {
    const updated = get().playlists.filter((pl) => pl.id !== id);
    savePlaylistsToStorage(updated);
    set((state) => ({
      playlists: updated,
      selectedPlaylistIdForView:
        state.selectedPlaylistIdForView === id ? null : state.selectedPlaylistIdForView,
    }));
  },

  addTrackToPlaylist: (playlistId, track) => {
    const updated = get().playlists.map((pl) => {
      if (pl.id === playlistId) {
        const exists = pl.tracks.some((t) => t.id === track.id);
        if (exists) return pl;
        return {
          ...pl,
          tracks: [...pl.tracks, track],
          updatedAt: new Date().toISOString(),
        };
      }
      return pl;
    });

    savePlaylistsToStorage(updated);
    set({ playlists: updated });
  },

  removeTrackFromPlaylist: (playlistId, trackId) => {
    const updated = get().playlists.map((pl) => {
      if (pl.id === playlistId) {
        return {
          ...pl,
          tracks: pl.tracks.filter((t) => t.id !== trackId),
          updatedAt: new Date().toISOString(),
        };
      }
      return pl;
    });

    savePlaylistsToStorage(updated);
    set({ playlists: updated });
  },

  reorderPlaylistTracks: (playlistId, fromIndex, toIndex) => {
    const updated = get().playlists.map((pl) => {
      if (pl.id === playlistId) {
        const newTracks = [...pl.tracks];
        const [moved] = newTracks.splice(fromIndex, 1);
        newTracks.splice(toIndex, 0, moved);
        return {
          ...pl,
          tracks: newTracks,
          updatedAt: new Date().toISOString(),
        };
      }
      return pl;
    });

    savePlaylistsToStorage(updated);
    set({ playlists: updated });
  },

  toggleLikeTrack: (track: Track) => {
    const { likedTracks } = get();
    const isAlreadyLiked = likedTracks.some((t) => t.id === track.id);
    let updated: Track[];

    if (isAlreadyLiked) {
      updated = likedTracks.filter((t) => t.id !== track.id);
    } else {
      updated = [track, ...likedTracks];
    }

    try {
      localStorage.setItem(LOCAL_STORAGE_LIKED_KEY, JSON.stringify(updated));
    } catch {
      // Ignore
    }

    set({ likedTracks: updated });
  },

  isLiked: (trackId: string | number) => {
    return get().likedTracks.some((t) => t.id === trackId);
  },

  setClientId: (id: string) => {
    const clean = id.trim();
    try {
      localStorage.setItem(LOCAL_STORAGE_CLIENT_ID_KEY, clean);
    } catch {
      // ignore
    }
    set({ clientId: clean });
  },

  setApiMode: (mode: ApiMode) => {
    try {
      localStorage.setItem(LOCAL_STORAGE_MODE_KEY, mode);
    } catch {
      // ignore
    }
    set({ apiMode: mode });
  },

  setHasSeenClientIdModal: (seen: boolean) => {
    try {
      localStorage.setItem(LOCAL_STORAGE_SEEN_MODAL_KEY, seen ? 'true' : 'false');
    } catch {
      // ignore
    }
    set({ hasSeenClientIdModal: seen });
  },

  setActiveTab: (tab: ActiveTab) => set({ activeTab: tab }),
  setIsFullscreenOpen: (open: boolean) => set({ isFullscreenOpen: open }),
  setIsSettingsOpen: (open: boolean) => set({ isSettingsOpen: open }),
  setIsProfileOpen: (open: boolean) => set({ isProfileOpen: open }),
  setIsQueueOpen: (open: boolean) => set({ isQueueOpen: open }),
  setIsAddToPlaylistOpen: (open: boolean, track: Track | null = null) =>
    set({ isAddToPlaylistOpen: open, trackToAddToPlaylist: track }),
  setSelectedPlaylistIdForView: (id: string | null) => set({ selectedPlaylistIdForView: id }),
  setSearchQuery: (query: string) => set({ searchQuery: query }),
  setAudioError: (error: string | null) => set({ audioError: error }),

  setCurrentTime: (time: number) => set({ currentTime: time }),
  setDuration: (duration: number) => set({ duration }),
  setIsPlaying: (playing: boolean) => set({ isPlaying: playing }),

  clearLibrary: () => {
    try {
      localStorage.removeItem(LOCAL_STORAGE_LIKED_KEY);
      localStorage.removeItem(LOCAL_STORAGE_HISTORY_KEY);
      localStorage.removeItem(LOCAL_STORAGE_PLAYLISTS_KEY);
    } catch {
      // ignore
    }
    set({ likedTracks: [], history: [], playlists: [] });
  },
}));

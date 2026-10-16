export interface Track {
  id: string | number;
  title: string;
  artist: string;
  artistUrl?: string;
  artworkUrl: string;
  streamUrl: string;
  duration: number; // in seconds
  genre?: string;
  permalinkUrl?: string;
  playbackCount?: number;
  likesCount?: number;
  createdAt?: string;
  description?: string;
  lyrics?: string[];
  isStreamable: boolean;
  isDemo?: boolean;
  isImported?: boolean;
}

export interface Playlist {
  id: string;
  name: string;
  description?: string;
  coverUrl?: string;
  tracks: Track[];
  createdAt: string;
  updatedAt: string;
}

export type RepeatMode = 'off' | 'one' | 'all';
export type ActiveTab = 'home' | 'search' | 'library';

export interface PlayerStore {
  // Playback state
  currentTrack: Track | null;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  isMuted: boolean;
  repeatMode: RepeatMode;
  shuffle: boolean;
  playbackRate: number;
  sleepTimerMinutes: number | null;

  // Queue
  queue: Track[];
  queueIndex: number;

  // Library & Playlists
  playlists: Playlist[];
  likedTracks: Track[];
  history: Track[];

  // UI state
  activeTab: ActiveTab;
  isFullscreenOpen: boolean;
  isSettingsOpen: boolean;
  isQueueOpen: boolean;
  isAddToPlaylistOpen: boolean;
  trackToAddToPlaylist: Track | null;
  selectedPlaylistIdForView: string | null;
  searchQuery: string;
  audioError: string | null;

  // Actions
  playTrack: (track: Track, newQueue?: Track[]) => void;
  togglePlayPause: () => void;
  pause: () => void;
  resume: () => void;
  seek: (time: number) => void;
  setVolume: (volume: number) => void;
  toggleMute: () => void;
  nextTrack: () => void;
  previousTrack: () => void;
  setRepeatMode: (mode: RepeatMode) => void;
  toggleShuffle: () => void;
  setPlaybackRate: (rate: number) => void;
  setSleepTimer: (minutes: number | null) => void;

  // Queue Management
  addToQueue: (track: Track) => void;
  removeFromQueue: (index: number) => void;
  clearQueue: () => void;

  // Artwork & Track Metadata Sync
  updateTrackArtwork: (trackId: string | number, newArtworkUrl: string) => void;

  // Playlists
  createPlaylist: (name: string, description?: string, coverUrl?: string, initialTracks?: Track[]) => Playlist;
  updatePlaylistDetails: (id: string, updates: { name?: string; description?: string; coverUrl?: string }) => void;
  renamePlaylist: (id: string, name: string, description?: string) => void;
  deletePlaylist: (id: string) => void;
  addTrackToPlaylist: (playlistId: string, track: Track) => void;
  removeTrackFromPlaylist: (playlistId: string, trackId: string | number) => void;
  reorderPlaylistTracks: (playlistId: string, fromIndex: number, toIndex: number) => void;

  toggleLikeTrack: (track: Track) => void;
  isLiked: (trackId: string | number) => boolean;

  setActiveTab: (tab: ActiveTab) => void;
  setIsFullscreenOpen: (open: boolean) => void;
  setIsSettingsOpen: (open: boolean) => void;
  setIsQueueOpen: (open: boolean) => void;
  setIsAddToPlaylistOpen: (open: boolean, track?: Track | null) => void;
  setSelectedPlaylistIdForView: (id: string | null) => void;
  setSearchQuery: (query: string) => void;
  setAudioError: (error: string | null) => void;

  // Internal sync from audio engine
  setCurrentTime: (time: number) => void;
  setDuration: (duration: number) => void;
  setIsPlaying: (playing: boolean) => void;
  clearLibrary: () => void;
}

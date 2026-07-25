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
  isStreamable: boolean;
  isDemo?: boolean;
  isUnmatched?: boolean; // Flagged for imported tracks without exact SC match
  spotifyOriginalMeta?: {
    spotifyId: string;
    originalTitle: string;
    originalArtist: string;
  };
}

export interface Playlist {
  id: string;
  name: string;
  description?: string;
  coverUrl?: string;
  tracks: Track[];
  createdAt: string;
  updatedAt: string;
  isSpotifyImported?: boolean;
}

export interface UserProfile {
  displayName: string;
  avatarUrl: string;
  bio?: string;
  updatedAt?: string;
}

export type RepeatMode = 'off' | 'one' | 'all';
export type ApiMode = 'demo' | 'live';
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

  // Queue
  queue: Track[];
  queueIndex: number;

  // Library & Playlists & User Profile
  userProfile: UserProfile;
  playlists: Playlist[];
  likedTracks: Track[];
  history: Track[];

  // Client ID & Configuration
  clientId: string;
  apiMode: ApiMode;
  hasSeenClientIdModal: boolean;

  // UI state
  activeTab: ActiveTab;
  isFullscreenOpen: boolean;
  isSettingsOpen: boolean;
  isProfileOpen: boolean;
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

  // User Profile
  updateUserProfile: (profile: Partial<UserProfile>) => Promise<void>;
  loadUserProfile: () => Promise<void>;

  // Playlists
  createPlaylist: (name: string, description?: string, coverUrl?: string) => Playlist;
  renamePlaylist: (id: string, name: string, description?: string) => void;
  deletePlaylist: (id: string) => void;
  addTrackToPlaylist: (playlistId: string, track: Track) => void;
  removeTrackFromPlaylist: (playlistId: string, trackId: string | number) => void;
  reorderPlaylistTracks: (playlistId: string, fromIndex: number, toIndex: number) => void;

  toggleLikeTrack: (track: Track) => void;
  isLiked: (trackId: string | number) => boolean;

  setClientId: (id: string) => void;
  setApiMode: (mode: ApiMode) => void;
  setHasSeenClientIdModal: (seen: boolean) => void;

  setActiveTab: (tab: ActiveTab) => void;
  setIsFullscreenOpen: (open: boolean) => void;
  setIsSettingsOpen: (open: boolean) => void;
  setIsProfileOpen: (open: boolean) => void;
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

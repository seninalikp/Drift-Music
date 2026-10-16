import React from 'react';
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Repeat1,
  Volume2,
  VolumeX,
  Heart,
  ListMusic,
} from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { formatTime } from './TrackCard';
import { ArtworkImage } from './ArtworkImage';

export const MiniPlayer: React.FC = () => {
  const currentTrack = usePlayerStore((s) => s.currentTrack);
  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const currentTime = usePlayerStore((s) => s.currentTime);
  const duration = usePlayerStore((s) => s.duration);
  const volume = usePlayerStore((s) => s.volume);
  const isMuted = usePlayerStore((s) => s.isMuted);
  const repeatMode = usePlayerStore((s) => s.repeatMode);
  const shuffle = usePlayerStore((s) => s.shuffle);

  const togglePlayPause = usePlayerStore((s) => s.togglePlayPause);
  const nextTrack = usePlayerStore((s) => s.nextTrack);
  const previousTrack = usePlayerStore((s) => s.previousTrack);
  const seek = usePlayerStore((s) => s.seek);
  const setVolume = usePlayerStore((s) => s.setVolume);
  const toggleMute = usePlayerStore((s) => s.toggleMute);
  const setRepeatMode = usePlayerStore((s) => s.setRepeatMode);
  const toggleShuffle = usePlayerStore((s) => s.toggleShuffle);
  const toggleLikeTrack = usePlayerStore((s) => s.toggleLikeTrack);
  const isLiked = currentTrack ? usePlayerStore((s) => s.isLiked(currentTrack.id)) : false;

  const setIsFullscreenOpen = usePlayerStore((s) => s.setIsFullscreenOpen);
  const setIsQueueOpen = usePlayerStore((s) => s.setIsQueueOpen);
  const isQueueOpen = usePlayerStore((s) => s.isQueueOpen);

  if (!currentTrack) return null;

  const progressPercent = duration > 0 ? Math.min(100, (currentTime / duration) * 100) : 0;

  const handleSeekChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    seek(parseFloat(e.target.value));
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setVolume(parseFloat(e.target.value));
  };

  const cycleRepeat = () => {
    if (repeatMode === 'off') setRepeatMode('all');
    else if (repeatMode === 'all') setRepeatMode('one');
    else setRepeatMode('off');
  };

  return (
    <div className="fixed bottom-[68px] md:bottom-4 left-2.5 right-2.5 md:left-6 md:right-6 z-40 max-w-6xl mx-auto select-none pointer-events-none">
      {/* Apple Music × Liquid Glass Floating Pill Bar */}
      <div
        onClick={() => setIsFullscreenOpen(true)}
        className="pointer-events-auto relative overflow-hidden rounded-2xl md:rounded-[22px] liquid-glass-pill px-3 py-2 md:px-5 md:py-2.5 cursor-pointer transition-all duration-300 hover:border-white/25 group"
      >
        {/* Subtle Dynamic Ambient Tint from Current Artwork */}
        <div className="absolute -left-8 -top-8 w-36 h-36 rounded-full opacity-25 blur-2xl pointer-events-none overflow-hidden">
          <ArtworkImage
            src={currentTrack.artworkUrl}
            title={currentTrack.title}
            artist={currentTrack.artist}
            trackId={currentTrack.id}
            className="w-full h-full object-cover"
          />
        </div>

        <div className="relative z-10 flex items-center justify-between gap-3 md:gap-6">
          {/* Left: Apple Music Floating Album Art & Track Title */}
          <div className="flex items-center gap-3 min-w-0 flex-1 md:max-w-[320px]">
            <div
              className={`relative w-11 h-11 md:w-12 md:h-12 rounded-xl overflow-hidden shrink-0 shadow-lg border border-white/15 transition-transform duration-500 ${
                isPlaying ? 'scale-100' : 'scale-92 opacity-90'
              }`}
            >
              <ArtworkImage
                src={currentTrack.artworkUrl}
                title={currentTrack.title}
                artist={currentTrack.artist}
                trackId={currentTrack.id}
                className="w-full h-full object-cover"
              />
            </div>

            <div className="flex flex-col min-w-0 flex-1">
              <span className="text-[13px] md:text-sm font-bold text-white truncate tracking-tight">
                {currentTrack.title}
              </span>
              <span className="text-[11px] md:text-xs text-white/60 truncate font-medium">
                {currentTrack.artist}
              </span>
            </div>

            <button
              onClick={(e) => {
                e.stopPropagation();
                toggleLikeTrack(currentTrack);
              }}
              className="p-2 rounded-full text-white/60 hover:text-white hover:bg-white/10 transition-all shrink-0"
              title={isLiked ? 'Remove from Liked' : 'Save to Liked'}
              id="mini-like-btn"
            >
              <Heart
                className={`w-4 h-4 transition-transform active:scale-125 ${
                  isLiked ? 'fill-[#FA2D48] text-[#FA2D48]' : ''
                }`}
              />
            </button>
          </div>

          {/* Center: Desktop Spotify/Apple Hybrid Transport + Time Scrubber */}
          <div
            onClick={(e) => e.stopPropagation()}
            className="flex flex-col items-center gap-1 flex-1 max-w-md"
          >
            <div className="flex items-center gap-1.5 sm:gap-3">
              <button
                onClick={toggleShuffle}
                className={`hidden md:flex p-1.5 rounded-full transition-colors ${
                  shuffle ? 'text-[#1DB954]' : 'text-white/50 hover:text-white'
                }`}
                title="Shuffle"
                id="mini-shuffle-btn"
              >
                <Shuffle className="w-3.5 h-3.5" />
              </button>

              <button
                onClick={previousTrack}
                className="hidden sm:flex p-1.5 text-white/80 hover:text-white transition-transform active:scale-90"
                title="Previous"
                id="mini-prev-btn"
              >
                <SkipBack className="w-4 h-4 fill-current" />
              </button>

              <button
                onClick={togglePlayPause}
                className="w-9 h-9 md:w-10 md:h-10 rounded-full bg-white text-black flex items-center justify-center shadow-md hover:scale-105 active:scale-95 transition-all"
                title={isPlaying ? 'Pause' : 'Play'}
                id="mini-play-btn"
              >
                {isPlaying ? (
                  <Pause className="w-4 h-4 md:w-5 md:h-5 fill-black" />
                ) : (
                  <Play className="w-4 h-4 md:w-5 md:h-5 fill-black ml-0.5" />
                )}
              </button>

              <button
                onClick={nextTrack}
                className="p-1.5 text-white/85 hover:text-white transition-transform active:scale-90"
                title="Next"
                id="mini-next-btn"
              >
                <SkipForward className="w-5 h-5 md:w-4 md:h-4 fill-current" />
              </button>

              <button
                onClick={cycleRepeat}
                className={`hidden md:flex p-1.5 rounded-full transition-colors ${
                  repeatMode !== 'off' ? 'text-[#1DB954]' : 'text-white/50 hover:text-white'
                }`}
                title={`Repeat: ${repeatMode}`}
                id="mini-repeat-btn"
              >
                {repeatMode === 'one' ? (
                  <Repeat1 className="w-3.5 h-3.5" />
                ) : (
                  <Repeat className="w-3.5 h-3.5" />
                )}
              </button>
            </div>

            {/* Desktop Scrubber */}
            <div className="hidden md:flex items-center gap-2 w-full">
              <span className="text-[10px] font-mono text-white/50 w-7 text-right">
                {formatTime(currentTime)}
              </span>
              <div className="relative flex-1 flex items-center h-2 cursor-pointer group/seek">
                <input
                  type="range"
                  min={0}
                  max={duration || 100}
                  step={0.5}
                  value={currentTime}
                  onChange={handleSeekChange}
                  className="w-full h-1 bg-white/15 group-hover/seek:h-1.5 rounded-full appearance-none cursor-pointer accent-white focus:outline-none transition-all"
                  id="mini-player-seek"
                />
                <div
                  className="absolute left-0 top-1/2 -translate-y-1/2 h-1 group-hover/seek:h-1.5 bg-gradient-to-r from-[#1DB954] to-emerald-300 rounded-full pointer-events-none transition-all"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>
              <span className="text-[10px] font-mono text-white/50 w-7">
                {formatTime(duration)}
              </span>
            </div>
          </div>

          {/* Right: Queue & Volume */}
          <div
            onClick={(e) => e.stopPropagation()}
            className="flex items-center gap-1.5 md:gap-3 shrink-0"
          >
            <button
              onClick={() => setIsQueueOpen(!isQueueOpen)}
              className={`p-2 rounded-full transition-all ${
                isQueueOpen
                  ? 'bg-[#1DB954]/20 text-[#1DB954] border border-[#1DB954]/40'
                  : 'text-white/65 hover:text-white hover:bg-white/10'
              }`}
              title="Up Next Queue"
              id="mini-queue-btn"
            >
              <ListMusic className="w-4 h-4" />
            </button>

            <div className="hidden lg:flex items-center gap-2">
              <button
                onClick={toggleMute}
                className="p-1.5 text-white/65 hover:text-white transition-colors"
                title={isMuted ? 'Unmute' : 'Mute'}
                id="mini-mute-btn"
              >
                {isMuted || volume === 0 ? (
                  <VolumeX className="w-4 h-4 text-rose-400" />
                ) : (
                  <Volume2 className="w-4 h-4" />
                )}
              </button>
              <input
                type="range"
                min={0}
                max={1}
                step={0.01}
                value={isMuted ? 0 : volume}
                onChange={handleVolumeChange}
                className="w-20 h-1 bg-white/20 rounded-full appearance-none cursor-pointer accent-white focus:outline-none"
                id="mini-volume-slider"
              />
            </div>
          </div>
        </div>

        {/* Apple Music Bottom Hairline Progress Indicator on Mobile */}
        <div className="md:hidden absolute bottom-0 left-0 right-0 h-[2.5px] bg-white/10 overflow-hidden">
          <div
            className="h-full bg-gradient-to-r from-[#1DB954] via-emerald-400 to-white transition-all duration-150"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
      </div>
    </div>
  );
};

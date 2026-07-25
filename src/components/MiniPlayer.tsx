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
  Maximize2,
  ListMusic,
  Radio,
  Sparkles,
} from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { formatTime } from './TrackCard';

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

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  const handleSeekChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value);
    seek(val);
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = parseFloat(e.target.value);
    setVolume(val);
  };

  const cycleRepeat = () => {
    if (repeatMode === 'off') setRepeatMode('all');
    else if (repeatMode === 'all') setRepeatMode('one');
    else setRepeatMode('off');
  };

  return (
    <div className="fixed bottom-14 md:bottom-0 left-0 right-0 z-40 bg-[#181818]/98 backdrop-blur-xl border-t border-[#282828] px-3 py-2 md:px-5 md:py-3 shadow-2xl select-none">
      {/* Top Seek Progress Line */}
      <div className="group relative w-full mb-1 flex items-center gap-2">
        <span className="hidden md:inline text-[10px] font-mono text-gray-400 w-8 text-right">
          {formatTime(currentTime)}
        </span>
        <div className="relative flex-1 flex items-center h-2 cursor-pointer">
          <input
            type="range"
            min={0}
            max={duration || 100}
            step={0.5}
            value={currentTime}
            onChange={handleSeekChange}
            className="w-full h-1 bg-[#383838] group-hover:h-1.5 rounded-lg appearance-none cursor-pointer accent-[#1DB954] focus:outline-none transition-all"
            aria-label="Seek time"
            id="mini-player-seek"
          />
          <div
            className="absolute left-0 top-1/2 -translate-y-1/2 h-1 group-hover:h-1.5 bg-[#1DB954] rounded-lg pointer-events-none transition-all"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
        <span className="hidden md:inline text-[10px] font-mono text-gray-400 w-8">
          {formatTime(duration)}
        </span>
      </div>

      <div className="flex items-center justify-between gap-2 md:gap-4">
        {/* Left: Track Info & Artwork */}
        <div className="flex items-center gap-3 min-w-0 max-w-[30%] md:max-w-[280px]">
          <div
            onClick={() => setIsFullscreenOpen(true)}
            className="relative w-11 h-11 md:w-12 md:h-12 rounded-lg overflow-hidden shrink-0 cursor-pointer group shadow-md"
          >
            <img
              src={currentTrack.artworkUrl}
              alt={currentTrack.title}
              className="w-full h-full object-cover group-hover:scale-105 transition-transform"
            />
            <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <Maximize2 className="w-5 h-5 text-white" />
            </div>
          </div>

          <div
            onClick={() => setIsFullscreenOpen(true)}
            className="flex flex-col min-w-0 cursor-pointer"
          >
            <div className="flex items-center gap-1.5 min-w-0">
              <span className="text-xs md:text-sm font-bold text-white truncate hover:underline">
                {currentTrack.title}
              </span>
              {currentTrack.isDemo ? (
                <Sparkles className="w-3 h-3 text-emerald-400 shrink-0" title="Demo Track" />
              ) : (
                <Radio className="w-3 h-3 text-orange-400 shrink-0" title="SoundCloud Track" />
              )}
            </div>
            <span className="text-[11px] md:text-xs text-gray-400 truncate hover:underline">
              {currentTrack.artist}
            </span>
          </div>

          <button
            onClick={() => toggleLikeTrack(currentTrack)}
            className="hidden sm:block p-1.5 text-gray-400 hover:text-white transition-colors shrink-0"
            title={isLiked ? 'Remove from Liked' : 'Save to Liked'}
            aria-label="Like track"
            id="mini-like-btn"
          >
            <Heart className={`w-4 h-4 ${isLiked ? 'fill-purple-500 text-purple-500' : ''}`} />
          </button>
        </div>

        {/* Center: Main Transport Controls */}
        <div className="flex items-center gap-2 md:gap-4 shrink-0">
          <button
            onClick={toggleShuffle}
            className={`hidden sm:block p-1.5 rounded-full transition-colors ${
              shuffle ? 'text-[#1DB954]' : 'text-gray-400 hover:text-white'
            }`}
            title="Shuffle"
            aria-label="Toggle Shuffle"
            id="mini-shuffle-btn"
          >
            <Shuffle className="w-4 h-4" />
          </button>

          <button
            onClick={previousTrack}
            className="p-1.5 text-gray-300 hover:text-white transition-colors"
            title="Previous Track"
            aria-label="Previous Track"
            id="mini-prev-btn"
          >
            <SkipBack className="w-5 h-5 fill-current" />
          </button>

          <button
            onClick={togglePlayPause}
            className="w-10 h-10 rounded-full bg-white hover:bg-gray-200 text-black flex items-center justify-center transition-all hover:scale-105 shadow-lg"
            title={isPlaying ? 'Pause' : 'Play'}
            aria-label={isPlaying ? 'Pause' : 'Play'}
            id="mini-play-btn"
          >
            {isPlaying ? (
              <Pause className="w-5 h-5 fill-black" />
            ) : (
              <Play className="w-5 h-5 fill-black ml-0.5" />
            )}
          </button>

          <button
            onClick={nextTrack}
            className="p-1.5 text-gray-300 hover:text-white transition-colors"
            title="Next Track"
            aria-label="Next Track"
            id="mini-next-btn"
          >
            <SkipForward className="w-5 h-5 fill-current" />
          </button>

          <button
            onClick={cycleRepeat}
            className={`hidden sm:block p-1.5 rounded-full transition-colors ${
              repeatMode !== 'off' ? 'text-[#1DB954]' : 'text-gray-400 hover:text-white'
            }`}
            title={`Repeat: ${repeatMode}`}
            aria-label="Cycle Repeat Mode"
            id="mini-repeat-btn"
          >
            {repeatMode === 'one' ? <Repeat1 className="w-4 h-4" /> : <Repeat className="w-4 h-4" />}
          </button>
        </div>

        {/* Right: Queue & Volume & Fullscreen Expand */}
        <div className="flex items-center gap-2 md:gap-3 shrink-0">
          <button
            onClick={() => setIsQueueOpen(!isQueueOpen)}
            className={`p-1.5 rounded-full transition-colors ${
              isQueueOpen ? 'text-[#1DB954] bg-[#282828]' : 'text-gray-400 hover:text-white'
            }`}
            title="Play Queue"
            aria-label="Open Queue"
            id="mini-queue-btn"
          >
            <ListMusic className="w-4 h-4" />
          </button>

          {/* Volume Slider */}
          <div className="hidden lg:flex items-center gap-2">
            <button
              onClick={toggleMute}
              className="p-1 text-gray-400 hover:text-white transition-colors"
              title={isMuted ? 'Unmute' : 'Mute'}
              id="mini-mute-btn"
            >
              {isMuted || volume === 0 ? (
                <VolumeX className="w-4 h-4 text-red-400" />
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
              className="w-20 h-1 bg-[#383838] rounded-lg appearance-none cursor-pointer accent-[#1DB954] focus:outline-none"
              aria-label="Volume slider"
              id="mini-volume-slider"
            />
          </div>

          <button
            onClick={() => setIsFullscreenOpen(true)}
            className="p-1.5 text-gray-400 hover:text-white transition-colors"
            title="Fullscreen Player"
            aria-label="Expand Player"
            id="mini-expand-btn"
          >
            <Maximize2 className="w-4 h-4" />
          </button>
        </div>
      </div>
    </div>
  );
};

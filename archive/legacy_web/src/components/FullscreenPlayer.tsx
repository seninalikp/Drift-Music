import React, { useEffect } from 'react';
import {
  ChevronDown,
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
  ExternalLink,
  Radio,
  Sparkles,
  Keyboard,
} from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { formatTime } from './TrackCard';

export const FullscreenPlayer: React.FC = () => {
  const isFullscreenOpen = usePlayerStore((s) => s.isFullscreenOpen);
  const setIsFullscreenOpen = usePlayerStore((s) => s.setIsFullscreenOpen);

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

  const isQueueOpen = usePlayerStore((s) => s.isQueueOpen);
  const setIsQueueOpen = usePlayerStore((s) => s.setIsQueueOpen);

  // Close on Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isFullscreenOpen) {
        setIsFullscreenOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isFullscreenOpen]);

  if (!isFullscreenOpen || !currentTrack) return null;

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  const handleSeekChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    seek(parseFloat(e.target.value));
  };

  const cycleRepeat = () => {
    if (repeatMode === 'off') setRepeatMode('all');
    else if (repeatMode === 'all') setRepeatMode('one');
    else setRepeatMode('off');
  };

  return (
    <div className="fixed inset-0 z-50 bg-[#090909]/95 backdrop-blur-2xl flex flex-col justify-between p-6 md:p-10 select-none overflow-y-auto animate-fadeIn">
      {/* Dynamic Background Glow */}
      <div className="absolute inset-0 pointer-events-none opacity-30 bg-gradient-to-tr from-[#1DB954]/30 via-purple-900/30 to-[#FF5500]/20 blur-3xl" />

      {/* Top Header Row */}
      <div className="relative z-10 flex items-center justify-between w-full max-w-4xl mx-auto">
        <button
          onClick={() => setIsFullscreenOpen(false)}
          className="p-2 rounded-full bg-[#181818] hover:bg-[#282828] text-gray-300 hover:text-white transition-colors"
          title="Minimize Player (Esc)"
          aria-label="Minimize Player"
          id="fullscreen-minimize-btn"
        >
          <ChevronDown className="w-6 h-6" />
        </button>

        <div className="flex flex-col items-center">
          <span className="text-[10px] uppercase tracking-widest font-bold text-gray-400">
            Playing From Playlist
          </span>
          <span className="text-xs font-bold text-white flex items-center gap-1.5 mt-0.5">
            {currentTrack.isDemo ? (
              <>
                <Sparkles className="w-3.5 h-3.5 text-emerald-400" /> Demo Recommendations
              </>
            ) : (
              <>
                <Radio className="w-3.5 h-3.5 text-orange-400" /> SoundCloud Live Stream
              </>
            )}
          </span>
        </div>

        <button
          onClick={() => setIsQueueOpen(!isQueueOpen)}
          className={`p-2 rounded-full transition-colors ${
            isQueueOpen ? 'bg-[#1DB954] text-black' : 'bg-[#181818] hover:bg-[#282828] text-gray-300'
          }`}
          title="View Play Queue"
          aria-label="Open Queue"
          id="fullscreen-queue-toggle-btn"
        >
          <ListMusic className="w-5 h-5" />
        </button>
      </div>

      {/* Middle Content Row: Artwork & Track Metadata */}
      <div className="relative z-10 my-auto flex flex-col items-center justify-center w-full max-w-lg mx-auto gap-6 py-6">
        {/* Large Artwork */}
        <div className="relative aspect-square w-64 h-64 sm:w-80 sm:h-80 rounded-2xl overflow-hidden shadow-2xl border border-white/10 group">
          <img
            src={currentTrack.artworkUrl}
            alt={currentTrack.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
          />
        </div>

        {/* Track Title & Artist */}
        <div className="flex items-center justify-between w-full">
          <div className="flex flex-col min-w-0 pr-4">
            <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight truncate">
              {currentTrack.title}
            </h1>
            <p className="text-sm sm:text-base font-semibold text-gray-400 truncate mt-1">
              {currentTrack.artist}
            </p>
            {currentTrack.genre && (
              <span className="mt-2 text-[11px] font-bold text-[#1DB954] bg-[#1DB954]/10 border border-[#1DB954]/30 px-2.5 py-0.5 rounded-full w-fit">
                {currentTrack.genre}
              </span>
            )}
          </div>

          <div className="flex items-center gap-2">
            {currentTrack.permalinkUrl && (
              <a
                href={currentTrack.permalinkUrl}
                target="_blank"
                rel="noreferrer"
                className="p-2.5 rounded-full bg-[#181818] hover:bg-[#282828] text-gray-300 hover:text-white transition-colors"
                title="Open on SoundCloud"
              >
                <ExternalLink className="w-5 h-5" />
              </a>
            )}

            <button
              onClick={() => toggleLikeTrack(currentTrack)}
              className={`p-2.5 rounded-full bg-[#181818] hover:bg-[#282828] transition-colors ${
                isLiked ? 'text-purple-400' : 'text-gray-400 hover:text-white'
              }`}
              title={isLiked ? 'Liked' : 'Like'}
              aria-label="Like Track"
              id="fullscreen-like-btn"
            >
              <Heart className={`w-6 h-6 ${isLiked ? 'fill-purple-500 text-purple-500' : ''}`} />
            </button>
          </div>
        </div>

        {/* Large Seek Bar */}
        <div className="w-full flex flex-col gap-2">
          <div className="relative flex items-center w-full h-3 cursor-pointer">
            <input
              type="range"
              min={0}
              max={duration || 100}
              step={0.5}
              value={currentTime}
              onChange={handleSeekChange}
              className="w-full h-1.5 bg-[#383838] hover:h-2 rounded-lg appearance-none cursor-pointer accent-[#1DB954] focus:outline-none transition-all"
              id="fullscreen-seek-slider"
            />
            <div
              className="absolute left-0 top-1/2 -translate-y-1/2 h-1.5 bg-[#1DB954] rounded-lg pointer-events-none"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          <div className="flex items-center justify-between text-xs font-mono text-gray-400">
            <span>{formatTime(currentTime)}</span>
            <span>{formatTime(duration)}</span>
          </div>
        </div>

        {/* Transport Action Bar */}
        <div className="flex items-center justify-between w-full max-w-sm px-2">
          <button
            onClick={toggleShuffle}
            className={`p-2 rounded-full transition-colors ${
              shuffle ? 'text-[#1DB954] bg-[#1DB954]/10' : 'text-gray-400 hover:text-white'
            }`}
            title="Shuffle"
            aria-label="Toggle Shuffle"
            id="fullscreen-shuffle-btn"
          >
            <Shuffle className="w-5 h-5" />
          </button>

          <button
            onClick={previousTrack}
            className="p-3 text-gray-200 hover:text-white transition-transform hover:scale-110"
            title="Previous"
            aria-label="Previous Track"
            id="fullscreen-prev-btn"
          >
            <SkipBack className="w-7 h-7 fill-current" />
          </button>

          <button
            onClick={togglePlayPause}
            className="w-16 h-16 rounded-full bg-[#1DB954] hover:bg-[#1ed760] text-black flex items-center justify-center shadow-xl hover:scale-105 transition-all"
            title={isPlaying ? 'Pause' : 'Play'}
            aria-label={isPlaying ? 'Pause' : 'Play'}
            id="fullscreen-play-btn"
          >
            {isPlaying ? (
              <Pause className="w-8 h-8 fill-black" />
            ) : (
              <Play className="w-8 h-8 fill-black ml-1" />
            )}
          </button>

          <button
            onClick={nextTrack}
            className="p-3 text-gray-200 hover:text-white transition-transform hover:scale-110"
            title="Next"
            aria-label="Next Track"
            id="fullscreen-next-btn"
          >
            <SkipForward className="w-7 h-7 fill-current" />
          </button>

          <button
            onClick={cycleRepeat}
            className={`p-2 rounded-full transition-colors ${
              repeatMode !== 'off' ? 'text-[#1DB954] bg-[#1DB954]/10' : 'text-gray-400 hover:text-white'
            }`}
            title={`Repeat: ${repeatMode}`}
            aria-label="Cycle Repeat"
            id="fullscreen-repeat-btn"
          >
            {repeatMode === 'one' ? <Repeat1 className="w-5 h-5" /> : <Repeat className="w-5 h-5" />}
          </button>
        </div>

        {/* Volume Slider Row */}
        <div className="flex items-center gap-3 w-full max-w-xs pt-2">
          <button
            onClick={toggleMute}
            className="p-1 text-gray-400 hover:text-white"
            title={isMuted ? 'Unmute' : 'Mute'}
            id="fullscreen-mute-btn"
          >
            {isMuted || volume === 0 ? (
              <VolumeX className="w-5 h-5 text-red-400" />
            ) : (
              <Volume2 className="w-5 h-5" />
            )}
          </button>
          <input
            type="range"
            min={0}
            max={1}
            step={0.01}
            value={isMuted ? 0 : volume}
            onChange={(e) => setVolume(parseFloat(e.target.value))}
            className="w-full h-1.5 bg-[#383838] rounded-lg appearance-none cursor-pointer accent-[#1DB954] focus:outline-none"
            aria-label="Volume slider"
            id="fullscreen-volume-slider"
          />
        </div>
      </div>

      {/* Keyboard Shortcuts Footer Note */}
      <div className="relative z-10 w-full max-w-xl mx-auto flex items-center justify-center gap-4 text-xs text-gray-400 bg-[#121212]/80 border border-[#282828] py-2 px-4 rounded-full">
        <Keyboard className="w-4 h-4 text-[#1DB954]" />
        <span>
          <strong className="text-gray-300">Space:</strong> Play/Pause &nbsp;|&nbsp;{' '}
          <strong className="text-gray-300">← →:</strong> Seek 5s &nbsp;|&nbsp;{' '}
          <strong className="text-gray-300">Esc:</strong> Close
        </span>
      </div>
    </div>
  );
};

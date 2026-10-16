import React, { useEffect, useState } from 'react';
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
  Quote,
  Moon,
  Gauge,
  PlusCircle,
} from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { formatTime } from './TrackCard';
import { ArtworkImage } from './ArtworkImage';

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
  const playbackRate = usePlayerStore((s) => s.playbackRate);
  const sleepTimerMinutes = usePlayerStore((s) => s.sleepTimerMinutes);

  const togglePlayPause = usePlayerStore((s) => s.togglePlayPause);
  const nextTrack = usePlayerStore((s) => s.nextTrack);
  const previousTrack = usePlayerStore((s) => s.previousTrack);
  const seek = usePlayerStore((s) => s.seek);
  const setVolume = usePlayerStore((s) => s.setVolume);
  const toggleMute = usePlayerStore((s) => s.toggleMute);
  const setRepeatMode = usePlayerStore((s) => s.setRepeatMode);
  const toggleShuffle = usePlayerStore((s) => s.toggleShuffle);
  const toggleLikeTrack = usePlayerStore((s) => s.toggleLikeTrack);
  const setPlaybackRate = usePlayerStore((s) => s.setPlaybackRate);
  const setSleepTimer = usePlayerStore((s) => s.setSleepTimer);
  const setIsAddToPlaylistOpen = usePlayerStore((s) => s.setIsAddToPlaylistOpen);
  const isLiked = currentTrack ? usePlayerStore((s) => s.isLiked(currentTrack.id)) : false;

  const queue = usePlayerStore((s) => s.queue);
  const playTrack = usePlayerStore((s) => s.playTrack);

  // Apple Music Bottom Mode Tabs: 'art' | 'lyrics' | 'queue'
  const [activePanel, setActivePanel] = useState<'art' | 'lyrics' | 'queue'>('art');

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

  const progressPercent = duration > 0 ? Math.min(100, (currentTime / duration) * 100) : 0;
  const remainingTime = Math.max(0, duration - currentTime);

  const cycleRepeat = () => {
    if (repeatMode === 'off') setRepeatMode('all');
    else if (repeatMode === 'all') setRepeatMode('one');
    else setRepeatMode('off');
  };

  const cycleSpeed = () => {
    const speeds = [0.85, 1, 1.15, 1.25, 1.5];
    const nextIdx = (speeds.indexOf(playbackRate) + 1) % speeds.length;
    setPlaybackRate(speeds[nextIdx]);
  };

  const cycleSleepTimer = () => {
    const options: (number | null)[] = [null, 15, 30, 60];
    const nextIdx = (options.indexOf(sleepTimerMinutes) + 1) % options.length;
    setSleepTimer(options[nextIdx]);
  };

  const defaultLyrics = currentTrack.lyrics || [
    `♪ ${currentTrack.title} — ${currentTrack.artist}`,
    currentTrack.description || 'Feel the rhythm and immersive soundscape.',
    'Lost in the frequency, drifting through neon lights',
    'Every beat resonates deeper in the night',
    'Crystal clear acoustics, liquid glass horizons',
    ' Pure studio stream • Drift Audio Engine',
  ];

  return (
    <div className="fixed inset-0 z-50 bg-[#08080c] flex flex-col justify-between p-5 sm:p-8 md:p-10 select-none overflow-hidden animate-slideUpSheet">
      {/* sebjvidal Apple Music Dynamic Multi-Layer Blurred Artwork Mesh Background */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        <div className="absolute -inset-24 opacity-55 blur-[90px] saturate-200 animate-mesh">
          <ArtworkImage
            src={currentTrack.artworkUrl}
            title={currentTrack.title}
            artist={currentTrack.artist}
            trackId={currentTrack.id}
            className="w-full h-full object-cover"
          />
        </div>
        <div className="absolute inset-0 bg-gradient-to-b from-black/40 via-black/55 to-black/85" />
      </div>

      {/* Top Apple Music Sheet Grip Handle & Header */}
      <div className="relative z-10 flex flex-col items-center w-full max-w-2xl mx-auto gap-3">
        <div
          onClick={() => setIsFullscreenOpen(false)}
          className="w-11 h-1.5 rounded-full bg-white/30 hover:bg-white/60 cursor-pointer transition-colors"
          title="Swipe or click to minimize"
        />

        <div className="flex items-center justify-between w-full">
          <button
            onClick={() => setIsFullscreenOpen(false)}
            className="p-2.5 rounded-full liquid-button text-white/80 hover:text-white"
            title="Minimize (Esc)"
            id="fullscreen-minimize-btn"
          >
            <ChevronDown className="w-5 h-5" />
          </button>

          <div className="flex flex-col items-center">
            <span className="text-[10px] uppercase tracking-[0.2em] font-bold text-white/50">
              Playing From Library
            </span>
            <span className="text-xs font-semibold text-white/90 mt-0.5">
              {currentTrack.genre || 'Lossless Audio'}
            </span>
          </div>

          <button
            onClick={() => setIsAddToPlaylistOpen(true, currentTrack)}
            className="p-2.5 rounded-full liquid-button text-white/80 hover:text-white"
            title="Add to Playlist"
          >
            <PlusCircle className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Center Content Area: Switches smoothly between Artwork, Lyrics, and Up Next Queue */}
      <div className="relative z-10 my-auto flex flex-col items-center justify-center w-full max-w-lg mx-auto py-2 flex-1 min-h-0">
        {activePanel === 'art' && (
          <div className="flex flex-col items-center justify-center w-full h-full gap-6">
            {/* Apple Music Spring-Physics Scaling Artwork */}
            <div
              onClick={togglePlayPause}
              className={`relative aspect-square w-64 sm:w-80 md:w-[340px] rounded-[28px] overflow-hidden cursor-pointer transition-all duration-700 ease-[cubic-bezier(0.22,1,0.36,1)] border border-white/20 ${
                isPlaying
                  ? 'scale-100 shadow-[0_30px_80px_rgba(0,0,0,0.85)]'
                  : 'scale-[0.84] opacity-85 shadow-[0_12px_32px_rgba(0,0,0,0.5)]'
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
          </div>
        )}

        {activePanel === 'lyrics' && (
          <div className="w-full h-72 sm:h-80 overflow-y-auto custom-scrollbar liquid-glass rounded-3xl p-6 flex flex-col gap-5 text-left animate-fadeIn">
            <span className="text-[11px] font-bold uppercase tracking-widest text-white/50">
              Synced Audio Notes & Lyrics
            </span>
            {defaultLyrics.map((line, i) => {
              const activeLineIdx =
                Math.floor((currentTime / (duration || 180)) * defaultLyrics.length) %
                defaultLyrics.length;
              const isCurrentLine = i === activeLineIdx;
              return (
                <p
                  key={i}
                  onClick={() => seek((i / defaultLyrics.length) * (duration || 180))}
                  className={`text-lg sm:text-2xl font-extrabold tracking-tight cursor-pointer transition-all duration-500 ${
                    isCurrentLine
                      ? 'text-white scale-[1.02] origin-left drop-shadow-[0_2px_12px_rgba(255,255,255,0.35)]'
                      : 'text-white/35 hover:text-white/60 blur-[0.3px]'
                  }`}
                >
                  {line}
                </p>
              );
            })}
          </div>
        )}

        {activePanel === 'queue' && (
          <div className="w-full h-72 sm:h-80 overflow-y-auto custom-scrollbar liquid-glass rounded-3xl p-4 flex flex-col gap-2 animate-fadeIn">
            <div className="flex items-center justify-between px-2 pb-2 border-b border-white/10">
              <span className="text-xs font-bold uppercase tracking-wider text-white/70">
                Up Next ({queue.length})
              </span>
              <span className="text-[11px] text-white/40">Tap any track to play</span>
            </div>
            {queue.map((trk, idx) => {
              const isCurr = trk.id === currentTrack.id;
              return (
                <div
                  key={`fs-q-${trk.id}-${idx}`}
                  onClick={() => playTrack(trk, queue)}
                  className={`flex items-center gap-3 p-2.5 rounded-2xl cursor-pointer transition-all ${
                    isCurr
                      ? 'bg-white/15 border border-white/25'
                      : 'hover:bg-white/10'
                  }`}
                >
                  <div className="w-10 h-10 rounded-xl overflow-hidden shrink-0">
                    <ArtworkImage
                      src={trk.artworkUrl}
                      title={trk.title}
                      artist={trk.artist}
                      trackId={trk.id}
                      className="w-full h-full object-cover"
                    />
                  </div>
                  <div className="flex flex-col min-w-0 flex-1">
                    <span className={`text-xs font-bold truncate ${isCurr ? 'text-[#1DB954]' : 'text-white'}`}>
                      {trk.title}
                    </span>
                    <span className="text-[11px] text-white/60 truncate">{trk.artist}</span>
                  </div>
                  <span className="text-[11px] font-mono text-white/50">
                    {formatTime(trk.duration)}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Bottom Apple Music Controls Stack */}
      <div className="relative z-10 w-full max-w-lg mx-auto flex flex-col gap-4">
        {/* Title, Artist & Favorite Button */}
        <div className="flex items-center justify-between gap-4">
          <div className="flex flex-col min-w-0 flex-1">
            <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight truncate">
              {currentTrack.title}
            </h1>
            <p className="text-sm sm:text-base font-semibold text-white/65 truncate">
              {currentTrack.artist}
            </p>
          </div>

          <button
            onClick={() => toggleLikeTrack(currentTrack)}
            className="p-2.5 rounded-full liquid-button transition-transform active:scale-125"
            title={isLiked ? 'Liked' : 'Like'}
            id="fullscreen-like-btn"
          >
            <Heart
              className={`w-5 h-5 ${
                isLiked ? 'fill-[#FA2D48] text-[#FA2D48]' : 'text-white/80'
              }`}
            />
          </button>
        </div>

        {/* Liquid Glass Progress Bar with Remaining Time */}
        <div className="w-full flex flex-col gap-1.5">
          <div className="relative flex items-center w-full h-3 cursor-pointer group">
            <input
              type="range"
              min={0}
              max={duration || 100}
              step={0.5}
              value={currentTime}
              onChange={(e) => seek(parseFloat(e.target.value))}
              className="w-full h-1.5 group-hover:h-2.5 bg-white/20 rounded-full appearance-none cursor-pointer accent-white focus:outline-none transition-all"
              id="fullscreen-seek-slider"
            />
            <div
              className="absolute left-0 top-1/2 -translate-y-1/2 h-1.5 group-hover:h-2.5 bg-white rounded-full pointer-events-none transition-all shadow-[0_0_12px_rgba(255,255,255,0.8)]"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          <div className="flex items-center justify-between text-[11px] font-mono text-white/55">
            <span>{formatTime(currentTime)}</span>
            <span>-{formatTime(remainingTime)}</span>
          </div>
        </div>

        {/* Main Transport Controls */}
        <div className="flex items-center justify-between w-full px-2">
          <button
            onClick={toggleShuffle}
            className={`p-2.5 rounded-full transition-all ${
              shuffle ? 'bg-white/20 text-white' : 'text-white/50 hover:text-white'
            }`}
            title="Shuffle"
            id="fullscreen-shuffle-btn"
          >
            <Shuffle className="w-5 h-5" />
          </button>

          <button
            onClick={previousTrack}
            className="p-3 text-white hover:scale-110 active:scale-90 transition-transform"
            title="Previous"
            id="fullscreen-prev-btn"
          >
            <SkipBack className="w-8 h-8 fill-current" />
          </button>

          <button
            onClick={togglePlayPause}
            className="w-16 h-16 rounded-full bg-white text-black flex items-center justify-center shadow-[0_0_35px_rgba(255,255,255,0.4)] hover:scale-105 active:scale-95 transition-all"
            title={isPlaying ? 'Pause' : 'Play'}
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
            className="p-3 text-white hover:scale-110 active:scale-90 transition-transform"
            title="Next"
            id="fullscreen-next-btn"
          >
            <SkipForward className="w-8 h-8 fill-current" />
          </button>

          <button
            onClick={cycleRepeat}
            className={`p-2.5 rounded-full transition-all ${
              repeatMode !== 'off' ? 'bg-white/20 text-white' : 'text-white/50 hover:text-white'
            }`}
            title={`Repeat: ${repeatMode}`}
            id="fullscreen-repeat-btn"
          >
            {repeatMode === 'one' ? (
              <Repeat1 className="w-5 h-5" />
            ) : (
              <Repeat className="w-5 h-5" />
            )}
          </button>
        </div>

        {/* Apple Music Style Volume Slider */}
        <div className="flex items-center gap-3 w-full px-1">
          <button
            onClick={toggleMute}
            className="text-white/50 hover:text-white transition-colors"
            id="fullscreen-mute-btn"
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
            onChange={(e) => setVolume(parseFloat(e.target.value))}
            className="w-full h-1.5 bg-white/20 rounded-full appearance-none cursor-pointer accent-white focus:outline-none"
            id="fullscreen-volume-slider"
          />
        </div>

        {/* Apple Music Bottom Pill Toolbar: Lyrics, Speed (Slowed/Sped-up), Sleep Timer, Queue */}
        <div className="flex items-center justify-around pt-1">
          <button
            onClick={() => setActivePanel(activePanel === 'lyrics' ? 'art' : 'lyrics')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all ${
              activePanel === 'lyrics'
                ? 'bg-white text-black shadow-lg'
                : 'liquid-button text-white/75 hover:text-white'
            }`}
          >
            <Quote className="w-3.5 h-3.5" />
            <span>Lyrics</span>
          </button>

          <button
            onClick={cycleSpeed}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all ${
              playbackRate !== 1
                ? 'bg-[#1DB954] text-black shadow-lg'
                : 'liquid-button text-white/75 hover:text-white'
            }`}
            title="Playback Speed (Slowed / Sped Up)"
          >
            <Gauge className="w-3.5 h-3.5" />
            <span>{playbackRate}x</span>
          </button>

          <button
            onClick={cycleSleepTimer}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all ${
              sleepTimerMinutes
                ? 'bg-purple-500 text-white shadow-lg'
                : 'liquid-button text-white/75 hover:text-white'
            }`}
            title="Sleep Timer"
          >
            <Moon className="w-3.5 h-3.5" />
            <span>{sleepTimerMinutes ? `${sleepTimerMinutes}m` : 'Sleep'}</span>
          </button>

          <button
            onClick={() => setActivePanel(activePanel === 'queue' ? 'art' : 'queue')}
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-full text-xs font-bold transition-all ${
              activePanel === 'queue'
                ? 'bg-white text-black shadow-lg'
                : 'liquid-button text-white/75 hover:text-white'
            }`}
          >
            <ListMusic className="w-3.5 h-3.5" />
            <span>Up Next</span>
          </button>
        </div>
      </div>
    </div>
  );
};

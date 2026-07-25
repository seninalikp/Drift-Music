import React, { useEffect, useRef } from 'react';
import { usePlayerStore } from '../store/usePlayerStore';

export const AudioEngine: React.FC = () => {
  const audioRef = useRef<HTMLAudioElement | null>(null);

  const currentTrack = usePlayerStore((s) => s.currentTrack);
  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const volume = usePlayerStore((s) => s.volume);
  const isMuted = usePlayerStore((s) => s.isMuted);
  const currentTime = usePlayerStore((s) => s.currentTime);

  const setIsPlaying = usePlayerStore((s) => s.setIsPlaying);
  const setCurrentTime = usePlayerStore((s) => s.setCurrentTime);
  const setDuration = usePlayerStore((s) => s.setDuration);
  const nextTrack = usePlayerStore((s) => s.nextTrack);
  const setAudioError = usePlayerStore((s) => s.setAudioError);

  // Sync track source
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio || !currentTrack) return;

    if (audio.src !== currentTrack.streamUrl) {
      audio.src = currentTrack.streamUrl;
      audio.load();

      if (isPlaying) {
        audio
          .play()
          .then(() => {
            setIsPlaying(true);
            setAudioError(null);
          })
          .catch((err) => {
            console.warn('Audio playback error:', err);
            setIsPlaying(false);
            setAudioError(
              `Playback failed for "${currentTrack.title}". The track stream URL may be restricted or blocked by CORS. Try Demo Mode or enter a valid SoundCloud Client ID in Settings.`
            );
          });
      }
    }
  }, [currentTrack]);

  // Sync Play / Pause state
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio || !currentTrack) return;

    if (isPlaying && audio.paused) {
      audio
        .play()
        .then(() => setAudioError(null))
        .catch((err) => {
          console.warn('Play error:', err);
          setIsPlaying(false);
          setAudioError(
            `Could not start playback. If using SoundCloud API, make sure your Client ID has access to stream streamable tracks.`
          );
        });
    } else if (!isPlaying && !audio.paused) {
      audio.pause();
    }
  }, [isPlaying]);

  // Sync volume & mute
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    audio.volume = isMuted ? 0 : volume;
  }, [volume, isMuted]);

  // Sync Seek from Store when scrubbed
  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) return;
    // Only update native audio currentTime if discrepancy is > 0.5s to prevent feedback loops
    if (Math.abs(audio.currentTime - currentTime) > 0.6) {
      audio.currentTime = currentTime;
    }
  }, [currentTime]);

  // Audio Event Listeners
  const handleTimeUpdate = () => {
    const audio = audioRef.current;
    if (!audio) return;
    setCurrentTime(audio.currentTime);
  };

  const handleLoadedMetadata = () => {
    const audio = audioRef.current;
    if (!audio) return;
    if (!isNaN(audio.duration) && audio.duration > 0) {
      setDuration(Math.round(audio.duration));
    }
  };

  const handleEnded = () => {
    nextTrack();
  };

  const handleError = () => {
    const audio = audioRef.current;
    if (!audio) return;
    setIsPlaying(false);
    if (currentTrack) {
      setAudioError(
        `Unable to play "${currentTrack.title}". The audio stream URL requires authorization or is restricted.`
      );
    }
  };

  return (
    <audio
      ref={audioRef}
      onTimeUpdate={handleTimeUpdate}
      onLoadedMetadata={handleLoadedMetadata}
      onEnded={handleEnded}
      onError={handleError}
      preload="metadata"
    />
  );
};

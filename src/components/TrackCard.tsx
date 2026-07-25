import React from 'react';
import { Play, Pause, Heart, Radio, Sparkles, ExternalLink, PlusCircle, AlertCircle } from 'lucide-react';
import { Track } from '../types';
import { usePlayerStore } from '../store/usePlayerStore';

interface TrackCardProps {
  track: Track;
  playlistContext?: Track[];
}

export const formatTime = (seconds: number): string => {
  if (isNaN(seconds) || seconds < 0) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
};

export const formatCount = (count?: number): string => {
  if (!count) return '';
  if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`;
  if (count >= 1000) return `${(count / 1000).toFixed(1)}K`;
  return count.toString();
};

export const TrackCard: React.FC<TrackCardProps> = ({ track, playlistContext }) => {
  const currentTrack = usePlayerStore((s) => s.currentTrack);
  const isPlaying = usePlayerStore((s) => s.isPlaying);
  const playTrack = usePlayerStore((s) => s.playTrack);
  const togglePlayPause = usePlayerStore((s) => s.togglePlayPause);
  const toggleLikeTrack = usePlayerStore((s) => s.toggleLikeTrack);
  const isLiked = usePlayerStore((s) => s.isLiked(track.id));
  const setIsAddToPlaylistOpen = usePlayerStore((s) => s.setIsAddToPlaylistOpen);
  const setSearchQuery = usePlayerStore((s) => s.setSearchQuery);
  const setActiveTab = usePlayerStore((s) => s.setActiveTab);

  const isCurrent = currentTrack?.id === track.id;

  const handleCardClick = () => {
    if (isCurrent) {
      togglePlayPause();
    } else {
      playTrack(track, playlistContext);
    }
  };

  const handleLikeClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    toggleLikeTrack(track);
  };

  const handleAddToPlaylistClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsAddToPlaylistOpen(true, track);
  };

  const handleFindMatch = (e: React.MouseEvent) => {
    e.stopPropagation();
    const query = `${track.artist} ${track.title}`;
    setSearchQuery(query);
    setActiveTab('search');
  };

  return (
    <div
      onClick={handleCardClick}
      className={`group relative flex flex-col p-3 rounded-xl bg-[#181818] hover:bg-[#282828] border border-[#242424] hover:border-[#333] transition-all duration-300 cursor-pointer shadow-md hover:shadow-2xl ${
        isCurrent ? 'ring-1 ring-[#1DB954]/50 bg-[#222222]' : ''
      }`}
      id={`track-card-${track.id}`}
    >
      {/* Artwork Container */}
      <div className="relative aspect-square w-full rounded-lg overflow-hidden bg-[#242424] mb-3 shadow-inner">
        <img
          src={track.artworkUrl}
          alt={track.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
          loading="lazy"
          onError={(e) => {
            (e.target as HTMLImageElement).src =
              'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80';
          }}
        />

        {/* Hover Gradient Overlay */}
        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
          <button
            onClick={(e) => {
              e.stopPropagation();
              handleCardClick();
            }}
            className="w-12 h-12 rounded-full bg-[#1DB954] hover:bg-[#1ed760] text-black flex items-center justify-center shadow-2xl hover:scale-110 transition-all transform translate-y-2 group-hover:translate-y-0"
            aria-label={isCurrent && isPlaying ? 'Pause' : 'Play'}
            id={`play-btn-${track.id}`}
          >
            {isCurrent && isPlaying ? (
              <Pause className="w-6 h-6 fill-black" />
            ) : (
              <Play className="w-6 h-6 fill-black ml-0.5" />
            )}
          </button>
        </div>

        {/* Top Badges */}
        <div className="absolute top-2 left-2 flex items-center gap-1 flex-wrap">
          {track.isUnmatched ? (
            <span
              onClick={handleFindMatch}
              className="px-1.5 py-0.5 rounded bg-amber-950/80 backdrop-blur-md text-[10px] font-bold text-amber-300 border border-amber-500/40 flex items-center gap-1 hover:bg-amber-900 transition-colors"
              title="Click to search SoundCloud match"
            >
              <AlertCircle className="w-2.5 h-2.5 text-amber-400" /> Unmatched
            </span>
          ) : track.isDemo ? (
            <span className="px-1.5 py-0.5 rounded bg-black/70 backdrop-blur-md text-[10px] font-bold text-emerald-400 border border-emerald-500/30 flex items-center gap-1">
              <Sparkles className="w-2.5 h-2.5" /> Demo
            </span>
          ) : (
            <span className="px-1.5 py-0.5 rounded bg-black/70 backdrop-blur-md text-[10px] font-bold text-orange-400 border border-orange-500/30 flex items-center gap-1">
              <Radio className="w-2.5 h-2.5" /> SoundCloud
            </span>
          )}
        </div>

        {/* Duration Chip */}
        <span className="absolute bottom-2 right-2 px-1.5 py-0.5 rounded bg-black/80 backdrop-blur-md text-[10px] font-medium text-gray-200">
          {formatTime(track.duration)}
        </span>
      </div>

      {/* Metadata & Actions */}
      <div className="flex flex-col flex-1 justify-between gap-1">
        <div>
          <h3
            className={`text-sm font-bold truncate ${
              isCurrent ? 'text-[#1DB954]' : 'text-white group-hover:text-white'
            }`}
          >
            {track.title}
          </h3>
          <p className="text-xs text-gray-400 truncate mt-0.5">{track.artist}</p>
        </div>

        {/* Footer row: Genre & Action Buttons */}
        <div className="flex items-center justify-between pt-2 border-t border-[#282828] mt-2">
          <span className="text-[10px] font-semibold text-gray-400 bg-[#222] px-2 py-0.5 rounded-full truncate max-w-[90px]">
            {track.genre || 'Music'}
          </span>

          <div className="flex items-center gap-1">
            <button
              onClick={handleAddToPlaylistClick}
              className="p-1 text-gray-400 hover:text-white transition-colors"
              title="Add to Playlist"
              aria-label="Add to Playlist"
              id={`add-playlist-btn-${track.id}`}
            >
              <PlusCircle className="w-4 h-4" />
            </button>

            {track.permalinkUrl && (
              <a
                href={track.permalinkUrl}
                target="_blank"
                rel="noreferrer"
                onClick={(e) => e.stopPropagation()}
                className="p-1 text-gray-400 hover:text-gray-200 transition-colors"
                title="Open on SoundCloud"
              >
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            )}

            <button
              onClick={handleLikeClick}
              className={`p-1 rounded-full hover:bg-[#333] transition-colors ${
                isLiked ? 'text-purple-400' : 'text-gray-400 hover:text-white'
              }`}
              title={isLiked ? 'Remove from Liked Songs' : 'Add to Liked Songs'}
              aria-label="Like track"
              id={`like-btn-${track.id}`}
            >
              <Heart className={`w-4 h-4 ${isLiked ? 'fill-purple-500 text-purple-500' : ''}`} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

import React, { useState } from 'react';
import { Play, Sparkles, Flame, Clock, TrendingUp } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { MOCK_TRACKS, MOCK_GENRES } from '../data/mockTracks';
import { TrackCard } from '../components/TrackCard';

export const HomeView: React.FC = () => {
  const [selectedGenre, setSelectedGenre] = useState('All Genres');
  const playTrack = usePlayerStore((s) => s.playTrack);
  const history = usePlayerStore((s) => s.history);
  const currentTrack = usePlayerStore((s) => s.currentTrack);

  const filteredTracks =
    selectedGenre === 'All Genres'
      ? MOCK_TRACKS
      : MOCK_TRACKS.filter((t) => t.genre === selectedGenre);

  const heroTrack = MOCK_TRACKS[0];

  return (
    <div className="flex flex-col gap-8 p-4 md:p-8 max-w-7xl mx-auto w-full pb-32">
      <div className="relative w-full rounded-3xl overflow-hidden bg-gradient-to-r from-purple-900 via-indigo-900 to-[#121212] p-6 md:p-10 border border-white/10 shadow-2xl flex flex-col md:flex-row items-center justify-between gap-8 group">
        <div className="absolute top-0 right-0 w-96 h-96 bg-[#1DB954]/20 rounded-full blur-3xl pointer-events-none" />

        <div className="flex flex-col gap-4 max-w-xl z-10">
          <div className="flex items-center gap-2">
            <span className="px-3 py-1 rounded-full bg-[#1DB954]/20 border border-[#1DB954]/40 text-[#1DB954] text-xs font-bold uppercase tracking-wider flex items-center gap-1.5">
              <Flame className="w-3.5 h-3.5" /> Featured Release
            </span>
          </div>

          <h1 className="text-2xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight">
            {heroTrack.title}
          </h1>

          <p className="text-sm sm:text-base text-gray-300 leading-relaxed font-medium">
            {heroTrack.description}
          </p>

          <div className="flex items-center gap-4 pt-2">
            <button
              onClick={() => playTrack(heroTrack, MOCK_TRACKS)}
              className="px-6 py-3 rounded-full bg-[#1DB954] hover:bg-[#1ed760] text-black font-extrabold text-sm flex items-center gap-2 shadow-xl hover:scale-105 transition-all"
              id="hero-play-now-btn"
            >
              <Play className="w-5 h-5 fill-black" />
              <span>Play Featured</span>
            </button>
            <span className="text-xs font-semibold text-gray-400">
              by <strong className="text-white">{heroTrack.artist}</strong>
            </span>
          </div>
        </div>

        <div
          onClick={() => playTrack(heroTrack, MOCK_TRACKS)}
          className="relative aspect-square w-48 h-48 sm:w-64 sm:h-64 rounded-2xl overflow-hidden shadow-2xl border-2 border-white/10 cursor-pointer shrink-0 z-10 group-hover:scale-105 transition-transform duration-500"
        >
          <img
            src={heroTrack.artworkUrl}
            alt={heroTrack.title}
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-black/30 group-hover:bg-black/10 transition-colors flex items-center justify-center">
            <div className="w-14 h-14 rounded-full bg-[#1DB954] text-black flex items-center justify-center shadow-2xl group-hover:scale-110 transition-transform">
              <Play className="w-7 h-7 fill-black ml-1" />
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sparkles className="w-5 h-5 text-[#1DB954]" />
            <h2 className="text-lg font-extrabold text-white tracking-tight">Explore Categories</h2>
          </div>
        </div>

        <div className="flex items-center gap-2.5 overflow-x-auto pb-2 custom-scrollbar">
          {MOCK_GENRES.map((genre) => (
            <button
              key={genre}
              onClick={() => setSelectedGenre(genre)}
              className={`px-4 py-2 rounded-full text-xs font-bold whitespace-nowrap transition-all border shrink-0 ${
                selectedGenre === genre
                  ? 'bg-white text-black border-white shadow-lg'
                  : 'bg-[#181818] hover:bg-[#282828] text-gray-300 border-[#282828]'
              }`}
              id={`genre-pill-${genre.replace(/\s+/g, '-').toLowerCase()}`}
            >
              {genre}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-[#FF5500]" />
            <h2 className="text-lg font-extrabold text-white tracking-tight">
              {selectedGenre === 'All Genres' ? 'Trending Right Now' : `${selectedGenre} Tracks`}
            </h2>
          </div>
          <span className="text-xs text-gray-400 font-medium">
            {filteredTracks.length} tracks available
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-4">
          {filteredTracks.map((track) => (
            <TrackCard key={`home-track-${track.id}`} track={track} playlistContext={filteredTracks} />
          ))}
        </div>
      </div>

      {history.length > 0 && (
        <div className="flex flex-col gap-4 pt-4 border-t border-[#282828]">
          <div className="flex items-center gap-2">
            <Clock className="w-5 h-5 text-purple-400" />
            <h2 className="text-lg font-extrabold text-white tracking-tight">Jump Back In</h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
            {history.slice(0, 6).map((track) => {
              const isCurrent = currentTrack?.id === track.id;
              return (
                <div
                  key={`home-hist-${track.id}`}
                  onClick={() => playTrack(track, history)}
                  className={`flex items-center gap-3 p-3 rounded-xl bg-[#181818] hover:bg-[#282828] border border-[#242424] hover:border-[#333] cursor-pointer transition-all group ${
                    isCurrent ? 'ring-1 ring-[#1DB954]' : ''
                  }`}
                >
                  <img
                    src={track.artworkUrl}
                    alt={track.title}
                    className="w-12 h-12 rounded-lg object-cover shrink-0 shadow"
                  />
                  <div className="flex flex-col min-w-0 flex-1">
                    <span className="text-sm font-bold text-white group-hover:text-[#1DB954] truncate">
                      {track.title}
                    </span>
                    <span className="text-xs text-gray-400 truncate">{track.artist}</span>
                  </div>
                  <div className="w-8 h-8 rounded-full bg-[#1DB954] text-black flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow">
                    <Play className="w-4 h-4 fill-black ml-0.5" />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};

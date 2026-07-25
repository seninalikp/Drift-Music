import React, { useState, useEffect } from 'react';
import { Search, Loader2, Radio, AlertCircle, Sparkles, Music2, Filter } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { Track } from '../types';
import { searchSoundCloudTracks, filterMockTracks } from '../lib/soundcloud';
import { TrackCard } from '../components/TrackCard';
import { MOCK_GENRES } from '../data/mockTracks';

export const SearchView: React.FC = () => {
  const searchQuery = usePlayerStore((s) => s.searchQuery);
  const setSearchQuery = usePlayerStore((s) => s.setSearchQuery);
  const clientId = usePlayerStore((s) => s.clientId);
  const apiMode = usePlayerStore((s) => s.apiMode);
  const setIsSettingsOpen = usePlayerStore((s) => s.setIsSettingsOpen);

  const [results, setResults] = useState<Track[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedGenre, setSelectedGenre] = useState('All Genres');

  useEffect(() => {
    let isCancelled = false;

    const performSearch = async () => {
      setLoading(true);
      setError(null);

      if (apiMode === 'live' && clientId && clientId.trim()) {
        const queryToUse = searchQuery.trim() || selectedGenre !== 'All Genres' ? searchQuery || selectedGenre : 'electronic';
        const res = await searchSoundCloudTracks(queryToUse, clientId);

        if (!isCancelled) {
          setResults(res.tracks);
          if (res.error) {
            setError(res.error);
          }
          setLoading(false);
        }
      } else {
        // Demo Mode / Fallback local search
        const tracks = filterMockTracks(searchQuery, selectedGenre);
        if (!isCancelled) {
          setResults(tracks);
          setLoading(false);
        }
      }
    };

    performSearch();

    return () => {
      isCancelled = true;
    };
  }, [searchQuery, clientId, apiMode, selectedGenre]);

  return (
    <div className="flex flex-col gap-6 p-4 md:p-8 max-w-7xl mx-auto w-full pb-32">
      {/* Header Info Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-[#282828]">
        <div className="flex flex-col">
          <div className="flex items-center gap-2">
            <Search className="w-6 h-6 text-[#1DB954]" />
            <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight">
              {searchQuery ? `Search results for "${searchQuery}"` : 'Browse & Search Music'}
            </h1>
          </div>
          <span className="text-xs text-gray-400 mt-1">
            {apiMode === 'live' && clientId
              ? 'Searching live SoundCloud tracks'
              : 'Searching curated Demo music collection'}
          </span>
        </div>

        {/* Mode Toggle Button */}
        <button
          onClick={() => setIsSettingsOpen(true)}
          className="flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#181818] hover:bg-[#282828] border border-[#282828] text-xs font-semibold text-gray-300 hover:text-white transition-colors self-start sm:self-auto"
          id="search-mode-settings-btn"
        >
          {apiMode === 'live' && clientId ? (
            <>
              <Radio className="w-3.5 h-3.5 text-[#FF5500]" />
              <span>SoundCloud API Active</span>
            </>
          ) : (
            <>
              <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
              <span>Demo Search Active</span>
            </>
          )}
        </button>
      </div>

      {/* Genre Filter Pills */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 custom-scrollbar">
        <span className="text-xs font-bold text-gray-400 shrink-0 flex items-center gap-1 mr-1">
          <Filter className="w-3.5 h-3.5" /> Filter:
        </span>
        {MOCK_GENRES.map((genre) => (
          <button
            key={`search-genre-${genre}`}
            onClick={() => setSelectedGenre(genre)}
            className={`px-3.5 py-1.5 rounded-full text-xs font-semibold whitespace-nowrap transition-all border shrink-0 ${
              selectedGenre === genre
                ? 'bg-[#1DB954] text-black border-[#1DB954]'
                : 'bg-[#181818] hover:bg-[#282828] text-gray-300 border-[#282828]'
            }`}
            id={`search-filter-${genre.replace(/\s+/g, '-').toLowerCase()}`}
          >
            {genre}
          </button>
        ))}
      </div>

      {/* Actionable Error Callout if API Error Occurred */}
      {error && (
        <div className="p-4 rounded-xl bg-[#FF5500]/15 border border-[#FF5500]/40 text-xs text-orange-200 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-lg">
          <div className="flex items-center gap-3">
            <AlertCircle className="w-5 h-5 text-[#FF5500] shrink-0" />
            <span>{error}</span>
          </div>
          <button
            onClick={() => setIsSettingsOpen(true)}
            className="px-4 py-1.5 rounded-full bg-[#FF5500] text-black font-extrabold text-xs hover:bg-[#e04b00] transition-colors shrink-0"
            id="search-fix-id-btn"
          >
            Update Client ID
          </button>
        </div>
      )}

      {/* Results Section */}
      {loading ? (
        <div className="flex flex-col items-center justify-center py-20 gap-3 text-gray-400">
          <Loader2 className="w-8 h-8 animate-spin text-[#1DB954]" />
          <span className="text-sm font-semibold">Searching tracks...</span>
        </div>
      ) : results.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 gap-4 text-center">
          <div className="w-16 h-16 rounded-full bg-[#181818] border border-[#282828] flex items-center justify-center text-gray-400">
            <Music2 className="w-8 h-8" />
          </div>
          <div className="flex flex-col max-w-sm">
            <h3 className="text-lg font-bold text-white">No tracks found</h3>
            <p className="text-xs text-gray-400 mt-1">
              Try searching for artists, song titles, or genres like "Synthwave", "Lo-Fi", or "Chill".
            </p>
          </div>
          <button
            onClick={() => {
              setSearchQuery('');
              setSelectedGenre('All Genres');
            }}
            className="px-4 py-2 rounded-full bg-[#282828] hover:bg-[#333] text-xs font-bold text-white transition-colors"
            id="reset-search-btn"
          >
            Reset Search & Filters
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-4">
          {results.map((track) => (
            <TrackCard key={`search-card-${track.id}`} track={track} playlistContext={results} />
          ))}
        </div>
      )}
    </div>
  );
};

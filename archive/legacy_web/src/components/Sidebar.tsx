import React from 'react';
import { Home, Search, Library, Heart, Radio, ListMusic, History, PlusCircle, Sparkles, Music } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';

export const Sidebar: React.FC = () => {
  const activeTab = usePlayerStore((s) => s.activeTab);
  const setActiveTab = usePlayerStore((s) => s.setActiveTab);
  const likedTracks = usePlayerStore((s) => s.likedTracks);
  const playlists = usePlayerStore((s) => s.playlists);
  const history = usePlayerStore((s) => s.history);
  const playTrack = usePlayerStore((s) => s.playTrack);
  const setIsSettingsOpen = usePlayerStore((s) => s.setIsSettingsOpen);
  const createPlaylist = usePlayerStore((s) => s.createPlaylist);
  const setSelectedPlaylistIdForView = usePlayerStore((s) => s.setSelectedPlaylistIdForView);
  const selectedPlaylistIdForView = usePlayerStore((s) => s.selectedPlaylistIdForView);

  const handleCreateNewPlaylist = () => {
    const title = prompt('Enter a name for your new playlist:', 'New Playlist');
    if (title && title.trim()) {
      const pl = createPlaylist(title.trim());
      setSelectedPlaylistIdForView(pl.id);
      setActiveTab('library');
    }
  };

  return (
    <aside className="hidden md:flex flex-col w-64 shrink-0 bg-[#000000] p-2 gap-2 select-none h-[calc(100vh-65px)]">
      {/* Top Main Navigation Card */}
      <nav className="bg-[#121212] rounded-xl p-3 flex flex-col gap-1 border border-[#1e1e1e]">
        <button
          onClick={() => {
            setSelectedPlaylistIdForView(null);
            setActiveTab('home');
          }}
          className={`flex items-center gap-4 px-3 py-2.5 rounded-lg text-sm font-bold transition-all ${
            activeTab === 'home'
              ? 'text-white bg-[#282828]'
              : 'text-gray-400 hover:text-white hover:bg-[#1a1a1a]'
          }`}
          id="nav-home-btn"
        >
          <Home className={`w-5 h-5 ${activeTab === 'home' ? 'text-[#1DB954]' : ''}`} />
          <span>Home</span>
        </button>

        <button
          onClick={() => {
            setSelectedPlaylistIdForView(null);
            setActiveTab('search');
          }}
          className={`flex items-center gap-4 px-3 py-2.5 rounded-lg text-sm font-bold transition-all ${
            activeTab === 'search'
              ? 'text-white bg-[#282828]'
              : 'text-gray-400 hover:text-white hover:bg-[#1a1a1a]'
          }`}
          id="nav-search-btn"
        >
          <Search className={`w-5 h-5 ${activeTab === 'search' ? 'text-[#1DB954]' : ''}`} />
          <span>Search</span>
        </button>

        <button
          onClick={() => {
            setSelectedPlaylistIdForView(null);
            setActiveTab('library');
          }}
          className={`flex items-center gap-4 px-3 py-2.5 rounded-lg text-sm font-bold transition-all ${
            activeTab === 'library' && !selectedPlaylistIdForView
              ? 'text-white bg-[#282828]'
              : 'text-gray-400 hover:text-white hover:bg-[#1a1a1a]'
          }`}
          id="nav-library-btn"
        >
          <Library className={`w-5 h-5 ${activeTab === 'library' && !selectedPlaylistIdForView ? 'text-[#1DB954]' : ''}`} />
          <span>Your Library</span>
        </button>
      </nav>

      {/* Library & Playlists Container */}
      <div className="flex-1 bg-[#121212] rounded-xl p-3 flex flex-col gap-3 overflow-hidden border border-[#1e1e1e]">
        <div className="flex items-center justify-between px-1 text-gray-400">
          <div className="flex items-center gap-2">
            <ListMusic className="w-4 h-4 text-gray-400" />
            <span className="text-xs font-bold uppercase tracking-wider">Playlists ({playlists.length + 1})</span>
          </div>
          <button
            onClick={handleCreateNewPlaylist}
            className="p-1 hover:text-white hover:bg-[#282828] rounded-full transition-colors"
            title="Create Playlist"
            id="sidebar-add-playlist-btn"
          >
            <PlusCircle className="w-4 h-4 text-[#1DB954]" />
          </button>
        </div>

        {/* Liked Songs Quick Card */}
        <div
          onClick={() => {
            setSelectedPlaylistIdForView(null);
            setActiveTab('library');
          }}
          className={`flex items-center gap-3 p-2.5 rounded-lg bg-gradient-to-r from-purple-900/40 to-indigo-900/40 hover:from-purple-900/60 hover:to-indigo-900/60 border border-purple-500/20 cursor-pointer transition-all group ${
            activeTab === 'library' && !selectedPlaylistIdForView ? 'ring-1 ring-[#1DB954]' : ''
          }`}
          id="sidebar-liked-songs-card"
        >
          <div className="w-9 h-9 rounded bg-gradient-to-br from-purple-600 to-blue-500 flex items-center justify-center text-white shrink-0 shadow-md group-hover:scale-105 transition-transform">
            <Heart className="w-4 h-4 fill-white" />
          </div>
          <div className="flex flex-col min-w-0">
            <span className="text-xs font-bold text-white truncate">Liked Songs</span>
            <span className="text-[10px] text-gray-400">{likedTracks.length} tracks</span>
          </div>
        </div>

        {/* Custom Playlists & Recent History */}
        <div className="flex-1 overflow-y-auto pr-1 flex flex-col gap-1.5 custom-scrollbar">
          {playlists.length > 0 && (
            <div className="flex flex-col gap-1">
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider px-1 pt-1">
                Custom Playlists
              </span>
              {playlists.map((pl) => {
                const isSelected = selectedPlaylistIdForView === pl.id && activeTab === 'library';
                return (
                  <div
                    key={`side-pl-${pl.id}`}
                    onClick={() => {
                      setSelectedPlaylistIdForView(pl.id);
                      setActiveTab('library');
                    }}
                    className={`flex items-center gap-2.5 p-2 rounded-lg cursor-pointer group transition-all ${
                      isSelected
                        ? 'bg-[#282828] text-white border border-[#1DB954]/40'
                        : 'hover:bg-[#1f1f1f] text-gray-300'
                    }`}
                    id={`sidebar-playlist-${pl.id}`}
                  >
                    <div className="w-8 h-8 rounded bg-[#222] border border-[#303030] flex items-center justify-center shrink-0 overflow-hidden">
                      {pl.coverUrl ? (
                        <img src={pl.coverUrl} alt={pl.name} className="w-full h-full object-cover" />
                      ) : (
                        <Music className="w-4 h-4 text-gray-400" />
                      )}
                    </div>
                    <div className="flex flex-col min-w-0">
                      <span className="text-xs font-semibold group-hover:text-[#1DB954] truncate">
                        {pl.name}
                      </span>
                      <span className="text-[10px] text-gray-400 truncate">{pl.tracks.length} tracks</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <div className="text-[10px] font-bold text-gray-400 uppercase tracking-wider px-1 pt-2 flex items-center gap-1.5">
            <History className="w-3.5 h-3.5" />
            <span>Recently Played</span>
          </div>

          {history.length === 0 ? (
            <p className="text-xs text-gray-400 px-1 py-1 italic text-[11px]">No history yet.</p>
          ) : (
            history.slice(0, 5).map((track) => (
              <div
                key={`side-hist-${track.id}`}
                onClick={() => playTrack(track, history)}
                className="flex items-center gap-2.5 p-1.5 rounded-md hover:bg-[#282828] cursor-pointer group transition-colors"
              >
                <img
                  src={track.artworkUrl}
                  alt={track.title}
                  className="w-8 h-8 rounded object-cover shrink-0 bg-[#282828]"
                  loading="lazy"
                />
                <div className="flex flex-col min-w-0">
                  <span className="text-[11px] font-semibold text-gray-200 group-hover:text-[#1DB954] truncate">
                    {track.title}
                  </span>
                  <span className="text-[10px] text-gray-400 truncate">{track.artist}</span>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Bottom Config Banner */}
        <div className="p-2.5 rounded-lg bg-[#181818] border border-[#282828] flex flex-col gap-1.5">
          <div className="flex items-center gap-1.5 text-xs font-semibold text-gray-300">
            <Radio className="w-3.5 h-3.5 text-[#FF5500]" />
            <span>SoundCloud API</span>
          </div>
          <p className="text-[10px] text-gray-400 leading-snug">
            Connect Client ID for live tracks.
          </p>
          <button
            onClick={() => setIsSettingsOpen(true)}
            className="w-full py-1 rounded-full bg-[#282828] hover:bg-[#333] text-[11px] font-bold text-white transition-colors flex items-center justify-center gap-1"
            id="sidebar-config-btn"
          >
            <Sparkles className="w-3 h-3 text-[#1DB954]" />
            <span>Settings</span>
          </button>
        </div>
      </div>
    </aside>
  );
};

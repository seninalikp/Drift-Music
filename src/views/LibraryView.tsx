import React, { useState } from 'react';
import {
  Heart,
  Play,
  Trash2,
  History,
  Music2,
  ListMusic,
  PlusCircle,
  ArrowUp,
  ArrowDown,
  Edit2,
  Check,
  Radio,
  Sparkles,
} from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { formatTime } from '../components/TrackCard';
import { TrackCard } from '../components/TrackCard';
import { SpotifyImportModal } from '../components/SpotifyImportModal';

export const LibraryView: React.FC = () => {
  const likedTracks = usePlayerStore((s) => s.likedTracks);
  const playlists = usePlayerStore((s) => s.playlists);
  const selectedPlaylistId = usePlayerStore((s) => s.selectedPlaylistIdForView);
  const setSelectedPlaylistId = usePlayerStore((s) => s.setSelectedPlaylistIdForView);
  const history = usePlayerStore((s) => s.history);
  const playTrack = usePlayerStore((s) => s.playTrack);
  const clearLibrary = usePlayerStore((s) => s.clearLibrary);
  const createPlaylist = usePlayerStore((s) => s.createPlaylist);
  const renamePlaylist = usePlayerStore((s) => s.renamePlaylist);
  const deletePlaylist = usePlayerStore((s) => s.deletePlaylist);
  const removeTrackFromPlaylist = usePlayerStore((s) => s.removeTrackFromPlaylist);
  const reorderPlaylistTracks = usePlayerStore((s) => s.reorderPlaylistTracks);

  const [isSpotifyModalOpen, setIsSpotifyModalOpen] = useState(false);
  const [isEditingPlaylist, setIsEditingPlaylist] = useState(false);
  const [editName, setEditName] = useState('');
  const [editDesc, setEditDesc] = useState('');

  const currentPlaylist = playlists.find((p) => p.id === selectedPlaylistId);

  const handleCreateNewPlaylist = () => {
    const title = prompt('Enter a name for your new playlist:', 'My Custom Playlist');
    if (title && title.trim()) {
      const pl = createPlaylist(title.trim());
      setSelectedPlaylistId(pl.id);
    }
  };

  const handleStartRename = () => {
    if (!currentPlaylist) return;
    setEditName(currentPlaylist.name);
    setEditDesc(currentPlaylist.description || '');
    setIsEditingPlaylist(true);
  };

  const handleSaveRename = () => {
    if (!currentPlaylist) return;
    renamePlaylist(currentPlaylist.id, editName, editDesc);
    setIsEditingPlaylist(false);
  };

  const handleDeletePlaylist = () => {
    if (!currentPlaylist) return;
    if (confirm(`Are you sure you want to delete "${currentPlaylist.name}"?`)) {
      deletePlaylist(currentPlaylist.id);
      setSelectedPlaylistId(null);
    }
  };

  // If a specific playlist is selected to view
  if (currentPlaylist) {
    const totalSecs = currentPlaylist.tracks.reduce((acc, t) => acc + (t.duration || 0), 0);
    const totalMins = Math.floor(totalSecs / 60);

    return (
      <div className="flex flex-col gap-8 p-4 md:p-8 max-w-7xl mx-auto w-full pb-32">
        {/* Playlist Header */}
        <div className="relative w-full rounded-3xl overflow-hidden bg-gradient-to-r from-emerald-900/60 via-indigo-900/60 to-[#121212] p-6 md:p-10 border border-white/10 shadow-2xl flex flex-col sm:flex-row items-center sm:items-end gap-6">
          <div className="w-32 h-32 sm:w-40 sm:h-40 rounded-2xl bg-[#222] border border-[#333] flex items-center justify-center text-white shadow-2xl shrink-0 overflow-hidden">
            {currentPlaylist.coverUrl ? (
              <img
                src={currentPlaylist.coverUrl}
                alt={currentPlaylist.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <ListMusic className="w-16 h-16 text-gray-400" />
            )}
          </div>

          <div className="flex flex-col gap-2 min-w-0 text-center sm:text-left flex-1">
            <div className="flex items-center justify-center sm:justify-start gap-2">
              <span className="text-xs uppercase font-extrabold text-[#1DB954] tracking-widest">
                Custom Playlist
              </span>
              <button
                onClick={() => setSelectedPlaylistId(null)}
                className="text-xs text-gray-400 hover:text-white underline ml-2"
                id="back-to-liked-btn"
              >
                ← Back to Liked Songs
              </button>
            </div>

            {isEditingPlaylist ? (
              <div className="flex flex-col gap-2 max-w-md my-1">
                <input
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="bg-[#181818] text-white text-xl font-bold px-3 py-1.5 rounded-lg border border-[#1DB954] focus:outline-none"
                  id="playlist-rename-input"
                />
                <input
                  type="text"
                  value={editDesc}
                  onChange={(e) => setEditDesc(e.target.value)}
                  placeholder="Playlist description..."
                  className="bg-[#181818] text-gray-300 text-xs px-3 py-1 rounded-lg border border-[#303030] focus:outline-none"
                  id="playlist-desc-input"
                />
                <div className="flex items-center gap-2 pt-1">
                  <button
                    onClick={handleSaveRename}
                    className="px-3 py-1 rounded bg-[#1DB954] text-black font-extrabold text-xs flex items-center gap-1"
                    id="save-playlist-rename-btn"
                  >
                    <Check className="w-3.5 h-3.5" /> Save
                  </button>
                  <button
                    onClick={() => setIsEditingPlaylist(false)}
                    className="px-3 py-1 rounded text-xs text-gray-400 hover:text-white"
                    id="cancel-playlist-rename-btn"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="flex items-center justify-center sm:justify-start gap-3">
                  <h1 className="text-2xl sm:text-4xl font-black text-white tracking-tight">
                    {currentPlaylist.name}
                  </h1>
                  <button
                    onClick={handleStartRename}
                    className="p-1.5 rounded-full hover:bg-[#282828] text-gray-400 hover:text-white transition-colors"
                    title="Rename Playlist"
                    id="rename-playlist-icon-btn"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                </div>
                {currentPlaylist.description && (
                  <p className="text-xs sm:text-sm text-gray-300 font-medium">
                    {currentPlaylist.description}
                  </p>
                )}
              </>
            )}

            <div className="flex items-center justify-center sm:justify-start gap-2 text-xs text-gray-300 font-medium mt-1">
              <strong className="text-white">{currentPlaylist.tracks.length} tracks</strong>
              <span>•</span>
              <span>Approx. {totalMins} minutes</span>
            </div>
          </div>

          <div className="sm:ml-auto flex items-center gap-2">
            {currentPlaylist.tracks.length > 0 && (
              <button
                onClick={() => playTrack(currentPlaylist.tracks[0], currentPlaylist.tracks)}
                className="px-6 py-3 rounded-full bg-[#1DB954] hover:bg-[#1ed760] text-black font-extrabold text-sm flex items-center gap-2 shadow-xl hover:scale-105 transition-all"
                id="playlist-play-all-btn"
              >
                <Play className="w-5 h-5 fill-black" />
                <span>Play Playlist</span>
              </button>
            )}

            <button
              onClick={handleDeletePlaylist}
              className="p-3 rounded-full bg-red-950/40 hover:bg-red-900/60 border border-red-500/30 text-red-300 transition-colors"
              title="Delete Playlist"
              id="delete-playlist-btn"
            >
              <Trash2 className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Tracks Table */}
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between pb-2 border-b border-[#282828]">
            <h2 className="text-base font-bold text-white">Playlist Tracks</h2>
            <span className="text-xs text-gray-400">Reorder or remove tracks anytime</span>
          </div>

          {currentPlaylist.tracks.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 gap-3 text-center bg-[#181818] rounded-2xl border border-[#282828]">
              <Music2 className="w-10 h-10 text-gray-400" />
              <h3 className="text-base font-bold text-white">Playlist is empty</h3>
              <p className="text-xs text-gray-400 max-w-sm">
                Browse tracks on Home or Search and click the "+ Playlist" button to add music here!
              </p>
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {currentPlaylist.tracks.map((trk, idx) => (
                <div
                  key={`pl-trk-${trk.id}-${idx}`}
                  className="flex items-center justify-between p-3 rounded-xl bg-[#181818] hover:bg-[#282828] border border-[#242424] transition-all group"
                >
                  <div className="flex items-center gap-3 min-w-0 flex-1">
                    <span className="text-xs font-mono text-gray-400 w-5 text-right">{idx + 1}</span>
                    <img
                      src={trk.artworkUrl}
                      alt={trk.title}
                      className="w-10 h-10 rounded-lg object-cover bg-[#333] shrink-0"
                    />
                    <div className="flex flex-col min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-white group-hover:text-[#1DB954] truncate">
                          {trk.title}
                        </span>
                        {trk.isUnmatched && (
                          <span className="px-1.5 py-0.5 rounded bg-amber-500/20 text-amber-300 text-[10px] font-bold">
                            Unmatched
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-gray-400 truncate">{trk.artist}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => playTrack(trk, currentPlaylist.tracks)}
                      className="px-3 py-1 rounded-full bg-[#1DB954] text-black font-extrabold text-xs hover:bg-[#1ed760] transition-colors"
                      id={`play-pl-trk-${trk.id}`}
                    >
                      Play
                    </button>

                    {/* Reorder Buttons */}
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => reorderPlaylistTracks(currentPlaylist.id, idx, Math.max(0, idx - 1))}
                        disabled={idx === 0}
                        className="p-1 rounded hover:bg-[#333] disabled:opacity-30 text-gray-400 hover:text-white"
                        title="Move Up"
                      >
                        <ArrowUp className="w-3.5 h-3.5" />
                      </button>
                      <button
                        onClick={() =>
                          reorderPlaylistTracks(
                            currentPlaylist.id,
                            idx,
                            Math.min(currentPlaylist.tracks.length - 1, idx + 1)
                          )
                        }
                        disabled={idx === currentPlaylist.tracks.length - 1}
                        className="p-1 rounded hover:bg-[#333] disabled:opacity-30 text-gray-400 hover:text-white"
                        title="Move Down"
                      >
                        <ArrowDown className="w-3.5 h-3.5" />
                      </button>
                    </div>

                    <button
                      onClick={() => removeTrackFromPlaylist(currentPlaylist.id, trk.id)}
                      className="p-1.5 rounded hover:bg-red-500/20 text-gray-400 hover:text-red-400 transition-colors ml-1"
                      title="Remove from playlist"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }

  // Default Liked Songs + Playlists overview
  return (
    <div className="flex flex-col gap-8 p-4 md:p-8 max-w-7xl mx-auto w-full pb-32">
      {/* Liked Songs Hero Header */}
      <div className="relative w-full rounded-3xl overflow-hidden bg-gradient-to-r from-purple-900 via-indigo-900 to-[#121212] p-6 md:p-10 border border-white/10 shadow-2xl flex flex-col sm:flex-row items-center sm:items-end gap-6">
        <div className="w-32 h-32 sm:w-40 sm:h-40 rounded-2xl bg-gradient-to-br from-purple-600 to-blue-500 flex items-center justify-center text-white shadow-2xl shrink-0">
          <Heart className="w-16 h-16 fill-white" />
        </div>

        <div className="flex flex-col gap-2 min-w-0 text-center sm:text-left flex-1">
          <span className="text-xs uppercase font-extrabold text-purple-300 tracking-widest">
            Collection
          </span>
          <h1 className="text-3xl sm:text-5xl font-black text-white tracking-tight">
            Liked Songs
          </h1>
          <div className="flex items-center justify-center sm:justify-start gap-2 text-xs sm:text-sm text-gray-300 font-medium mt-1">
            <strong className="text-white">{likedTracks.length} saved tracks</strong>
            <span>•</span>
            <span>{playlists.length} custom playlists</span>
          </div>
        </div>

        <div className="sm:ml-auto flex items-center gap-3 flex-wrap">
          <button
            onClick={() => setIsSpotifyModalOpen(true)}
            className="px-4 py-2.5 rounded-full bg-[#1DB954]/20 hover:bg-[#1DB954]/30 border border-[#1DB954]/40 text-[#1DB954] font-extrabold text-xs flex items-center gap-2 transition-all shadow-lg"
            id="spotify-import-btn"
          >
            <div className="w-4 h-4 rounded-full bg-[#1DB954] text-black flex items-center justify-center text-[9px] font-extrabold">
              S
            </div>
            <span>Spotify Import</span>
          </button>

          <button
            onClick={handleCreateNewPlaylist}
            className="px-4 py-2.5 rounded-full bg-[#282828] hover:bg-[#333] text-white font-extrabold text-xs flex items-center gap-2 transition-all"
            id="create-playlist-btn"
          >
            <PlusCircle className="w-4 h-4 text-[#1DB954]" />
            <span>New Playlist</span>
          </button>

          {likedTracks.length > 0 && (
            <button
              onClick={() => playTrack(likedTracks[0], likedTracks)}
              className="px-6 py-2.5 rounded-full bg-[#1DB954] hover:bg-[#1ed760] text-black font-extrabold text-xs flex items-center gap-2 shadow-xl hover:scale-105 transition-all"
              id="library-play-all-btn"
            >
              <Play className="w-4 h-4 fill-black" />
              <span>Play All</span>
            </button>
          )}
        </div>
      </div>

      {/* Playlists Quick Bar */}
      {playlists.length > 0 && (
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-extrabold text-white tracking-tight flex items-center gap-2">
              <ListMusic className="w-5 h-5 text-[#1DB954]" /> Custom Playlists
            </h2>
            <span className="text-xs text-gray-400">{playlists.length} created</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
            {playlists.map((pl) => (
              <div
                key={`pl-grid-${pl.id}`}
                onClick={() => setSelectedPlaylistId(pl.id)}
                className="flex items-center gap-3 p-3 rounded-2xl bg-[#181818] hover:bg-[#282828] border border-[#242424] hover:border-[#1DB954]/40 cursor-pointer transition-all group shadow"
              >
                <div className="w-12 h-12 rounded-xl bg-[#222] border border-[#333] flex items-center justify-center shrink-0 overflow-hidden">
                  {pl.coverUrl ? (
                    <img src={pl.coverUrl} alt={pl.name} className="w-full h-full object-cover" />
                  ) : (
                    <Music2 className="w-6 h-6 text-gray-400" />
                  )}
                </div>
                <div className="flex flex-col min-w-0 flex-1">
                  <span className="text-sm font-bold text-white group-hover:text-[#1DB954] truncate">
                    {pl.name}
                  </span>
                  <span className="text-xs text-gray-400">{pl.tracks.length} tracks</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Liked Songs Track Grid */}
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between pb-2 border-b border-[#282828]">
          <h2 className="text-lg font-extrabold text-white tracking-tight flex items-center gap-2">
            <Heart className="w-5 h-5 text-purple-400 fill-purple-400" /> Saved Tracks
          </h2>

          {likedTracks.length > 0 && (
            <button
              onClick={() => {
                if (confirm('Clear all liked songs?')) clearLibrary();
              }}
              className="text-xs text-gray-400 hover:text-red-400 flex items-center gap-1.5 transition-colors"
              id="clear-liked-songs-btn"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Clear Saved</span>
            </button>
          )}
        </div>

        {likedTracks.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 gap-3 text-center bg-[#181818] rounded-2xl border border-[#282828]">
            <Music2 className="w-10 h-10 text-gray-400" />
            <h3 className="text-base font-bold text-white">No liked songs yet</h3>
            <p className="text-xs text-gray-400 max-w-sm">
              Click the heart icon on any track card to save your favorite songs here for easy access.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-4">
            {likedTracks.map((track) => (
              <TrackCard key={`liked-${track.id}`} track={track} playlistContext={likedTracks} />
            ))}
          </div>
        )}
      </div>

      {/* History Log Section */}
      {history.length > 0 && (
        <div className="flex flex-col gap-4 pt-6 border-t border-[#282828]">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-extrabold text-white tracking-tight flex items-center gap-2">
              <History className="w-5 h-5 text-blue-400" /> Recently Streamed
            </h2>
            <span className="text-xs text-gray-400">{history.length} sessions</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
            {history.slice(0, 9).map((track, i) => (
              <div
                key={`hist-page-${track.id}-${i}`}
                onClick={() => playTrack(track, history)}
                className="flex items-center gap-3 p-3 rounded-xl bg-[#181818] hover:bg-[#282828] border border-[#242424] hover:border-[#333] cursor-pointer transition-all group"
              >
                <img
                  src={track.artworkUrl}
                  alt={track.title}
                  className="w-10 h-10 rounded-lg object-cover shrink-0"
                />
                <div className="flex flex-col min-w-0 flex-1">
                  <span className="text-xs font-bold text-white group-hover:text-[#1DB954] truncate">
                    {track.title}
                  </span>
                  <span className="text-[11px] text-gray-400 truncate">{track.artist}</span>
                </div>
                <span className="text-[10px] font-mono text-gray-400">
                  {formatTime(track.duration)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Spotify Import Modal */}
      <SpotifyImportModal
        isOpen={isSpotifyModalOpen}
        onClose={() => setIsSpotifyModalOpen(false)}
      />
    </div>
  );
};

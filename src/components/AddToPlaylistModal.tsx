import React, { useState } from 'react';
import { X, Plus, Check, ListMusic, Music, Sparkles } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';

export const AddToPlaylistModal: React.FC = () => {
  const isAddToPlaylistOpen = usePlayerStore((s) => s.isAddToPlaylistOpen);
  const setIsAddToPlaylistOpen = usePlayerStore((s) => s.setIsAddToPlaylistOpen);
  const track = usePlayerStore((s) => s.trackToAddToPlaylist);
  const playlists = usePlayerStore((s) => s.playlists);
  const addTrackToPlaylist = usePlayerStore((s) => s.addTrackToPlaylist);
  const removeTrackFromPlaylist = usePlayerStore((s) => s.removeTrackFromPlaylist);
  const createPlaylist = usePlayerStore((s) => s.createPlaylist);

  const [newPlaylistName, setNewPlaylistName] = useState('');
  const [showCreateForm, setShowCreateForm] = useState(false);

  if (!isAddToPlaylistOpen || !track) return null;

  const handleCreateNewPlaylist = () => {
    if (!newPlaylistName.trim()) return;
    const newPl = createPlaylist(newPlaylistName.trim());
    addTrackToPlaylist(newPl.id, track);
    setNewPlaylistName('');
    setShowCreateForm(false);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 select-none animate-fadeIn">
      <div className="bg-[#181818] border border-[#282828] rounded-2xl max-w-sm w-full p-6 shadow-2xl flex flex-col gap-5 relative max-h-[85vh]">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#282828]">
          <div className="flex items-center gap-2">
            <ListMusic className="w-5 h-5 text-[#1DB954]" />
            <h2 className="text-base font-bold text-white">Add to Playlist</h2>
          </div>
          <button
            onClick={() => setIsAddToPlaylistOpen(false, null)}
            className="p-1.5 rounded-full hover:bg-[#282828] text-gray-400 hover:text-white transition-colors"
            id="add-to-playlist-close-btn"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Selected Track Preview */}
        <div className="flex items-center gap-3 p-2.5 rounded-xl bg-[#222] border border-[#2a2a2a]">
          <img
            src={track.artworkUrl}
            alt={track.title}
            className="w-10 h-10 rounded-lg object-cover bg-[#333]"
          />
          <div className="flex flex-col min-w-0 flex-1">
            <span className="text-xs font-bold text-white truncate">{track.title}</span>
            <span className="text-[11px] text-gray-400 truncate">{track.artist}</span>
          </div>
        </div>

        {/* Create New Playlist Toggle Button */}
        {!showCreateForm ? (
          <button
            onClick={() => setShowCreateForm(true)}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-purple-900/40 to-indigo-900/40 hover:from-purple-900/60 hover:to-indigo-900/60 border border-purple-500/30 text-white text-xs font-bold flex items-center justify-center gap-2 transition-all shadow-md"
            id="modal-create-new-playlist-btn"
          >
            <Plus className="w-4 h-4 text-[#1DB954]" />
            <span>Create New Playlist</span>
          </button>
        ) : (
          <div className="flex flex-col gap-2 p-3 rounded-xl bg-[#222] border border-[#303030]">
            <input
              type="text"
              value={newPlaylistName}
              onChange={(e) => setNewPlaylistName(e.target.value)}
              placeholder="Playlist Title..."
              autoFocus
              className="w-full bg-[#181818] text-white text-xs px-3 py-2 rounded-lg border border-[#383838] focus:border-[#1DB954] focus:outline-none"
              id="new-playlist-name-input"
            />
            <div className="flex items-center justify-end gap-2 pt-1">
              <button
                onClick={() => setShowCreateForm(false)}
                className="px-3 py-1 rounded-md text-[11px] font-semibold text-gray-400 hover:text-white"
                id="cancel-new-playlist-btn"
              >
                Cancel
              </button>
              <button
                onClick={handleCreateNewPlaylist}
                className="px-3 py-1 rounded-md bg-[#1DB954] text-black font-extrabold text-[11px] hover:bg-[#1ed760]"
                id="save-new-playlist-btn"
              >
                Create & Add
              </button>
            </div>
          </div>
        )}

        {/* Existing Playlists Checklist */}
        <div className="flex flex-col gap-1.5 overflow-y-auto max-h-60 pr-1 custom-scrollbar">
          <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider px-1">
            Your Playlists
          </span>

          {playlists.length === 0 ? (
            <p className="text-xs text-gray-400 p-3 italic text-center">No playlists yet. Create one above!</p>
          ) : (
            playlists.map((pl) => {
              const inPlaylist = pl.tracks.some((t) => t.id === track.id);
              return (
                <div
                  key={`add-to-pl-${pl.id}`}
                  onClick={() => {
                    if (inPlaylist) {
                      removeTrackFromPlaylist(pl.id, track.id);
                    } else {
                      addTrackToPlaylist(pl.id, track);
                    }
                  }}
                  className={`flex items-center justify-between p-2.5 rounded-xl border transition-all cursor-pointer ${
                    inPlaylist
                      ? 'bg-[#1DB954]/10 border-[#1DB954]/40 text-white'
                      : 'bg-[#1e1e1e] border-[#282828] text-gray-300 hover:bg-[#282828]'
                  }`}
                  id={`playlist-item-${pl.id}`}
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div className="w-8 h-8 rounded bg-[#282828] flex items-center justify-center shrink-0">
                      <Music className="w-4 h-4 text-gray-400" />
                    </div>
                    <div className="flex flex-col min-w-0">
                      <span className="text-xs font-bold truncate">{pl.name}</span>
                      <span className="text-[10px] text-gray-400">{pl.tracks.length} tracks</span>
                    </div>
                  </div>

                  <div
                    className={`w-5 h-5 rounded-full flex items-center justify-center ${
                      inPlaylist ? 'bg-[#1DB954] text-black' : 'border border-gray-500'
                    }`}
                  >
                    {inPlaylist && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                  </div>
                </div>
              );
            })
          )}
        </div>

        <button
          onClick={() => setIsAddToPlaylistOpen(false, null)}
          className="w-full py-2.5 rounded-full bg-[#282828] hover:bg-[#333] text-xs font-bold text-white transition-colors mt-2"
          id="done-add-to-playlist-btn"
        >
          Done
        </button>
      </div>
    </div>
  );
};

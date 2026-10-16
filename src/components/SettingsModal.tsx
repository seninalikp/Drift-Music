import React from 'react';
import { X, Trash2, Sliders, Keyboard, Info } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';

export const SettingsModal: React.FC = () => {
  const isSettingsOpen = usePlayerStore((s) => s.isSettingsOpen);
  const setIsSettingsOpen = usePlayerStore((s) => s.setIsSettingsOpen);
  const clearLibrary = usePlayerStore((s) => s.clearLibrary);

  if (!isSettingsOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 select-none animate-fadeIn">
      <div className="bg-[#181818] border border-[#282828] rounded-2xl max-w-lg w-full p-6 shadow-2xl flex flex-col gap-5 relative max-h-[90vh] overflow-y-auto custom-scrollbar">
        <div className="flex items-center justify-between pb-3 border-b border-[#282828]">
          <div className="flex items-center gap-2">
            <Sliders className="w-5 h-5 text-[#1DB954]" />
            <h2 className="text-lg font-bold text-white">App Settings</h2>
          </div>
          <button
            onClick={() => setIsSettingsOpen(false)}
            className="p-1.5 rounded-full hover:bg-[#282828] text-gray-400 hover:text-white transition-colors"
            id="settings-close-btn"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-3.5 rounded-xl bg-[#222222] border border-[#303030] flex flex-col gap-2">
          <div className="flex items-center gap-2 text-xs font-bold text-gray-200">
            <Keyboard className="w-4 h-4 text-[#1DB954]" />
            <span>Keyboard Controls</span>
          </div>
          <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-[11px] text-gray-400">
            <div>
              <strong className="text-gray-300">Space:</strong> Play / Pause
            </div>
            <div>
              <strong className="text-gray-300">Shift + ← / →:</strong> Prev / Next
            </div>
            <div>
              <strong className="text-gray-300">← / →:</strong> Seek 5s
            </div>
            <div>
              <strong className="text-gray-300">Esc:</strong> Close Fullscreen
            </div>
          </div>
        </div>

        <div className="flex items-center justify-between p-3.5 rounded-xl bg-red-950/20 border border-red-900/40">
          <div className="flex flex-col">
            <span className="text-xs font-bold text-red-300">Local Data Storage</span>
            <span className="text-[11px] text-gray-400">
              Clear saved songs, playlists, and playback history
            </span>
          </div>
          <button
            onClick={() => {
              if (confirm('Are you sure you want to clear your saved songs and playback history?')) {
                clearLibrary();
                alert('Library data cleared.');
              }
            }}
            className="px-3 py-1.5 rounded-lg bg-red-600/20 hover:bg-red-600/40 text-red-300 text-xs font-bold flex items-center gap-1.5 transition-colors"
            id="clear-storage-btn"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Clear Library</span>
          </button>
        </div>

        <div className="text-center text-[10px] text-gray-400 pt-2 border-t border-[#282828] flex items-center justify-center gap-1">
          <Info className="w-3 h-3 text-gray-400" />
          <span>Drift Music Player</span>
        </div>
      </div>
    </div>
  );
};

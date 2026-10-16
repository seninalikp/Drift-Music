import React, { useState, useEffect } from 'react';
import { Search, Settings, Radio, AlertCircle } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';

export const Header: React.FC = () => {
  const searchQuery = usePlayerStore((s) => s.searchQuery);
  const setSearchQuery = usePlayerStore((s) => s.setSearchQuery);
  const setActiveTab = usePlayerStore((s) => s.setActiveTab);
  const activeTab = usePlayerStore((s) => s.activeTab);
  const setIsSettingsOpen = usePlayerStore((s) => s.setIsSettingsOpen);
  const audioError = usePlayerStore((s) => s.audioError);
  const setAudioError = usePlayerStore((s) => s.setAudioError);

  const [localInput, setLocalInput] = useState(searchQuery);

  useEffect(() => {
    setLocalInput(searchQuery);
  }, [searchQuery]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (localInput !== searchQuery) {
        setSearchQuery(localInput);
        if (localInput.trim().length > 0 && activeTab !== 'search') {
          setActiveTab('search');
        }
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [localInput]);

  return (
    <header className="sticky top-0 z-30 flex flex-col w-full bg-[#121212]/95 backdrop-blur-md border-b border-[#282828] px-4 py-3 md:px-6">
      <div className="flex items-center justify-between gap-3 md:gap-6">
        <div
          onClick={() => setActiveTab('home')}
          className="flex items-center gap-2.5 cursor-pointer select-none group shrink-0"
        >
          <img
            src="/logo.png"
            alt="Drift"
            className="w-9 h-9 rounded-full object-cover shadow-lg shadow-[#38BDF8]/20 group-hover:scale-105 transition-transform"
          />
          <div className="hidden sm:flex flex-col">
            <span className="font-extrabold text-white text-lg tracking-tight leading-none group-hover:text-[#38BDF8] transition-colors">
              Drift<span className="text-[#38BDF8]"> Music</span>
            </span>
            <span className="text-[10px] text-gray-400 font-medium tracking-wider uppercase">
              Streaming Player
            </span>
          </div>
        </div>

        <div className="flex-1 max-w-xl mx-auto">
          <div className="relative flex items-center w-full">
            <Search className="absolute left-3.5 w-4 h-4 text-gray-400 pointer-events-none" />
            <input
              type="text"
              value={localInput}
              onChange={(e) => setLocalInput(e.target.value)}
              onFocus={() => {
                if (activeTab !== 'search') setActiveTab('search');
              }}
              placeholder="What do you want to play? (Artists, tracks, genres...)"
              className="w-full bg-[#242424] hover:bg-[#2a2a2a] focus:bg-[#2a2a2a] text-white text-sm placeholder-gray-400 pl-10 pr-10 py-2.5 rounded-full border border-transparent focus:border-[#1DB954] focus:outline-none transition-all shadow-inner"
              id="global-search-input"
            />
            {localInput && (
              <button
                onClick={() => {
                  setLocalInput('');
                  setSearchQuery('');
                }}
                className="absolute right-3 text-xs text-gray-400 hover:text-white bg-[#333] hover:bg-[#444] rounded-full w-5 h-5 flex items-center justify-center transition-colors"
                title="Clear search"
                id="clear-search-btn"
              >
                ✕
              </button>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <button
            onClick={() => setIsSettingsOpen(true)}
            className="p-2.5 rounded-full text-gray-300 hover:text-white hover:bg-[#282828] transition-colors relative"
            title="Settings"
            aria-label="Open Settings"
            id="settings-icon-btn"
          >
            <Settings className="w-5 h-5" />
          </button>
        </div>
      </div>

      {audioError && (
        <div className="mt-2.5 p-2.5 rounded-lg bg-[#FF5500]/15 border border-[#FF5500]/40 text-xs text-orange-200 flex items-center justify-between gap-3 animate-fadeIn">
          <div className="flex items-center gap-2 min-w-0">
            <AlertCircle className="w-4 h-4 text-[#FF5500] shrink-0" />
            <span className="truncate">{audioError}</span>
          </div>
          <button
            onClick={() => setAudioError(null)}
            className="text-gray-400 hover:text-white px-1.5 py-0.5 text-xs shrink-0"
            id="dismiss-audio-error-btn"
          >
            ✕
          </button>
        </div>
      )}
    </header>
  );
};

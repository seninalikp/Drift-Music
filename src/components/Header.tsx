import React, { useState, useEffect } from 'react';
import { Search, Settings, Radio, Sparkles, User, AlertCircle, Smartphone } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { InstallAppModal } from './InstallAppModal';

export const Header: React.FC = () => {
  const searchQuery = usePlayerStore((s) => s.searchQuery);
  const setSearchQuery = usePlayerStore((s) => s.setSearchQuery);
  const setActiveTab = usePlayerStore((s) => s.setActiveTab);
  const activeTab = usePlayerStore((s) => s.activeTab);
  const apiMode = usePlayerStore((s) => s.apiMode);
  const clientId = usePlayerStore((s) => s.clientId);
  const setIsSettingsOpen = usePlayerStore((s) => s.setIsSettingsOpen);
  const setIsProfileOpen = usePlayerStore((s) => s.setIsProfileOpen);
  const userProfile = usePlayerStore((s) => s.userProfile);
  const loadUserProfile = usePlayerStore((s) => s.loadUserProfile);
  const audioError = usePlayerStore((s) => s.audioError);
  const setAudioError = usePlayerStore((s) => s.setAudioError);

  const [localInput, setLocalInput] = useState(searchQuery);
  const [isInstallModalOpen, setIsInstallModalOpen] = useState(false);

  useEffect(() => {
    loadUserProfile();
  }, []);

  // Sync local input with store
  useEffect(() => {
    setLocalInput(searchQuery);
  }, [searchQuery]);

  // Debounced search trigger
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
        {/* Logo & Brand */}
        <div
          onClick={() => setActiveTab('home')}
          className="flex items-center gap-2.5 cursor-pointer select-none group shrink-0"
        >
          <div className="w-9 h-9 rounded-full bg-gradient-to-tr from-[#1DB954] to-[#FF5500] flex items-center justify-center shadow-lg shadow-[#1DB954]/20 group-hover:scale-105 transition-transform">
            <Radio className="w-5 h-5 text-black font-bold" />
          </div>
          <div className="hidden sm:flex flex-col">
            <span className="font-extrabold text-white text-lg tracking-tight leading-none group-hover:text-[#1DB954] transition-colors">
              Sound<span className="text-[#FF5500]">Stream</span>
            </span>
            <span className="text-[10px] text-gray-400 font-medium tracking-wider uppercase">
              Spotify × SoundCloud
            </span>
          </div>
        </div>

        {/* Search Bar */}
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

        {/* Right Action Controls */}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          {/* Mode Pill Badge */}
          <button
            onClick={() => setIsSettingsOpen(true)}
            className={`hidden md:flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold border transition-all ${
              apiMode === 'live' && clientId
                ? 'bg-[#1DB954]/10 border-[#1DB954]/40 text-[#1DB954] hover:bg-[#1DB954]/20'
                : 'bg-[#FF5500]/10 border-[#FF5500]/40 text-[#FF5500] hover:bg-[#FF5500]/20'
            }`}
            title="Click to configure SoundCloud API settings"
            id="api-mode-badge"
          >
            {apiMode === 'live' && clientId ? (
              <>
                <Radio className="w-3.5 h-3.5 animate-pulse" />
                <span>Live API Mode</span>
              </>
            ) : (
              <>
                <Sparkles className="w-3.5 h-3.5" />
                <span>Demo Mode</span>
              </>
            )}
          </button>

          {/* Install App / Android Button */}
          <button
            onClick={() => setIsInstallModalOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-[#1DB954]/10 hover:bg-[#1DB954]/20 border border-[#1DB954]/40 text-[#1DB954] text-xs font-bold transition-all"
            title="Install App on Android / Device"
            id="header-install-app-btn"
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Install App</span>
          </button>

          {/* Settings Button */}
          <button
            onClick={() => setIsSettingsOpen(true)}
            className="p-2.5 rounded-full text-gray-300 hover:text-white hover:bg-[#282828] transition-colors relative"
            title="Settings & SoundCloud Client ID"
            aria-label="Open Settings"
            id="settings-icon-btn"
          >
            <Settings className="w-5 h-5" />
          </button>

          {/* User Profile */}
          <div
            className="flex items-center gap-2 p-1.5 pl-2 pr-3 rounded-full bg-[#181818] border border-[#282828] hover:border-[#1DB954]/50 transition-colors cursor-pointer group"
            onClick={() => setIsProfileOpen(true)}
            title="Edit Artist Profile & Avatar"
            id="user-profile-btn"
          >
            <div className="w-7 h-7 rounded-full bg-gradient-to-tr from-purple-600 to-indigo-600 flex items-center justify-center text-white text-xs font-bold overflow-hidden shadow">
              {userProfile.avatarUrl ? (
                <img
                  src={userProfile.avatarUrl}
                  alt={userProfile.displayName}
                  className="w-full h-full object-cover"
                />
              ) : (
                <User className="w-4 h-4" />
              )}
            </div>
            <span className="hidden lg:inline text-xs font-bold text-gray-200 group-hover:text-[#1DB954] transition-colors max-w-[120px] truncate">
              {userProfile.displayName || 'Guest User'}
            </span>
          </div>
        </div>
      </div>

      {/* Audio Error Banner if present */}
      {audioError && (
        <div className="mt-2.5 p-2.5 rounded-lg bg-[#FF5500]/15 border border-[#FF5500]/40 text-xs text-orange-200 flex items-center justify-between gap-3 animate-fadeIn">
          <div className="flex items-center gap-2 min-w-0">
            <AlertCircle className="w-4 h-4 text-[#FF5500] shrink-0" />
            <span className="truncate">{audioError}</span>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => setIsSettingsOpen(true)}
              className="px-2.5 py-1 rounded bg-[#FF5500] hover:bg-[#e04b00] text-black font-bold text-[11px] transition-colors"
              id="fix-client-id-btn"
            >
              Fix Client ID
            </button>
            <button
              onClick={() => setAudioError(null)}
              className="text-gray-400 hover:text-white px-1.5 py-0.5 text-xs"
              id="dismiss-audio-error-btn"
            >
              ✕
            </button>
          </div>
        </div>
      )}
      {/* Install App Modal */}
      <InstallAppModal
        isOpen={isInstallModalOpen}
        onClose={() => setIsInstallModalOpen(false)}
      />
    </header>
  );
};

import React from 'react';
import { Home, Search, Library, Settings } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';

export const BottomNav: React.FC = () => {
  const activeTab = usePlayerStore((s) => s.activeTab);
  const setActiveTab = usePlayerStore((s) => s.setActiveTab);
  const setIsSettingsOpen = usePlayerStore((s) => s.setIsSettingsOpen);

  return (
    <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-[#121212]/95 backdrop-blur-lg border-t border-[#282828] px-4 py-2 flex items-center justify-around select-none">
      <button
        onClick={() => setActiveTab('home')}
        className={`flex flex-col items-center gap-1 text-xs font-semibold ${
          activeTab === 'home' ? 'text-[#1DB954]' : 'text-gray-400'
        }`}
        id="mobile-nav-home"
      >
        <Home className="w-5 h-5" />
        <span>Home</span>
      </button>

      <button
        onClick={() => setActiveTab('search')}
        className={`flex flex-col items-center gap-1 text-xs font-semibold ${
          activeTab === 'search' ? 'text-[#1DB954]' : 'text-gray-400'
        }`}
        id="mobile-nav-search"
      >
        <Search className="w-5 h-5" />
        <span>Search</span>
      </button>

      <button
        onClick={() => setActiveTab('library')}
        className={`flex flex-col items-center gap-1 text-xs font-semibold ${
          activeTab === 'library' ? 'text-[#1DB954]' : 'text-gray-400'
        }`}
        id="mobile-nav-library"
      >
        <Library className="w-5 h-5" />
        <span>Library</span>
      </button>

      <button
        onClick={() => setIsSettingsOpen(true)}
        className="flex flex-col items-center gap-1 text-xs font-semibold text-gray-400 hover:text-white"
        id="mobile-nav-settings"
      >
        <Settings className="w-5 h-5" />
        <span>Settings</span>
      </button>
    </div>
  );
};

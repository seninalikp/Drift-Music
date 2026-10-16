import React, { useEffect } from 'react';
import { usePlayerStore } from './store/usePlayerStore';
import { AudioEngine } from './components/AudioEngine';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { BottomNav } from './components/BottomNav';
import { MiniPlayer } from './components/MiniPlayer';
import { FullscreenPlayer } from './components/FullscreenPlayer';
import { SettingsModal } from './components/SettingsModal';
import { QueueDrawer } from './components/QueueDrawer';
import { AddToPlaylistModal } from './components/AddToPlaylistModal';

import { HomeView } from './views/HomeView';
import { SearchView } from './views/SearchView';
import { LibraryView } from './views/LibraryView';

export default function App() {
  const activeTab = usePlayerStore((s) => s.activeTab);
  const currentTrack = usePlayerStore((s) => s.currentTrack);
  const currentTime = usePlayerStore((s) => s.currentTime);
  const duration = usePlayerStore((s) => s.duration);

  const togglePlayPause = usePlayerStore((s) => s.togglePlayPause);
  const seek = usePlayerStore((s) => s.seek);
  const toggleMute = usePlayerStore((s) => s.toggleMute);
  const toggleLikeTrack = usePlayerStore((s) => s.toggleLikeTrack);
  const isFullscreenOpen = usePlayerStore((s) => s.isFullscreenOpen);
  const setIsFullscreenOpen = usePlayerStore((s) => s.setIsFullscreenOpen);
  const nextTrack = usePlayerStore((s) => s.nextTrack);
  const previousTrack = usePlayerStore((s) => s.previousTrack);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      if (
        target &&
        (target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.isContentEditable)
      ) {
        return;
      }

      if (e.code === 'Space') {
        e.preventDefault();
        togglePlayPause();
      } else if (e.key === 'ArrowRight' && !e.shiftKey) {
        e.preventDefault();
        seek(Math.min(duration, currentTime + 5));
      } else if (e.key === 'ArrowLeft' && !e.shiftKey) {
        e.preventDefault();
        seek(Math.max(0, currentTime - 5));
      } else if (e.key === 'ArrowRight' && e.shiftKey) {
        e.preventDefault();
        nextTrack();
      } else if (e.key === 'ArrowLeft' && e.shiftKey) {
        e.preventDefault();
        previousTrack();
      } else if (e.key === 'm' || e.key === 'M') {
        e.preventDefault();
        toggleMute();
      } else if (e.key === 'l' || e.key === 'L') {
        if (currentTrack) {
          e.preventDefault();
          toggleLikeTrack(currentTrack);
        }
      } else if (e.key === 'f' || e.key === 'F') {
        e.preventDefault();
        setIsFullscreenOpen(!isFullscreenOpen);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [
    togglePlayPause,
    currentTime,
    duration,
    seek,
    toggleMute,
    currentTrack,
    toggleLikeTrack,
    isFullscreenOpen,
    setIsFullscreenOpen,
    nextTrack,
    previousTrack,
  ]);

  return (
    <div className="min-h-screen bg-[#121212] text-white flex flex-col font-sans selection:bg-[#1DB954] selection:text-black">
      <AudioEngine />
      <Header />

      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-y-auto custom-scrollbar bg-gradient-to-b from-[#181818] via-[#121212] to-[#121212]">
          {activeTab === 'home' && <HomeView />}
          {activeTab === 'search' && <SearchView />}
          {activeTab === 'library' && <LibraryView />}
        </main>
      </div>

      <MiniPlayer />
      <FullscreenPlayer />
      <QueueDrawer />
      <SettingsModal />
      <AddToPlaylistModal />
      <BottomNav />
    </div>
  );
}

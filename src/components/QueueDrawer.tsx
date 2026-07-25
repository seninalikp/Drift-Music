import React from 'react';
import { X, Play, Music2, Trash2 } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { formatTime } from './TrackCard';

export const QueueDrawer: React.FC = () => {
  const isQueueOpen = usePlayerStore((s) => s.isQueueOpen);
  const setIsQueueOpen = usePlayerStore((s) => s.setIsQueueOpen);
  const queue = usePlayerStore((s) => s.queue);
  const queueIndex = usePlayerStore((s) => s.queueIndex);
  const currentTrack = usePlayerStore((s) => s.currentTrack);
  const playTrack = usePlayerStore((s) => s.playTrack);

  if (!isQueueOpen) return null;

  return (
    <div className="fixed inset-y-0 right-0 z-50 w-full sm:w-96 bg-[#181818]/98 backdrop-blur-2xl border-l border-[#282828] shadow-2xl flex flex-col p-5 select-none animate-slideLeft">
      {/* Drawer Header */}
      <div className="flex items-center justify-between pb-4 border-b border-[#282828]">
        <div className="flex items-center gap-2">
          <Music2 className="w-5 h-5 text-[#1DB954]" />
          <h3 className="text-base font-extrabold text-white">Play Queue</h3>
          <span className="text-xs text-gray-400 font-mono">({queue.length})</span>
        </div>
        <button
          onClick={() => setIsQueueOpen(false)}
          className="p-1.5 rounded-full hover:bg-[#282828] text-gray-400 hover:text-white transition-colors"
          id="queue-close-btn"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {/* Now Playing Section */}
      {currentTrack && (
        <div className="py-4 border-b border-[#282828] flex flex-col gap-2">
          <span className="text-[11px] font-bold text-[#1DB954] uppercase tracking-wider">
            Now Playing
          </span>
          <div className="flex items-center gap-3 p-2 rounded-xl bg-[#242424] border border-[#1DB954]/30">
            <img
              src={currentTrack.artworkUrl}
              alt={currentTrack.title}
              className="w-12 h-12 rounded-lg object-cover shrink-0"
            />
            <div className="flex flex-col min-w-0 flex-1">
              <span className="text-sm font-bold text-white truncate">{currentTrack.title}</span>
              <span className="text-xs text-gray-400 truncate">{currentTrack.artist}</span>
            </div>
            <span className="text-xs font-mono text-gray-400 pr-1">
              {formatTime(currentTrack.duration)}
            </span>
          </div>
        </div>
      )}

      {/* Up Next List */}
      <div className="flex-1 overflow-y-auto py-3 custom-scrollbar flex flex-col gap-2">
        <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider px-1">
          Next Up
        </span>

        {queue.length === 0 ? (
          <p className="text-xs text-gray-400 p-3 italic">Queue is empty.</p>
        ) : (
          queue.map((track, idx) => {
            const isCurrent = currentTrack?.id === track.id;
            return (
              <div
                key={`queue-${track.id}-${idx}`}
                onClick={() => playTrack(track, queue)}
                className={`group flex items-center gap-3 p-2.5 rounded-xl cursor-pointer transition-all ${
                  isCurrent
                    ? 'bg-[#1DB954]/10 border border-[#1DB954]/40 text-[#1DB954]'
                    : 'bg-[#202020] hover:bg-[#282828] text-white'
                }`}
              >
                <div className="relative w-10 h-10 rounded-lg overflow-hidden shrink-0 bg-[#282828]">
                  <img
                    src={track.artworkUrl}
                    alt={track.title}
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                    <Play className="w-4 h-4 fill-white text-white" />
                  </div>
                </div>

                <div className="flex flex-col min-w-0 flex-1">
                  <span
                    className={`text-xs font-bold truncate ${
                      isCurrent ? 'text-[#1DB954]' : 'text-gray-200 group-hover:text-white'
                    }`}
                  >
                    {track.title}
                  </span>
                  <span className="text-[11px] text-gray-400 truncate">{track.artist}</span>
                </div>

                <span className="text-[11px] font-mono text-gray-400">
                  {formatTime(track.duration)}
                </span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

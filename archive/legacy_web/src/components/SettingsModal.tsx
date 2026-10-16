import React, { useState } from 'react';
import {
  X,
  Radio,
  Sparkles,
  Key,
  CheckCircle2,
  Trash2,
  Eye,
  EyeOff,
  ShieldAlert,
  Sliders,
  Keyboard,
  Info,
  Smartphone,
  Download,
} from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { testSoundCloudClientId } from '../lib/soundcloud';

export const SettingsModal: React.FC = () => {
  const isSettingsOpen = usePlayerStore((s) => s.isSettingsOpen);
  const setIsSettingsOpen = usePlayerStore((s) => s.setIsSettingsOpen);

  const clientId = usePlayerStore((s) => s.clientId);
  const setClientId = usePlayerStore((s) => s.setClientId);
  const apiMode = usePlayerStore((s) => s.apiMode);
  const setApiMode = usePlayerStore((s) => s.setApiMode);
  const clearLibrary = usePlayerStore((s) => s.clearLibrary);

  const [inputVal, setInputVal] = useState(clientId);
  const [showMasked, setShowMasked] = useState(true);
  const [isTesting, setIsTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ valid?: boolean; message?: string } | null>(null);

  if (!isSettingsOpen) return null;

  const handleSaveClientId = async () => {
    const clean = inputVal.trim();
    if (!clean) {
      setClientId('');
      setApiMode('demo');
      setTestResult({ valid: false, message: 'Client ID cleared. Switched to Demo Mode.' });
      return;
    }

    setIsTesting(true);
    setTestResult(null);

    const res = await testSoundCloudClientId(clean);
    setIsTesting(false);
    setTestResult(res);

    if (res.valid) {
      setClientId(clean);
      setApiMode('live');
    }
  };

  const maskString = (str: string) => {
    if (!str || str.length < 8) return '••••••••';
    return `${str.slice(0, 4)}••••••••${str.slice(-4)}`;
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 select-none animate-fadeIn">
      <div className="bg-[#181818] border border-[#282828] rounded-2xl max-w-lg w-full p-6 shadow-2xl flex flex-col gap-5 relative max-h-[90vh] overflow-y-auto custom-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#282828]">
          <div className="flex items-center gap-2">
            <Sliders className="w-5 h-5 text-[#1DB954]" />
            <h2 className="text-lg font-bold text-white">App & API Settings</h2>
          </div>
          <button
            onClick={() => setIsSettingsOpen(false)}
            className="p-1.5 rounded-full hover:bg-[#282828] text-gray-400 hover:text-white transition-colors"
            id="settings-close-btn"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Data Mode Switcher */}
        <div className="flex flex-col gap-2">
          <label className="text-xs font-bold text-gray-300 uppercase tracking-wider">
            Audio Data Mode
          </label>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => setApiMode('demo')}
              className={`flex items-center gap-2.5 p-3 rounded-xl border transition-all text-left ${
                apiMode === 'demo'
                  ? 'bg-[#1DB954]/10 border-[#1DB954] text-white'
                  : 'bg-[#222] border-[#303030] text-gray-400 hover:text-white'
              }`}
              id="mode-demo-btn"
            >
              <Sparkles className="w-4 h-4 text-[#1DB954] shrink-0" />
              <div className="flex flex-col">
                <span className="text-xs font-bold">Demo Mode</span>
                <span className="text-[10px] text-gray-400">Royalty-free streamable audio</span>
              </div>
            </button>

            <button
              onClick={() => setApiMode('live')}
              className={`flex items-center gap-2.5 p-3 rounded-xl border transition-all text-left ${
                apiMode === 'live'
                  ? 'bg-[#FF5500]/10 border-[#FF5500] text-white'
                  : 'bg-[#222] border-[#303030] text-gray-400 hover:text-white'
              }`}
              id="mode-live-btn"
            >
              <Radio className="w-4 h-4 text-[#FF5500] shrink-0" />
              <div className="flex flex-col">
                <span className="text-xs font-bold">SoundCloud API</span>
                <span className="text-[10px] text-gray-400">Live search & streams</span>
              </div>
            </button>
          </div>
        </div>

        {/* SoundCloud Client ID Settings */}
        <div className="flex flex-col gap-3 p-4 rounded-xl bg-[#222222] border border-[#303030]">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm font-bold text-white">
              <Key className="w-4 h-4 text-[#FF5500]" />
              <span>SoundCloud Client ID</span>
            </div>
            {clientId && (
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                Configured
              </span>
            )}
          </div>

          <div className="relative flex items-center">
            <input
              type={showMasked ? 'password' : 'text'}
              value={inputVal}
              onChange={(e) => setInputVal(e.target.value)}
              placeholder="Enter SoundCloud Client ID..."
              className="w-full bg-[#181818] text-white text-xs px-3 py-2.5 pr-10 rounded-lg border border-[#383838] focus:border-[#FF5500] focus:outline-none"
              id="settings-client-id-input"
            />
            <button
              onClick={() => setShowMasked(!showMasked)}
              className="absolute right-2 text-gray-400 hover:text-white p-1"
              title={showMasked ? 'Show Client ID' : 'Mask Client ID'}
              id="toggle-mask-btn"
            >
              {showMasked ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
            </button>
          </div>

          <div className="flex items-center justify-between gap-2">
            <button
              onClick={handleSaveClientId}
              disabled={isTesting}
              className="px-4 py-2 rounded-lg bg-[#FF5500] hover:bg-[#e04b00] disabled:bg-gray-600 text-black font-bold text-xs transition-colors"
              id="save-client-id-btn"
            >
              {isTesting ? 'Verifying...' : 'Save & Verify'}
            </button>

            {clientId && (
              <button
                onClick={() => {
                  setClientId('');
                  setInputVal('');
                  setTestResult({ valid: false, message: 'Client ID removed.' });
                }}
                className="text-xs text-red-400 hover:text-red-300 font-semibold"
                id="clear-client-id-btn"
              >
                Clear Client ID
              </button>
            )}
          </div>

          {testResult && (
            <div
              className={`p-2.5 rounded-lg text-xs flex items-center gap-2 ${
                testResult.valid
                  ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/30'
                  : 'bg-red-500/10 text-red-300 border border-red-500/30'
              }`}
            >
              {testResult.valid ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
              ) : (
                <ShieldAlert className="w-4 h-4 text-red-400 shrink-0" />
              )}
              <span>{testResult.message}</span>
            </div>
          )}
        </div>

        {/* Android App Install Guide Card */}
        <div className="p-3.5 rounded-xl bg-[#1DB954]/10 border border-[#1DB954]/30 flex flex-col gap-2">
          <div className="flex items-center gap-2 text-xs font-bold text-[#1DB954]">
            <Smartphone className="w-4 h-4" />
            <span>Install App on Android (PWA)</span>
          </div>
          <p className="text-[11px] text-gray-300 leading-snug">
            To install SoundStream on your Android home screen: open this link in Chrome on Android, tap the 3 dots menu <strong className="text-white">⋮</strong> and select <strong className="text-white font-bold">"Add to Home screen"</strong> or <strong className="text-white font-bold">"Install App"</strong>.
          </p>
        </div>

        {/* Keyboard Shortcuts Reference */}
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

        {/* Storage Management */}
        <div className="flex items-center justify-between p-3.5 rounded-xl bg-red-950/20 border border-red-900/40">
          <div className="flex flex-col">
            <span className="text-xs font-bold text-red-300">Local Data Storage</span>
            <span className="text-[11px] text-gray-400">
              Clear saved songs, search history, and offline state
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

        {/* Footer info */}
        <div className="text-center text-[10px] text-gray-400 pt-2 border-t border-[#282828] flex items-center justify-center gap-1">
          <Info className="w-3 h-3 text-gray-400" />
          <span>SoundStream v1.0 • Built with Next/React, Zustand, and HTML5 Audio</span>
        </div>
      </div>
    </div>
  );
};

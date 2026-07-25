import React, { useState } from 'react';
import { Radio, Sparkles, CheckCircle2, AlertTriangle, Key, ArrowRight, ShieldAlert } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { testSoundCloudClientId } from '../lib/soundcloud';

export const ClientIdModal: React.FC = () => {
  const hasSeenClientIdModal = usePlayerStore((s) => s.hasSeenClientIdModal);
  const setHasSeenClientIdModal = usePlayerStore((s) => s.setHasSeenClientIdModal);
  const clientId = usePlayerStore((s) => s.clientId);
  const setClientId = usePlayerStore((s) => s.setClientId);
  const setApiMode = usePlayerStore((s) => s.setApiMode);

  const [inputVal, setInputVal] = useState(clientId);
  const [isTesting, setIsTesting] = useState(false);
  const [testResult, setTestResult] = useState<{ valid?: boolean; message?: string } | null>(null);

  if (hasSeenClientIdModal) return null;

  const handleStartDemo = () => {
    setApiMode('demo');
    setHasSeenClientIdModal(true);
  };

  const handleTestAndConnect = async () => {
    if (!inputVal.trim()) {
      setTestResult({ valid: false, message: 'Please enter a Client ID.' });
      return;
    }

    setIsTesting(true);
    setTestResult(null);

    const res = await testSoundCloudClientId(inputVal.trim());
    setIsTesting(false);
    setTestResult(res);

    if (res.valid) {
      setClientId(inputVal.trim());
      setApiMode('live');
      setTimeout(() => {
        setHasSeenClientIdModal(true);
      }, 1000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-xl flex items-center justify-center p-4 select-none animate-fadeIn">
      <div className="bg-[#181818] border border-[#282828] rounded-2xl max-w-lg w-full p-6 sm:p-8 shadow-2xl flex flex-col gap-6 relative overflow-hidden">
        {/* Top Gradient Bar */}
        <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-[#1DB954] via-amber-500 to-[#FF5500]" />

        {/* Header */}
        <div className="flex flex-col gap-2 text-center items-center">
          <div className="w-12 h-12 rounded-2xl bg-[#FF5500]/10 border border-[#FF5500]/30 flex items-center justify-center text-[#FF5500]">
            <Radio className="w-6 h-6" />
          </div>
          <h2 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight">
            Welcome to SoundStream
          </h2>
          <p className="text-xs sm:text-sm text-gray-400 max-w-md">
            Spotify-style web client integrated with SoundCloud streaming and local persistence.
          </p>
        </div>

        {/* SoundCloud API Constraint Warning Callout */}
        <div className="p-3.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-xs text-amber-200 flex gap-3 items-start">
          <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />
          <div className="flex flex-col gap-1">
            <strong className="text-amber-300 font-bold">SoundCloud API Notice</strong>
            <p className="leading-relaxed">
              SoundCloud’s public track API (`api.soundcloud.com`) requires a valid Client ID for live stream URLs.
              You can connect your own Client ID or start immediately with Demo Mode.
            </p>
          </div>
        </div>

        {/* Options */}
        <div className="flex flex-col gap-4">
          {/* Option A: Demo Mode */}
          <button
            onClick={handleStartDemo}
            className="group flex items-center justify-between p-4 rounded-xl bg-[#222222] hover:bg-[#282828] border border-[#303030] hover:border-[#1DB954]/50 transition-all text-left"
            id="start-demo-mode-btn"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[#1DB954]/10 text-[#1DB954] flex items-center justify-center shrink-0">
                <Sparkles className="w-5 h-5" />
              </div>
              <div className="flex flex-col">
                <span className="text-sm font-bold text-white group-hover:text-[#1DB954] transition-colors">
                  Start in Demo Mode (Recommended)
                </span>
                <span className="text-xs text-gray-400">
                  Pre-loaded with high-quality streamable tracks across top genres. No API key required.
                </span>
              </div>
            </div>
            <ArrowRight className="w-5 h-5 text-gray-400 group-hover:text-[#1DB954] group-hover:translate-x-1 transition-all shrink-0 ml-2" />
          </button>

          {/* Option B: Enter Client ID */}
          <div className="flex flex-col gap-3 p-4 rounded-xl bg-[#222222] border border-[#303030]">
            <div className="flex items-center gap-2 text-sm font-bold text-white">
              <Key className="w-4 h-4 text-[#FF5500]" />
              <span>Connect SoundCloud Client ID</span>
            </div>
            <p className="text-xs text-gray-400">
              Enter your scraped or registered SoundCloud Client ID to fetch live SoundCloud search tracks.
            </p>

            <div className="flex gap-2">
              <input
                type="text"
                value={inputVal}
                onChange={(e) => setInputVal(e.target.value)}
                placeholder="e.g. 2t9UnvsW2R4g..."
                className="flex-1 bg-[#181818] text-white text-xs px-3 py-2 rounded-lg border border-[#383838] focus:border-[#FF5500] focus:outline-none"
                id="modal-client-id-input"
              />
              <button
                onClick={handleTestAndConnect}
                disabled={isTesting}
                className="px-4 py-2 rounded-lg bg-[#FF5500] hover:bg-[#e04b00] disabled:bg-gray-600 text-black font-bold text-xs transition-colors shrink-0"
                id="modal-test-connect-btn"
              >
                {isTesting ? 'Testing...' : 'Connect ID'}
              </button>
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
        </div>

        {/* Footer */}
        <div className="text-center text-[11px] text-gray-400">
          You can change your Client ID or switch back to Demo Mode anytime in Settings.
        </div>
      </div>
    </div>
  );
};

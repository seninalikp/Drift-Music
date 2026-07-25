import React, { useState, useEffect } from 'react';
import { X, Smartphone, Download, CheckCircle2, Share, PlusSquare, ArrowRight, ExternalLink } from 'lucide-react';

interface InstallAppModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const InstallAppModal: React.FC<InstallAppModalProps> = ({ isOpen, onClose }) => {
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [isInstalled, setIsInstalled] = useState(false);

  useEffect(() => {
    const handleBeforeInstallPrompt = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
    };

    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);

    if (window.matchMedia('(display-mode: standalone)').matches) {
      setIsInstalled(true);
    }

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    };
  }, []);

  if (!isOpen) return null;

  const handleNativeInstall = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      if (outcome === 'accepted') {
        setIsInstalled(true);
      }
      setDeferredPrompt(null);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 select-none animate-fadeIn">
      <div className="bg-[#181818] border border-[#282828] rounded-2xl max-w-md w-full p-6 shadow-2xl flex flex-col gap-5 relative max-h-[90vh] overflow-y-auto custom-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#282828]">
          <div className="flex items-center gap-2">
            <Smartphone className="w-5 h-5 text-[#1DB954]" />
            <h2 className="text-lg font-bold text-white">Install SoundStream on Android</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-[#282828] text-gray-400 hover:text-white transition-colors"
            id="install-modal-close-btn"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Status Banner */}
        {isInstalled ? (
          <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 flex items-center gap-3">
            <CheckCircle2 className="w-6 h-6 text-emerald-400 shrink-0" />
            <div className="flex flex-col">
              <span className="text-sm font-bold">App Already Installed!</span>
              <span className="text-xs text-emerald-200/80">
                SoundStream is installed on your device home screen.
              </span>
            </div>
          </div>
        ) : (
          /* Native PWA Prompt Button if available */
          deferredPrompt && (
            <button
              onClick={handleNativeInstall}
              className="w-full py-3 rounded-xl bg-[#1DB954] hover:bg-[#1ed760] text-black font-extrabold text-sm flex items-center justify-center gap-2 shadow-xl transition-transform hover:scale-[1.02]"
              id="pwa-native-install-btn"
            >
              <Download className="w-5 h-5" />
              <span>Tap to Install App on Android</span>
            </button>
          )
        )}

        {/* Step-by-Step Android Installation Guide */}
        <div className="flex flex-col gap-3">
          <span className="text-xs font-bold text-gray-300 uppercase tracking-wider">
            How to install on Android (Chrome Browser)
          </span>

          <div className="flex flex-col gap-2 text-xs text-gray-300">
            <div className="flex items-start gap-3 p-3 rounded-xl bg-[#222] border border-[#2d2d2d]">
              <div className="w-6 h-6 rounded-full bg-[#1DB954]/20 border border-[#1DB954]/40 text-[#1DB954] font-bold flex items-center justify-center shrink-0">
                1
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-bold text-white">Open in Chrome on your Android phone</span>
                <span className="text-[11px] text-gray-400">
                  Open the app link in Google Chrome or your default Android browser.
                </span>
              </div>
            </div>

            <div className="flex items-start gap-3 p-3 rounded-xl bg-[#222] border border-[#2d2d2d]">
              <div className="w-6 h-6 rounded-full bg-[#1DB954]/20 border border-[#1DB954]/40 text-[#1DB954] font-bold flex items-center justify-center shrink-0">
                2
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-bold text-white flex items-center gap-1.5">
                  Tap the 3 dots menu <span className="font-mono text-[#1DB954] text-base">⋮</span>
                </span>
                <span className="text-[11px] text-gray-400">
                  Tap the top-right menu button in Chrome browser.
                </span>
              </div>
            </div>

            <div className="flex items-start gap-3 p-3 rounded-xl bg-[#222] border border-[#2d2d2d]">
              <div className="w-6 h-6 rounded-full bg-[#1DB954]/20 border border-[#1DB954]/40 text-[#1DB954] font-bold flex items-center justify-center shrink-0">
                3
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-bold text-white flex items-center gap-1.5">
                  Select "Add to Home screen" or "Install App"
                </span>
                <span className="text-[11px] text-gray-400">
                  This will place a full-screen SoundStream app icon directly on your Android home screen!
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Source Code & APK Export Guide */}
        <div className="p-4 rounded-xl bg-gradient-to-r from-purple-900/30 to-indigo-900/30 border border-purple-500/30 flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-purple-300">Looking to export source code / ZIP?</span>
          </div>
          <p className="text-[11px] text-gray-300 leading-relaxed">
            In AI Studio top toolbar, click the top <strong>Settings / Project menu</strong> (top right or header bar) and select <strong>"Download ZIP"</strong> or <strong>"Export to GitHub"</strong>.
          </p>
        </div>

        <button
          onClick={onClose}
          className="w-full py-2.5 rounded-full bg-[#282828] hover:bg-[#333] text-white text-xs font-bold transition-colors"
          id="close-install-modal-btn"
        >
          Got it
        </button>
      </div>
    </div>
  );
};

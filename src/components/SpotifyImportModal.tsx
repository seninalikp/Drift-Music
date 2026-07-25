import React, { useState, useEffect } from 'react';
import {
  X,
  Radio,
  Sparkles,
  Loader2,
  CheckCircle2,
  ListMusic,
  ExternalLink,
  ShieldCheck,
  AlertCircle,
  HelpCircle,
  ArrowRight,
} from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import {
  getSpotifyAuthUrl,
  exchangeSpotifyCode,
  fetchSpotifyUserPlaylists,
  fetchSpotifyLikedSongs,
  matchSpotifyTrackToSoundCloud,
  SAMPLE_SPOTIFY_PLAYLISTS,
  SpotifyPlaylistMeta,
} from '../lib/spotify';
import { Track } from '../types';

interface SpotifyImportModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SpotifyImportModal: React.FC<SpotifyImportModalProps> = ({ isOpen, onClose }) => {
  const clientId = usePlayerStore((s) => s.clientId);
  const apiMode = usePlayerStore((s) => s.apiMode);
  const createPlaylist = usePlayerStore((s) => s.createPlaylist);
  const addTrackToPlaylist = usePlayerStore((s) => s.addTrackToPlaylist);

  const [spotifyClientId, setSpotifyClientId] = useState('');
  const [redirectUri, setRedirectUri] = useState('');
  const [step, setStep] = useState<'config' | 'importing' | 'complete'>('config');
  const [progressMsg, setProgressMsg] = useState('');
  const [progressPercent, setProgressPercent] = useState(0);

  const [importedCount, setImportedCount] = useState(0);
  const [matchedCount, setMatchedCount] = useState(0);
  const [unmatchedCount, setUnmatchedCount] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const currentUrl = window.location.origin + window.location.pathname;
      setRedirectUri(currentUrl);

      // Check if redirected back with ?code= from Spotify
      const urlParams = new URLSearchParams(window.location.search);
      const code = urlParams.get('code');
      const savedSpClientId = localStorage.getItem('spotify_app_client_id_v1');

      if (code && savedSpClientId) {
        handleOAuthReturn(code, savedSpClientId, currentUrl);
      }
    }
  }, []);

  if (!isOpen) return null;

  const handleOAuthReturn = async (code: string, spClientId: string, uri: string) => {
    try {
      setStep('importing');
      setProgressMsg('Exchanging authorization code for Spotify token...');
      setProgressPercent(10);

      // Clean query string from browser bar without reloading page
      window.history.replaceState({}, document.title, window.location.pathname);

      const accessToken = await exchangeSpotifyCode(code, spClientId, uri);

      setProgressMsg('Fetching Spotify Saved Songs and Playlists...');
      setProgressPercent(30);

      const spPlaylists = await fetchSpotifyUserPlaylists(accessToken);
      const spLiked = await fetchSpotifyLikedSongs(accessToken);

      if (spLiked.length > 0) {
        spPlaylists.unshift({
          id: 'sp-liked-tracks',
          name: 'Spotify Saved Tracks',
          description: 'Imported Liked Songs from Spotify',
          tracks: spLiked,
        });
      }

      await processAndImportPlaylists(spPlaylists);
    } catch (err: any) {
      setError(err.message || 'Spotify PKCE authorization failed.');
      setStep('config');
    }
  };

  const handleStartSpotifyPKCE = async () => {
    if (!spotifyClientId.trim()) {
      setError('Please enter a valid Spotify App Client ID.');
      return;
    }
    try {
      setError(null);
      localStorage.setItem('spotify_app_client_id_v1', spotifyClientId.trim());
      const authUrl = await getSpotifyAuthUrl(spotifyClientId.trim(), redirectUri);
      window.location.href = authUrl;
    } catch (err: any) {
      setError(err.message || 'Failed to initialize Spotify PKCE auth URL.');
    }
  };

  const handleInstantDemoImport = async () => {
    setStep('importing');
    setError(null);
    await processAndImportPlaylists(SAMPLE_SPOTIFY_PLAYLISTS);
  };

  const processAndImportPlaylists = async (playlistsData: SpotifyPlaylistMeta[]) => {
    let totalTracks = 0;
    let matched = 0;
    let unmatched = 0;

    const totalToProcess = playlistsData.reduce((acc, p) => acc + p.tracks.length, 0);
    let processedCount = 0;

    for (const plMeta of playlistsData) {
      setProgressMsg(`Matching tracks for playlist "${plMeta.name}" with SoundCloud...`);

      const matchedTracks: Track[] = [];

      for (const spTrk of plMeta.tracks) {
        processedCount++;
        setProgressPercent(Math.round(30 + (processedCount / (totalToProcess || 1)) * 65));

        const matchedTrack = await matchSpotifyTrackToSoundCloud(spTrk, clientId, apiMode);
        matchedTracks.push(matchedTrack);

        if (matchedTrack.isUnmatched) {
          unmatched++;
        } else {
          matched++;
        }
        totalTracks++;
      }

      // Create playlist in Zustand store
      const newPl = createPlaylist(
        plMeta.name,
        plMeta.description || 'Imported Spotify Playlist',
        plMeta.coverArt
      );

      for (const trk of matchedTracks) {
        addTrackToPlaylist(newPl.id, trk);
      }
    }

    setImportedCount(totalTracks);
    setMatchedCount(matched);
    setUnmatchedCount(unmatched);
    setProgressPercent(100);
    setStep('complete');
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 select-none animate-fadeIn">
      <div className="bg-[#181818] border border-[#282828] rounded-2xl max-w-lg w-full p-6 shadow-2xl flex flex-col gap-6 relative max-h-[90vh] overflow-y-auto custom-scrollbar">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#282828]">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-full bg-[#1DB954] flex items-center justify-center text-black font-bold text-xs">
              S
            </div>
            <h2 className="text-lg font-bold text-white">Import Spotify Metadata</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-[#282828] text-gray-400 hover:text-white transition-colors"
            id="spotify-import-close-btn"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-xs text-red-300 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {step === 'config' && (
          <div className="flex flex-col gap-5">
            {/* Info notice about Spotify Policy & Option 1 */}
            <div className="p-4 rounded-xl bg-[#222] border border-[#303030] flex flex-col gap-2">
              <div className="flex items-center gap-2 text-xs font-bold text-[#1DB954]">
                <ShieldCheck className="w-4 h-4" />
                <span>Option 1: Import Metadata & Auto-Match SoundCloud Audio</span>
              </div>
              <p className="text-xs text-gray-300 leading-relaxed">
                Spotify does not permit streaming raw audio outside Spotify apps.
                SoundStream imports your Spotify playlists & Liked Songs, then automatically searches SoundCloud to match streamable audio tracks for in-browser playback!
              </p>
            </div>

            {/* Option A: OAuth PKCE Flow with User's Spotify Client ID */}
            <div className="flex flex-col gap-3 p-4 rounded-xl bg-[#202020] border border-[#2a2a2a]">
              <span className="text-xs font-bold text-white">1. Connect via Spotify PKCE OAuth</span>
              <p className="text-[11px] text-gray-400">
                Requires a Spotify Developer App Client ID with redirect URI set to:{' '}
                <code className="text-[#1DB954] bg-black/40 px-1 py-0.5 rounded text-[10px] break-all">
                  {redirectUri}
                </code>
              </p>

              <div className="flex items-center gap-2">
                <input
                  type="text"
                  value={spotifyClientId}
                  onChange={(e) => setSpotifyClientId(e.target.value)}
                  placeholder="Enter Spotify Client ID..."
                  className="flex-1 bg-[#181818] text-white text-xs px-3 py-2 rounded-lg border border-[#383838] focus:border-[#1DB954] focus:outline-none"
                  id="spotify-client-id-input"
                />
                <button
                  onClick={handleStartSpotifyPKCE}
                  className="px-4 py-2 rounded-lg bg-[#1DB954] hover:bg-[#1ed760] text-black font-extrabold text-xs transition-colors shrink-0 flex items-center gap-1.5"
                  id="spotify-connect-pkce-btn"
                >
                  <span>Connect</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            {/* Option B: 1-Click Instant Demo Import */}
            <div className="flex flex-col gap-2 p-4 rounded-xl bg-gradient-to-r from-purple-900/30 to-indigo-900/30 border border-purple-500/30">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-purple-400" />
                  <span className="text-xs font-bold text-white">2. Instant 1-Click Demo Import</span>
                </div>
                <span className="text-[10px] bg-purple-500/20 text-purple-300 px-2 py-0.5 rounded font-mono">
                  Recommended
                </span>
              </div>
              <p className="text-[11px] text-gray-300">
                Instantly import sample Spotify playlists without registering a Spotify developer app or redirect URI!
              </p>
              <button
                onClick={handleInstantDemoImport}
                className="mt-1 w-full py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-extrabold text-xs transition-all shadow-lg flex items-center justify-center gap-2"
                id="instant-demo-import-btn"
              >
                <Sparkles className="w-4 h-4" />
                <span>Import Sample Spotify Metadata</span>
              </button>
            </div>
          </div>
        )}

        {step === 'importing' && (
          <div className="flex flex-col items-center justify-center py-10 gap-5 text-center">
            <Loader2 className="w-12 h-12 text-[#1DB954] animate-spin" />
            <div className="flex flex-col gap-1 max-w-sm">
              <h3 className="text-sm font-bold text-white">{progressMsg}</h3>
              <p className="text-xs text-gray-400">Auto-matching track names with SoundCloud audio streams...</p>
            </div>

            {/* Progress Bar */}
            <div className="w-full bg-[#282828] h-2.5 rounded-full overflow-hidden max-w-xs">
              <div
                className="bg-[#1DB954] h-full transition-all duration-300"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
            <span className="text-xs font-mono text-[#1DB954]">{progressPercent}%</span>
          </div>
        )}

        {step === 'complete' && (
          <div className="flex flex-col items-center justify-center py-6 gap-5 text-center">
            <div className="w-14 h-14 rounded-full bg-[#1DB954]/20 border border-[#1DB954]/40 flex items-center justify-center text-[#1DB954]">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <div className="flex flex-col gap-1">
              <h3 className="text-lg font-extrabold text-white">Import Complete!</h3>
              <p className="text-xs text-gray-300">
                Imported <strong className="text-white">{importedCount} tracks</strong> into your SoundStream local library.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-3 w-full max-w-xs text-xs font-medium">
              <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex flex-col items-center">
                <span className="text-lg font-black text-emerald-400">{matchedCount}</span>
                <span className="text-[10px] text-gray-400">Matched SoundCloud</span>
              </div>
              <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 flex flex-col items-center">
                <span className="text-lg font-black text-amber-400">{unmatchedCount}</span>
                <span className="text-[10px] text-gray-400">Flagged Unmatched</span>
              </div>
            </div>

            <button
              onClick={onClose}
              className="w-full py-3 rounded-full bg-[#1DB954] hover:bg-[#1ed760] text-black font-extrabold text-xs transition-colors shadow-lg mt-2"
              id="view-imported-library-btn"
            >
              View in Your Library
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

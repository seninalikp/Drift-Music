/**
 * Spotify PKCE Authorization Code Flow & Track Matching Engine
 * Enables importing Spotify playlists and saved songs, matching them to streamable SoundCloud tracks.
 */

import { Track, Playlist } from '../types';
import { searchSoundCloudTracks, filterMockTracks } from './soundcloud';

export interface SpotifyTrackMeta {
  id: string;
  name: string;
  artists: string[];
  albumName?: string;
  albumArt?: string;
  durationMs?: number;
}

export interface SpotifyPlaylistMeta {
  id: string;
  name: string;
  description?: string;
  coverArt?: string;
  tracks: SpotifyTrackMeta[];
}

/**
 * Generate PKCE Random Verifier
 */
export function generateCodeVerifier(length = 64): string {
  const possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
  const values = new Uint8Array(length);
  window.crypto.getRandomValues(values);
  return Array.from(values)
    .map((x) => possible[x % possible.length])
    .join('');
}

/**
 * Generate SHA-256 Code Challenge from Verifier
 */
export async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await window.crypto.subtle.digest('SHA-256', data);
  const base64Digest = btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return base64Digest;
}

/**
 * Build Spotify Authorization URL for PKCE
 */
export async function getSpotifyAuthUrl(clientId: string, redirectUri: string): Promise<string> {
  const verifier = generateCodeVerifier();
  localStorage.setItem('spotify_pkce_verifier', verifier);

  const challenge = await generateCodeChallenge(verifier);
  const scope = 'user-library-read playlist-read-private playlist-read-collaborative';

  const params = new URLSearchParams({
    client_id: clientId.trim(),
    response_type: 'code',
    redirect_uri: redirectUri,
    code_challenge_method: 'S256',
    code_challenge: challenge,
    scope: scope,
  });

  return `https://accounts.spotify.com/authorize?${params.toString()}`;
}

/**
 * Exchange Authorization Code for Spotify Access Token
 */
export async function exchangeSpotifyCode(
  code: string,
  clientId: string,
  redirectUri: string
): Promise<string> {
  const verifier = localStorage.getItem('spotify_pkce_verifier') || '';
  if (!verifier) {
    throw new Error('PKCE verifier missing. Please restart Spotify login.');
  }

  const body = new URLSearchParams({
    client_id: clientId.trim(),
    grant_type: 'authorization_code',
    code: code,
    redirect_uri: redirectUri,
    code_verifier: verifier,
  });

  const response = await fetch('https://accounts.spotify.com/api/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  });

  if (!response.ok) {
    const errData = await response.json().catch(() => ({}));
    throw new Error(errData.error_description || `Failed to exchange Spotify token (HTTP ${response.status})`);
  }

  const data = await response.json();
  return data.access_token as string;
}

/**
 * Fetch Spotify Saved Tracks ("Liked Songs")
 */
export async function fetchSpotifyLikedSongs(accessToken: string, limit = 50): Promise<SpotifyTrackMeta[]> {
  const res = await fetch(`https://api.spotify.com/v1/me/tracks?limit=${limit}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch Spotify Liked Songs (HTTP ${res.status})`);
  }

  const data = await res.json();
  const items = data.items || [];

  return items.map((item: any) => ({
    id: item.track?.id || String(Math.random()),
    name: item.track?.name || 'Unknown Track',
    artists: item.track?.artists?.map((a: any) => a.name) || ['Unknown Artist'],
    albumName: item.track?.album?.name,
    albumArt: item.track?.album?.images?.[0]?.url,
    durationMs: item.track?.duration_ms,
  }));
}

/**
 * Fetch User's Spotify Playlists
 */
export async function fetchSpotifyUserPlaylists(accessToken: string): Promise<SpotifyPlaylistMeta[]> {
  const res = await fetch('https://api.spotify.com/v1/me/playlists?limit=20', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch Spotify Playlists (HTTP ${res.status})`);
  }

  const data = await res.json();
  const playlistsData = data.items || [];

  const playlists: SpotifyPlaylistMeta[] = [];

  for (const pl of playlistsData) {
    try {
      const tracksRes = await fetch(`https://api.spotify.com/v1/playlists/${pl.id}/tracks?limit=50`, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });

      if (tracksRes.ok) {
        const tracksData = await tracksRes.json();
        const rawTracks = tracksData.items || [];
        const tracks: SpotifyTrackMeta[] = rawTracks
          .filter((t: any) => t.track)
          .map((t: any) => ({
            id: t.track.id || String(Math.random()),
            name: t.track.name || 'Unknown Track',
            artists: t.track.artists?.map((a: any) => a.name) || ['Unknown Artist'],
            albumName: t.track.album?.name,
            albumArt: t.track.album?.images?.[0]?.url,
            durationMs: t.track.duration_ms,
          }));

        playlists.push({
          id: pl.id,
          name: pl.name,
          description: pl.description,
          coverArt: pl.images?.[0]?.url,
          tracks,
        });
      }
    } catch {
      // Continue to next playlist
    }
  }

  return playlists;
}

/**
 * Match a single Spotify track metadata entry with a streamable SoundCloud track
 */
export async function matchSpotifyTrackToSoundCloud(
  spTrack: SpotifyTrackMeta,
  scClientId: string,
  apiMode: 'demo' | 'live'
): Promise<Track> {
  const primaryArtist = spTrack.artists[0] || '';
  const searchString = `${primaryArtist} ${spTrack.name}`.trim();

  let matchedTrack: Track | null = null;

  if (apiMode === 'live' && scClientId && scClientId.trim()) {
    const scResult = await searchSoundCloudTracks(searchString, scClientId, 3);
    if (scResult.tracks && scResult.tracks.length > 0) {
      // Take the top streamable search result
      const topMatch = scResult.tracks.find((t) => t.isStreamable) || scResult.tracks[0];
      matchedTrack = {
        ...topMatch,
        title: topMatch.title,
        artist: topMatch.artist || primaryArtist,
        spotifyOriginalMeta: {
          spotifyId: spTrack.id,
          originalTitle: spTrack.name,
          originalArtist: spTrack.artists.join(', '),
        },
      } as Track;
    }
  }

  // Fallback / Demo mode match
  if (!matchedTrack) {
    const demoMatches = filterMockTracks(spTrack.name);
    if (demoMatches.length > 0) {
      matchedTrack = {
        ...demoMatches[0],
        title: `${spTrack.name} (Matched)`,
        artist: primaryArtist || demoMatches[0].artist,
        spotifyOriginalMeta: {
          spotifyId: spTrack.id,
          originalTitle: spTrack.name,
          originalArtist: spTrack.artists.join(', '),
        },
      } as Track;
    }
  }

  // If still no match found, create a flagged non-matched track entry
  if (!matchedTrack) {
    const fallbackAudio =
      'https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=chill-lofi-song-8444.mp3';

    matchedTrack = {
      id: `sp-unmatched-${spTrack.id}`,
      title: spTrack.name,
      artist: spTrack.artists.join(', '),
      artworkUrl:
        spTrack.albumArt ||
        'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80',
      streamUrl: fallbackAudio, // streamable preview fallback
      duration: Math.round((spTrack.durationMs || 180000) / 1000),
      genre: 'Spotify Import',
      isStreamable: true,
      isDemo: true,
      isUnmatched: true, // FLAGGED PER-TRACK
      spotifyOriginalMeta: {
        spotifyId: spTrack.id,
        originalTitle: spTrack.name,
        originalArtist: spTrack.artists.join(', '),
      },
    } as Track;
  }

  return matchedTrack;
}

/**
 * Sample Demo Spotify Metadata for Instant 1-Click Testing without credentials
 */
export const SAMPLE_SPOTIFY_PLAYLISTS: SpotifyPlaylistMeta[] = [
  {
    id: 'sp-demo-pl-1',
    name: 'Spotify Top Electronic Hits',
    description: 'Imported from Spotify • Auto-matched with SoundCloud stream URLs',
    coverArt: 'https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80',
    tracks: [
      {
        id: 'sp-tr-1',
        name: 'Strobe',
        artists: ['deadmau5'],
        albumName: 'For Lack of a Better Name',
        albumArt: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80',
        durationMs: 360000,
      },
      {
        id: 'sp-tr-2',
        name: 'Midnight City',
        artists: ['M83'],
        albumName: 'Hurry Up, We\'re Dreaming',
        albumArt: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80',
        durationMs: 243000,
      },
      {
        id: 'sp-tr-3',
        name: 'Get Lucky',
        artists: ['Daft Punk', 'Pharrell Williams'],
        albumName: 'Random Access Memories',
        albumArt: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=600&q=80',
        durationMs: 248000,
      },
      {
        id: 'sp-tr-4',
        name: 'Resonance',
        artists: ['HOME'],
        albumName: 'Odyssey',
        albumArt: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80',
        durationMs: 212000,
      },
    ],
  },
];

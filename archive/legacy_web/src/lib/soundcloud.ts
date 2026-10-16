import { Track } from '../types';
import { MOCK_TRACKS } from '../data/mockTracks';

export interface SoundCloudTrackResponse {
  id: number;
  title: string;
  user: {
    username: string;
    permalink_url: string;
    avatar_url: string;
  };
  artwork_url: string | null;
  stream_url?: string;
  streamable?: boolean;
  duration: number; // in milliseconds
  genre?: string;
  permalink_url: string;
  playback_count?: number;
  likes_count?: number;
  created_at?: string;
  description?: string;
}

/**
 * Format SoundCloud artwork URL to higher resolution (t500x500 instead of large)
 */
export function formatArtworkUrl(url: string | null | undefined): string {
  if (!url) {
    return 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80';
  }
  return url.replace('-large', '-t500x500');
}

/**
 * Map raw SoundCloud API track object to App Track type
 */
export function transformSoundCloudTrack(
  scTrack: SoundCloudTrackResponse,
  clientId: string
): Track {
  // SoundCloud track stream URL needs client_id parameter attached
  let streamUrl = scTrack.stream_url
    ? `${scTrack.stream_url}?client_id=${clientId}`
    : `https://api.soundcloud.com/tracks/${scTrack.id}/stream?client_id=${clientId}`;

  return {
    id: `sc-${scTrack.id}`,
    title: scTrack.title,
    artist: scTrack.user?.username || 'Unknown Artist',
    artistUrl: scTrack.user?.permalink_url,
    artworkUrl: formatArtworkUrl(scTrack.artwork_url || scTrack.user?.avatar_url),
    streamUrl,
    duration: Math.round((scTrack.duration || 180000) / 1000), // convert ms to s
    genre: scTrack.genre || 'Electronic',
    permalinkUrl: scTrack.permalink_url,
    playbackCount: scTrack.playback_count || 0,
    likesCount: scTrack.likes_count || 0,
    createdAt: scTrack.created_at,
    description: scTrack.description,
    isStreamable: Boolean(scTrack.streamable !== false),
    isDemo: false,
  };
}

/**
 * Search tracks using SoundCloud Public API
 */
export async function searchSoundCloudTracks(
  query: string,
  clientId: string,
  limit: number = 20
): Promise<{ tracks: Track[]; error?: string }> {
  if (!clientId || !clientId.trim()) {
    return {
      tracks: filterMockTracks(query),
      error: 'No SoundCloud Client ID provided. Returning high-quality demo tracks.',
    };
  }

  try {
    const encodedQuery = encodeURIComponent(query);
    const url = `https://api.soundcloud.com/tracks?q=${encodedQuery}&client_id=${clientId.trim()}&limit=${limit}`;

    const res = await fetch(url);

    if (!res.ok) {
      if (res.status === 401 || res.status === 403) {
        throw new Error(
          `Invalid or expired SoundCloud Client ID (HTTP ${res.status}). Please verify your Client ID in Settings.`
        );
      }
      throw new Error(`SoundCloud API request failed with status HTTP ${res.status}`);
    }

    const data: SoundCloudTrackResponse[] = await res.json();
    if (!Array.isArray(data)) {
      throw new Error('Unexpected API response format from SoundCloud.');
    }

    const tracks = data.map((item) => transformSoundCloudTrack(item, clientId));
    return { tracks };
  } catch (err: unknown) {
    const errorMessage =
      err instanceof Error ? err.message : 'Failed to connect to SoundCloud API.';
    
    // Return filtered fallback tracks plus actionable error
    return {
      tracks: filterMockTracks(query),
      error: `${errorMessage} Showing fallback demo results.`,
    };
  }
}

/**
 * Helper to filter local demo tracks by query or genre
 */
export function filterMockTracks(query: string, genre?: string): Track[] {
  let list = [...MOCK_TRACKS];
  if (genre && genre !== 'All Genres') {
    list = list.filter((t) => t.genre?.toLowerCase() === genre.toLowerCase());
  }
  if (!query || !query.trim()) {
    return list;
  }
  const q = query.toLowerCase().trim();
  return list.filter(
    (t) =>
      t.title.toLowerCase().includes(q) ||
      t.artist.toLowerCase().includes(q) ||
      (t.genre && t.genre.toLowerCase().includes(q))
  );
}

/**
 * Verify Client ID validity against SoundCloud API
 */
export async function testSoundCloudClientId(clientId: string): Promise<{
  valid: boolean;
  message: string;
}> {
  const cleanId = clientId.trim();
  if (!cleanId) {
    return { valid: false, message: 'Client ID cannot be blank.' };
  }

  try {
    const url = `https://api.soundcloud.com/tracks?limit=1&client_id=${cleanId}`;
    const res = await fetch(url);

    if (res.status === 200) {
      return { valid: true, message: 'Client ID is valid and connected!' };
    } else if (res.status === 401 || res.status === 403) {
      return {
        valid: false,
        message: 'Invalid or restricted Client ID (HTTP 401/403).',
      };
    } else {
      return {
        valid: false,
        message: `SoundCloud responded with HTTP status ${res.status}`,
      };
    }
  } catch (err: unknown) {
    return {
      valid: false,
      message: 'Network or CORS error connecting to api.soundcloud.com.',
    };
  }
}

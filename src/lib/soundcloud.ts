import { Track } from '../types';
import { MOCK_TRACKS } from '../data/mockTracks';

/**
 * Helper to filter local tracks by query or genre
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

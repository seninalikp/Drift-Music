import React, { useState, useEffect } from 'react';
import { Disc3 } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';

interface ArtworkImageProps {
  src?: string;
  title: string;
  artist: string;
  trackId?: string | number;
  className?: string;
  alt?: string;
}

const artworkCache = new Map<string, string>();

const FALLBACK_GRADIENT_COVER =
  'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80';

/**
 * Smart Artwork Component that fixes broken/missing Spotify or stream thumbnails
 * by automatically querying iTunes/Apple Music high-res 600x600 album art.
 */
export const ArtworkImage: React.FC<ArtworkImageProps> = ({
  src,
  title,
  artist,
  trackId,
  className = 'w-full h-full object-cover',
  alt,
}) => {
  const updateTrackArtwork = usePlayerStore((s) => s.updateTrackArtwork);
  const [resolvedSrc, setResolvedSrc] = useState<string>(src || '');
  const [hasFailed, setHasFailed] = useState(false);
  const [isFetchingFallback, setIsFetchingFallback] = useState(false);

  const cacheKey = `${artist}-${title}`.toLowerCase().trim();

  useEffect(() => {
    setHasFailed(false);
    const cleanSrc = src ? src.replace(/^http:\/\//i, 'https://').trim() : '';

    // Check if the URL is missing or a generic placeholder that needs real album art
    const isGenericPlaceholder =
      !cleanSrc ||
      cleanSrc.includes('photo-1511671782779-c97d3d27a1d4');

    if (!isGenericPlaceholder) {
      setResolvedSrc(cleanSrc);
    } else if (artworkCache.has(cacheKey)) {
      const cached = artworkCache.get(cacheKey)!;
      setResolvedSrc(cached);
      if (trackId) updateTrackArtwork(trackId, cached);
    } else {
      resolveRealAlbumArt();
    }
  }, [src, title, artist]);

  const resolveRealAlbumArt = async () => {
    if (isFetchingFallback) return;
    if (artworkCache.has(cacheKey)) {
      const cached = artworkCache.get(cacheKey)!;
      setResolvedSrc(cached);
      if (trackId) updateTrackArtwork(trackId, cached);
      return;
    }

    setIsFetchingFallback(true);
    try {
      const cleanTitle = title.replace(/\(.*?\)|\[.*?\]/g, '').trim();
      const cleanArtist = artist.split(',')[0].trim();
      const query = encodeURIComponent(`${cleanArtist}${cleanTitle}`);
      const res = await fetch(`https://itunes.apple.com/search?term=${query}&entity=song&limit=1`);
      if (res.ok) {
        const data = await res.json();
        if (data.results && data.results.length > 0 && data.results[0].artworkUrl100) {
          const hiRes = data.results[0].artworkUrl100.replace('100x100bb', '600x600bb');
          artworkCache.set(cacheKey, hiRes);
          setResolvedSrc(hiRes);
          setHasFailed(false);
          if (trackId) updateTrackArtwork(trackId, hiRes);
          setIsFetchingFallback(false);
          return;
        }
      }
    } catch {
      // Ignore network error and use fallback cover
    }

    setIsFetchingFallback(false);
    setResolvedSrc(src || FALLBACK_GRADIENT_COVER);
  };

  const handleImageError = () => {
    if (!hasFailed) {
      setHasFailed(true);
      resolveRealAlbumArt();
    } else {
      setResolvedSrc(FALLBACK_GRADIENT_COVER);
    }
  };

  if (!resolvedSrc) {
    return (
      <div className={`bg-gradient-to-br from-zinc-800 to-zinc-950 flex items-center justify-center ${className}`}>
        <Disc3 className="w-1/3 h-1/3 text-white/20 animate-spin" />
      </div>
    );
  }

  return (
    <img
      src={resolvedSrc}
      alt={alt || title}
      className={className}
      referrerPolicy="no-referrer"
       crossOrigin="anonymous"
      loading="lazy"
      onError={handleImageError}
    />
  );
};

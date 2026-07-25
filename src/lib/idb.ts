/**
 * IndexedDB helper for storing profile image and custom assets
 * Avoids localStorage 5MB quota limit and handles WebView edge cases.
 */

const DB_NAME = 'soundstream_assets_db_v1';
const STORE_NAME = 'user_assets';
const DB_VERSION = 1;

let dbPromise: Promise<IDBDatabase> | null = null;

function getDB(): Promise<IDBDatabase> {
  if (dbPromise) return dbPromise;

  dbPromise = new Promise((resolve, reject) => {
    if (typeof window === 'undefined' || !('indexedDB' in window)) {
      reject(new Error('IndexedDB not supported in this environment'));
      return;
    }

    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME);
      }
    };

    request.onsuccess = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      resolve(db);
    };

    request.onerror = () => {
      reject(request.error || new Error('Failed to open IndexedDB'));
    };
  });

  return dbPromise;
}

/**
 * Save an asset (base64 or string data) by key
 */
export async function setAsset(key: string, value: string): Promise<boolean> {
  try {
    const db = await getDB();
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const req = store.put(value, key);
      req.onsuccess = () => resolve(true);
      req.onerror = () => {
        // Fall back to localStorage if IDB fails
        try {
          localStorage.setItem(`idb_fallback_${key}`, value);
        } catch {
          // ignore
        }
        resolve(false);
      };
    });
  } catch (e) {
    try {
      localStorage.setItem(`idb_fallback_${key}`, value);
    } catch {
      // ignore
    }
    return false;
  }
}

/**
 * Get an asset by key
 */
export async function getAsset(key: string): Promise<string | null> {
  try {
    const db = await getDB();
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const req = store.get(key);
      req.onsuccess = () => {
        if (req.result) {
          resolve(req.result as string);
        } else {
          // Check fallback
          const fallback = localStorage.getItem(`idb_fallback_${key}`);
          resolve(fallback);
        }
      };
      req.onerror = () => {
        const fallback = localStorage.getItem(`idb_fallback_${key}`);
        resolve(fallback);
      };
    });
  } catch (e) {
    const fallback = localStorage.getItem(`idb_fallback_${key}`);
    return fallback;
  }
}

/**
 * Delete an asset by key
 */
export async function deleteAsset(key: string): Promise<boolean> {
  try {
    const db = await getDB();
    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const req = store.delete(key);
      req.onsuccess = () => resolve(true);
      req.onerror = () => resolve(false);
    });
  } catch {
    localStorage.removeItem(`idb_fallback_${key}`);
    return false;
  }
}

/**
 * Utility to process user photo upload and scale down using HTML5 Canvas
 * Returns compressed base64 JPEG data URL suitable for IndexedDB.
 */
export function compressAndProcessImage(file: File, maxDim = 400): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > maxDim) {
            height = Math.round((height * maxDim) / width);
            width = maxDim;
          }
        } else {
          if (height > maxDim) {
            width = Math.round((width * maxDim) / height);
            height = maxDim;
          }
        }

        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(e.target?.result as string);
          return;
        }

        ctx.drawImage(img, 0, 0, width, height);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
        resolve(dataUrl);
      };
      img.onerror = () => reject(new Error('Failed to parse image file'));
      img.src = e.target?.result as string;
    };
    reader.onerror = () => reject(new Error('Failed to read image file'));
    reader.readAsDataURL(file);
  });
}

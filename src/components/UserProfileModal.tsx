import React, { useState, useRef, useEffect } from 'react';
import { X, User, Camera, Check, Sparkles, Shield, Trash2 } from 'lucide-react';
import { usePlayerStore } from '../store/usePlayerStore';
import { compressAndProcessImage } from '../lib/idb';

export const UserProfileModal: React.FC = () => {
  const isProfileOpen = usePlayerStore((s) => s.isProfileOpen);
  const setIsProfileOpen = usePlayerStore((s) => s.setIsProfileOpen);
  const userProfile = usePlayerStore((s) => s.userProfile);
  const updateUserProfile = usePlayerStore((s) => s.updateUserProfile);

  const [name, setName] = useState(userProfile.displayName);
  const [bio, setBio] = useState(userProfile.bio || '');
  const [avatarPreview, setAvatarPreview] = useState(userProfile.avatarUrl);
  const [isProcessing, setIsProcessing] = useState(false);
  const [savedSuccess, setSavedSuccess] = useState(false);

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setName(userProfile.displayName);
    setBio(userProfile.bio || '');
    setAvatarPreview(userProfile.avatarUrl);
  }, [userProfile, isProfileOpen]);

  if (!isProfileOpen) return null;

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setIsProcessing(true);
      const compressedDataUrl = await compressAndProcessImage(file, 400);
      setAvatarPreview(compressedDataUrl);
    } catch (err) {
      alert('Failed to process uploaded image. Please try another photo.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleSave = async () => {
    setIsProcessing(true);
    await updateUserProfile({
      displayName: name.trim() || 'SoundStream Producer',
      bio: bio.trim(),
      avatarUrl: avatarPreview,
    });
    setIsProcessing(false);
    setSavedSuccess(true);
    setTimeout(() => {
      setSavedSuccess(false);
      setIsProfileOpen(false);
    }, 800);
  };

  const handleRemoveAvatar = async () => {
    setAvatarPreview('');
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 select-none animate-fadeIn">
      <div className="bg-[#181818] border border-[#282828] rounded-2xl max-w-md w-full p-6 shadow-2xl flex flex-col gap-6 relative">
        {/* Modal Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#282828]">
          <div className="flex items-center gap-2">
            <User className="w-5 h-5 text-[#1DB954]" />
            <h2 className="text-lg font-bold text-white">Your Artist Profile</h2>
          </div>
          <button
            onClick={() => setIsProfileOpen(false)}
            className="p-1.5 rounded-full hover:bg-[#282828] text-gray-400 hover:text-white transition-colors"
            id="profile-close-btn"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Profile Avatar Upload Section */}
        <div className="flex flex-col items-center gap-3">
          <div className="relative group">
            <div className="w-28 h-28 rounded-full overflow-hidden bg-gradient-to-tr from-purple-600 via-indigo-600 to-[#1DB954] flex items-center justify-center border-4 border-[#282828] shadow-xl">
              {avatarPreview ? (
                <img
                  src={avatarPreview}
                  alt={name}
                  className="w-full h-full object-cover"
                />
              ) : (
                <User className="w-12 h-12 text-white" />
              )}
            </div>

            {/* Photo Picker Overlay Button */}
            <button
              onClick={() => fileInputRef.current?.click()}
              className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 rounded-full flex flex-col items-center justify-center text-white transition-opacity gap-1 text-xs font-bold"
              id="upload-avatar-btn"
            >
              <Camera className="w-6 h-6 text-[#1DB954]" />
              <span>Change</span>
            </button>
          </div>

          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept="image/*"
            className="hidden"
            id="avatar-file-input"
          />

          <div className="flex items-center gap-2">
            <button
              onClick={() => fileInputRef.current?.click()}
              className="px-3 py-1.5 rounded-full bg-[#282828] hover:bg-[#333] text-xs font-semibold text-gray-200 transition-colors flex items-center gap-1.5"
              id="choose-photo-btn"
            >
              <Camera className="w-3.5 h-3.5 text-[#1DB954]" />
              <span>Choose Photo</span>
            </button>

            {avatarPreview && (
              <button
                onClick={handleRemoveAvatar}
                className="p-1.5 rounded-full bg-red-500/10 hover:bg-red-500/20 text-red-400 transition-colors"
                title="Remove avatar photo"
                id="remove-avatar-btn"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
          <span className="text-[10px] text-gray-400">
            Stored in local IndexedDB (zero server upload)
          </span>
        </div>

        {/* Inputs */}
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-gray-300 uppercase tracking-wider">
              Display Name
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Your producer or DJ name"
              className="w-full bg-[#222] text-white text-sm px-3.5 py-2.5 rounded-xl border border-[#303030] focus:border-[#1DB954] focus:outline-none transition-colors"
              id="profile-name-input"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold text-gray-300 uppercase tracking-wider">
              Bio / Artist Statement
            </label>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              rows={3}
              placeholder="Tell listeners what music you love..."
              className="w-full bg-[#222] text-white text-sm px-3.5 py-2.5 rounded-xl border border-[#303030] focus:border-[#1DB954] focus:outline-none transition-colors resize-none"
              id="profile-bio-input"
            />
          </div>
        </div>

        {/* Storage Notice */}
        <div className="p-3 rounded-xl bg-[#222] border border-[#303030] flex items-center gap-2 text-xs text-gray-400">
          <Shield className="w-4 h-4 text-[#1DB954] shrink-0" />
          <span>Your profile data is kept 100% private in client IndexedDB storage.</span>
        </div>

        {/* Action Controls */}
        <div className="flex items-center justify-end gap-3 pt-2 border-t border-[#282828]">
          <button
            onClick={() => setIsProfileOpen(false)}
            className="px-4 py-2 rounded-full text-xs font-bold text-gray-400 hover:text-white transition-colors"
            id="cancel-profile-btn"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={isProcessing}
            className="px-5 py-2.5 rounded-full bg-[#1DB954] hover:bg-[#1ed760] disabled:bg-gray-600 text-black font-extrabold text-xs transition-colors flex items-center gap-2 shadow-lg"
            id="save-profile-btn"
          >
            {savedSuccess ? (
              <>
                <Check className="w-4 h-4" /> Saved!
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4" /> Save Profile
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

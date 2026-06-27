import { useState, useEffect } from 'react';
import './AvatarOrb.css';

export type AvatarState = 'idle' | 'listening' | 'thinking' | 'speaking';

interface AvatarOrbProps {
  state: AvatarState;
  onClick?: () => void;
}

export function AvatarOrb({ state, onClick }: AvatarOrbProps) {
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    setIsAnimating(state === 'thinking' || state === 'speaking');
  }, [state]);

  return (
    <div
      className={`avatar-orb avatar-${state}`}
      onClick={onClick}
      role="button"
      tabIndex={0}
      aria-label={`Avatar - ${state}`}
    >
      {/* Main orb - TRON image */}
      <div className={`orb-inner ${isAnimating ? 'pulse' : ''}`}>
        {/* Use the exact TRON image */}
        <img src="/tron.png" alt="TRON Avatar" className="tron-image" />
      </div>

      {/* Outer rotating ring */}
      <div className={`orb-ring ${isAnimating ? 'spin' : ''}`} />

      {/* Listening dots */}
      {state === 'listening' && (
        <div className="listening-dots">
          <span className="dot dot-1" />
          <span className="dot dot-2" />
          <span className="dot dot-3" />
        </div>
      )}

      {/* Speaking waveform */}
      {state === 'speaking' && (
        <div className="waveform">
          <div className="wave wave-1" />
          <div className="wave wave-2" />
          <div className="wave wave-3" />
        </div>
      )}

      {/* Glow effect */}
      <div className={`orb-glow ${isAnimating ? 'active' : ''}`} />
    </div>
  );
}

// Voice + Avatar integration for Chat
import { AvatarState } from './AvatarOrb';

export type SpeechState = 'idle' | 'recording' | 'processing' | 'transcribing';

/**
 * Maps voice chat state + speech state to avatar animation state.
 * - Listening: user is recording audio
 * - Thinking: transcribing or waiting for AI response
 * - Speaking: AI is responding (note: not triggered here, handled separately)
 * - Idle: everything quiet
 */
export function getChatAvatarState(
  isStreaming: boolean,
  speechState: SpeechState,
): AvatarState {
  // AI is responding to user
  if (isStreaming) {
    return 'thinking'; // Show pulsing orb while generating
  }
  
  // User is speaking
  if (speechState === 'recording') {
    return 'listening'; // Show bouncing dots
  }
  
  // Transcribing or processing speech
  if (speechState === 'processing' || speechState === 'transcribing') {
    return 'thinking'; // Show pulsing orb + rotating ring
  }
  
  // Everything is quiet
  return 'idle'; // Show neutral blue glow
}

/**
 * Optional: Determine if we should show TTS speaker icon.
 * Call after assistant message is rendered.
 */
export function shouldShowSpeaker(isStreaming: boolean, lastMessageRole?: string): boolean {
  return !isStreaming && lastMessageRole === 'assistant';
}

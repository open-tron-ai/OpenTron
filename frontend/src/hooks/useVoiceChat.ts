import { useState, useRef, useCallback } from 'react';

export interface VoiceState {
  isListening: boolean;
  isTranscribing: boolean;
  transcript: string;
  error: string | null;
}

export interface VoiceActions {
  startListening: () => Promise<void>;
  stopListening: () => Promise<void>;
  speakText: (text: string) => Promise<void>;
  reset: () => void;
}

export function useVoiceChat(): [VoiceState, VoiceActions] {
  const [state, setState] = useState<VoiceState>({
    isListening: false,
    isTranscribing: false,
    transcript: '',
    error: null,
  });

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const chunksRef = useRef<Blob[]>([]);

  const startListening = useCallback(async () => {
    try {
      setState((s) => ({ ...s, error: null, isListening: true, transcript: '' }));
      chunksRef.current = [];

      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });

      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) {
          chunksRef.current.push(e.data);
        }
      };

      mediaRecorder.start();
      mediaRecorderRef.current = mediaRecorder;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to access microphone';
      setState((s) => ({ ...s, error: message, isListening: false }));
    }
  }, []);

  const stopListening = useCallback(async () => {
    return new Promise<void>((resolve) => {
      if (!mediaRecorderRef.current) {
        resolve();
        return;
      }

      setState((s) => ({ ...s, isListening: false, isTranscribing: true }));

      mediaRecorderRef.current.onstop = async () => {
        try {
          const audioBlob = new Blob(chunksRef.current, { type: 'audio/webm' });

          // In production, call your backend speech/transcribe endpoint
          // For now, just return a mock transcript
          const mockTranscript = 'Hello, how can you help me?';

          setState((s) => ({
            ...s,
            transcript: mockTranscript,
            isTranscribing: false,
          }));

          // Stop the stream
          mediaRecorderRef.current?.stream.getTracks().forEach((track) => track.stop());

          resolve();
        } catch (err) {
          const message = err instanceof Error ? err.message : 'Transcription failed';
          setState((s) => ({ ...s, error: message, isTranscribing: false }));
          resolve();
        }
      };

      mediaRecorderRef.current.stop();
    });
  }, []);

  const speakText = useCallback(async (text: string) => {
    try {
      // Use browser's native SpeechSynthesis API
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = 1;
      utterance.pitch = 1;
      utterance.volume = 1;

      window.speechSynthesis.cancel();
      window.speechSynthesis.speak(utterance);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Speech synthesis failed';
      setState((s) => ({ ...s, error: message }));
    }
  }, []);

  const reset = useCallback(() => {
    setState({
      isListening: false,
      isTranscribing: false,
      transcript: '',
      error: null,
    });
    chunksRef.current = [];
    window.speechSynthesis.cancel();
  }, []);

  return [state, { startListening, stopListening, speakText, reset }];
}

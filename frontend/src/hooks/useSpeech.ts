import { useState, useCallback, useRef, useEffect } from 'react';
import { transcribeAudio, fetchSpeechHealth } from '../lib/api';

export type SpeechState = 'idle' | 'recording' | 'transcribing' | 'processing';

export function useSpeech() {
  const [state, setState] = useState<SpeechState>('idle');
  const [error, setError] = useState<string | null>(null);
  const [available, setAvailable] = useState(false);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const streamRef = useRef<MediaStream | null>(null);

  useEffect(() => {
    fetchSpeechHealth()
      .then((health) => {
        console.log('[useSpeech] Health check:', health);
        setAvailable(health.available ?? false);
      })
      .catch((err) => {
        console.warn('[useSpeech] Health check failed:', err);
        setAvailable(false);
      });
  }, []);

  const startRecording = useCallback(async (): Promise<void> => {
    setError(null);

    if (!navigator.mediaDevices?.getUserMedia) {
      const msg = 'Microphone not supported in this browser';
      setError(msg);
      console.error('[useSpeech]', msg);
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      streamRef.current = stream;

      const recorder = new MediaRecorder(stream);
      chunksRef.current = [];

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data);
      };

      recorder.start();
      mediaRecorderRef.current = recorder;
      setState('recording');
      console.log('[useSpeech] Recording started');
    } catch (err) {
      const msg = 'Microphone access denied';
      setError(msg);
      setState('idle');
      console.error('[useSpeech]', msg, err);
    }
  }, []);

  const stopRecording = useCallback(async (): Promise<string> => {
    return new Promise((resolve, reject) => {
      const recorder = mediaRecorderRef.current;
      if (!recorder || recorder.state !== 'recording') {
        const msg = 'Not recording';
        console.warn('[useSpeech]', msg);
        reject(new Error(msg));
        return;
      }

      recorder.onstop = async () => {
        setState('transcribing');
        console.log('[useSpeech] Recording stopped, transcribing...');

        streamRef.current?.getTracks().forEach((track) => track.stop());
        streamRef.current = null;

        const blob = new Blob(chunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        chunksRef.current = [];

        try {
          let result;
          
          if (available) {
            console.log('[useSpeech] Using backend transcription');
            result = await transcribeAudio(blob);
          } else {
            console.log('[useSpeech] Backend unavailable, using mock transcription');
            result = {
              text: 'Hello world',
              language: 'en',
              confidence: 0.95,
              duration_seconds: blob.size / 16000,
            };
          }

          setState('idle');
          console.log('[useSpeech] Transcription result:', result.text);
          resolve(result.text);
        } catch (err) {
          setState('idle');
          const msg = err instanceof Error ? err.message : 'Transcription failed';
          setError(msg);
          console.error('[useSpeech] Transcription error:', msg);
          
          console.log('[useSpeech] Falling back to mock due to error');
          resolve('Hello world');
        }
      };

      recorder.stop();
    });
  }, [available]);

  return {
    state,
    error,
    available,
    startRecording,
    stopRecording,
    isRecording: state === 'recording',
    isTranscribing: state === 'transcribing',
  };
}

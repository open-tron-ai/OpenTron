package io.opentron.cli;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Cross-platform voice activation system.
 * Handles text-to-speech (TTS) and speech-to-text (STT).
 * Supports Windows, macOS, and Linux.
 */
public class VoiceActivation {
    private String os;
    private boolean ttsAvailable = false;
    private boolean sttAvailable = false;

    public VoiceActivation() {
        this.os = System.getProperty("os.name").toLowerCase();
        initializeVoiceSystem();
    }

    /**
     * Initialize voice system for current platform.
     */
    private void initializeVoiceSystem() {
        if (os.contains("mac")) {
            ttsAvailable = true;  // macOS has native 'say' command
            sttAvailable = true;  // macOS has dictation
        } else if (os.contains("win")) {
            ttsAvailable = true;  // Windows has PowerShell TTS
            sttAvailable = true;  // Windows has Speech Recognition
        } else if (os.contains("linux")) {
            ttsAvailable = commandExists("espeak");
            sttAvailable = commandExists("speech-recognition");
        }
    }

    /**
     * Check if a command exists on the system.
     */
    private boolean commandExists(String command) {
        try {
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("where", command);
            } else {
                pb = new ProcessBuilder("which", command);
            }
            Process process = pb.start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Listen for native voice input asynchronously.
     */
    public CompletableFuture<String> listenNative() {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        if (!sttAvailable) {
            future.completeExceptionally(
                new UnsupportedOperationException("Voice recognition not available on " + os)
            );
            return future;
        }
        
        new Thread(() -> {
            try {
                String result = getVoiceInput();
                future.complete(result != null ? result : "");
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).start();
        
        return future;
    }

    /**
     * Get voice input from platform-specific STT.
     */
    private String getVoiceInput() throws Exception {
        if (os.contains("mac")) {
            return getMacVoiceInput();
        } else if (os.contains("win")) {
            return getWindowsVoiceInput();
        } else if (os.contains("linux")) {
            return getLinuxVoiceInput();
        }
        throw new UnsupportedOperationException("Voice recognition not supported on " + os);
    }

    /**
     * macOS voice input using native dictation.
     */
    private String getMacVoiceInput() throws Exception {
        // Using script to capture voice input
        String script = "set the_result to \"\"\n" +
                       "try\n" +
                       "    set the_result to (listen)\n" +
                       "end try\n" +
                       "return the_result";
        
        ProcessBuilder pb = new ProcessBuilder("osascript", "-e", script);
        Process process = pb.start();
        
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(process.getInputStream())
        );
        String result = reader.readLine();
        process.waitFor();
        
        return result != null ? result.trim() : null;
    }

    /**
     * Windows voice input using Speech Recognition.
     */
    private String getWindowsVoiceInput() throws Exception {
        String psScript = "@echo off\n" +
            "PowerShell -NoProfile -Command \"\n" +
            "Add-Type -AssemblyName System.Speech;\n" +
            "$speech = New-Object System.Speech.Recognition.SpeechRecognitionEngine;\n" +
            "$speech.SetInputToDefaultAudioDevice();\n" +
            "$grammar = new-object System.Speech.Recognition.DictationGrammar;\n" +
            "$speech.LoadGrammar($grammar);\n" +
            "$result = $speech.Recognize();\n" +
            "if ($result -ne $null) {\n" +
            "  Write-Host $result.Text\n" +
            "}\n" +
            "\"";
        
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", psScript);
        Process process = pb.start();
        
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(process.getInputStream())
        );
        String result = reader.readLine();
        process.waitFor();
        
        return result != null ? result.trim() : null;
    }

    /**
     * Linux voice input using speech-recognition module.
     */
    private String getLinuxVoiceInput() throws Exception {
        String pythonScript = "#!/usr/bin/env python3\n" +
            "try:\n" +
            "    import speech_recognition as sr\n" +
            "    recognizer = sr.Recognizer()\n" +
            "    with sr.Microphone() as source:\n" +
            "        audio = recognizer.listen(source, timeout=30)\n" +
            "        try:\n" +
            "            text = recognizer.recognize_google(audio)\n" +
            "            print(text)\n" +
            "        except:\n" +
            "            pass\n" +
            "except:\n" +
            "    pass";
        
        ProcessBuilder pb = new ProcessBuilder("python3", "-c", pythonScript);
        Process process = pb.start();
        
        java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(process.getInputStream())
        );
        String result = reader.readLine();
        process.waitFor(30, TimeUnit.SECONDS);
        
        return result != null ? result.trim() : null;
    }

    /**
     * Text-to-speech output.
     */
    public void speak(String text) throws Exception {
        if (!ttsAvailable) {
            throw new UnsupportedOperationException("Text-to-speech not available on " + os);
        }
        
        if (os.contains("mac")) {
            speakMac(text);
        } else if (os.contains("win")) {
            speakWindows(text);
        } else if (os.contains("linux")) {
            speakLinux(text);
        }
    }

    /**
     * macOS text-to-speech using 'say' command.
     */
    private void speakMac(String text) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("say", text);
        Process process = pb.start();
        process.waitFor();
    }

    /**
     * Windows text-to-speech using PowerShell.
     */
    private void speakWindows(String text) throws Exception {
        String escaped = text.replace("\"", "\\\"");
        String psScript = String.format(
            "Add-Type -AssemblyName System.Speech; " +
            "$synthesizer = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
            "$synthesizer.Speak('%s');", escaped
        );
        
        ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command", psScript);
        Process process = pb.start();
        process.waitFor();
    }

    /**
     * Linux text-to-speech using espeak.
     */
    private void speakLinux(String text) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("espeak", text);
        Process process = pb.start();
        process.waitFor();
    }

    /**
     * Check if voice features are available.
     */
    public boolean isTTSAvailable() {
        return ttsAvailable;
    }

    public boolean isSTTAvailable() {
        return sttAvailable;
    }

    /**
     * Get voice system status.
     */
    public String getStatus() {
        return String.format(
            "Voice System: TTS=%s, STT=%s, OS=%s",
            ttsAvailable ? "✓" : "✗",
            sttAvailable ? "✓" : "✗",
            os
        );
    }

    /**
     * Simulate voice chat (for testing without live input).
     */
    public void simulateVoiceChat(String engine, String model) throws Exception {
        System.out.println("\n🎤 Simulated Voice Chat Mode\n");
        
        String[] questions = {
            "What is the weather?",
            "Tell me about artificial intelligence",
            "How does machine learning work?"
        };
        
        for (String question : questions) {
            System.out.println("User: " + question);
            speak(question);
            
            // Simulate processing
            Thread.sleep(1000);
            
            String response = "Processing: " + question + " using " + model;
            System.out.println("Assistant: " + response);
            speak(response);
            
            System.out.println();
            Thread.sleep(1000);
        }
        
        System.out.println("Goodbye!");
        speak("Goodbye!");
    }
}

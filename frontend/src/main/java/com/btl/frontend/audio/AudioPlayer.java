/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.frontend.audio;

/**
 *
 * @author ADMIN
 */
import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Engine phát nhạc sử dụng javax.sound.sampled (API âm thanh tích hợp trong JDK).
 *
 * Hỗ trợ: WAV (PCM). Không hỗ trợ MP3 (cần thư viện ngoài như JLayer).
 *
 * Kiến trúc:
 * - Clip: Đối tượng chính để phát audio, load toàn bộ dữ liệu vào RAM
 * - PlayerListener: Interface callback thông báo trạng thái cho UI (Observer pattern)
 * - Position Thread: Thread nền cập nhật vị trí phát mỗi 250ms
 *
 * Luồng: load(bytes) → play() → [position updates] → pause()/stop()
 */
public class AudioPlayer {

    /** Interface callback - UI implement để nhận cập nhật từ player */
    public interface PlayerListener {
        void onPositionChanged(int currentSeconds, int totalSeconds); // Vị trí thay đổi
        void onStateChanged(PlayState state);   // Trạng thái thay đổi (play/pause/stop)
        void onTrackFinished();                 // Bài hát kết thúc
    }

    /** Các trạng thái của player */
    public enum PlayState { STOPPED, PLAYING, PAUSED }

    private Clip clip;                        // Đối tượng phát audio chính
    private PlayState state = PlayState.STOPPED;
    private final List<PlayerListener> listeners = new ArrayList<>(); // Danh sách listener
    private Thread positionThread;            // Thread cập nhật vị trí
    private float volume = 0.8f;              // Âm lượng (0.0 - 1.0)
    private byte[] currentAudioData;          // Dữ liệu audio hiện tại
    private int totalDurationSeconds;         // Tổng thời lượng (giây)

    public void addListener(PlayerListener listener) {
        listeners.add(listener);
    }

    /**
     * Load audio từ byte array (dữ liệu WAV tải từ server).
     * Toàn bộ file được load vào RAM thông qua Clip.
     */
    public void load(byte[] audioData) {
        stop();
        this.currentAudioData = audioData;
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bais);
            AudioFormat format = ais.getFormat();

            // Chuyển byte array thành AudioInputStream
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                // Nếu không phải PCM, chuyển đổi sang PCM 16-bit signed
                AudioFormat decoded = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(), 16,
                    format.getChannels(),
                    format.getChannels() * 2,
                    format.getSampleRate(), false
                );
                ais = AudioSystem.getAudioInputStream(decoded, ais);
                format = decoded;
            }

            clip = AudioSystem.getClip();       // Lấy Clip từ hệ thống
            clip.open(ais);                      // Load toàn bộ audio vào RAM
            totalDurationSeconds = (int) (clip.getMicrosecondLength() / 1_000_000); // Tính thời lượng
            setVolume(volume);

            // Listener phát hiện khi bài hát kết thúc
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && state == PlayState.PLAYING) {
                    if (clip.getMicrosecondPosition() >= clip.getMicrosecondLength()) {
                        state = PlayState.STOPPED;
                        notifyStateChanged();
                        for (PlayerListener l : listeners) l.onTrackFinished();
                    }
                }
            });

            state = PlayState.STOPPED;
            notifyStateChanged();
        } catch (Exception e) {
            System.err.println("[AudioPlayer] Load error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Phát nhạc từ vị trí hiện tại */
    public void play() {
        if (clip == null) return;
        clip.start();                // Bắt đầu phát
        state = PlayState.PLAYING;
        notifyStateChanged();
        startPositionUpdater();      // Bắt đầu thread cập nhật vị trí
    }

    /** Tạm dừng phát (giữ vị trí hiện tại) */
    public void pause() {
        if (clip == null) return;
        clip.stop();                 // Dừng phát (không reset vị trí)
        state = PlayState.PAUSED;
        notifyStateChanged();
        stopPositionUpdater();
    }

    /** Chuyển đổi giữa play và pause */
    public void togglePlayPause() {
        if (state == PlayState.PLAYING) pause();
        else play();
    }

    public void stop() {
        stopPositionUpdater();
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
        state = PlayState.STOPPED;
        notifyStateChanged();
    }

    /** Nhảy đến vị trí (giây) bất kỳ trong bài */
    public void seek(int seconds) {
        if (clip == null) return;
        long micros = (long) seconds * 1_000_000;  // Chuyển giây sang microseconds
        clip.setMicrosecondPosition(Math.min(micros, clip.getMicrosecondLength()));
    }

    /**
     * Điều chỉnh âm lượng (0.0 = im lặng, 1.0 = tối đa).
     * Chuyển từ thầng tuyến (0-1) sang decibel vì API Java dùng dB.
     * Công thức: dB = 20 * log10(volume)
     */
    public void setVolume(float vol) {
        this.volume = Math.max(0f, Math.min(1f, vol)); // Giới hạn 0-1
        if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl fc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log10(Math.max(volume, 0.0001)) * 20); // Chuyển sang dB
            dB = Math.max(dB, fc.getMinimum());  // Không dưới mức tối thiểu
            dB = Math.min(dB, fc.getMaximum());  // Không trên mức tối đa
            fc.setValue(dB);
        }
    }

    public float getVolume() { return volume; }
    public PlayState getState() { return state; }
    public boolean isPlaying() { return state == PlayState.PLAYING; }
    public int getTotalSeconds() { return totalDurationSeconds; }

    public int getCurrentSeconds() {
        if (clip == null) return 0;
        return (int) (clip.getMicrosecondPosition() / 1_000_000);
    }

    public byte[] getCurrentAudioData() { return currentAudioData; }

    /**
     * Trích xuất dữ liệu waveform để vẽ đồ thị sóng âm.
     *
     * Thuật toán:
     * 1. Đọc toàn bộ samples từ WAV
     * 2. Chia thành numSamples "bucket" (đoạn)
     * 3. Mỗi bucket lấy giá trị peak (cao nhất)
     * 4. Trả về mảng float[] (0.0 - 1.0) để vẽ lên WaveformPanel
     *
     * @param numSamples Số thanh (bar) sẽ vẽ trên màn hình
     */
    public float[] getWaveformData(int numSamples) {
        if (currentAudioData == null) return new float[0];
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(currentAudioData);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bais);
            AudioFormat format = ais.getFormat();
            byte[] audioBytes = ais.readAllBytes();

            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int channels = format.getChannels();
            int totalSamples = audioBytes.length / (bytesPerSample * channels);
            int samplesPerBucket = Math.max(1, totalSamples / numSamples);

            float[] waveform = new float[numSamples];
            for (int i = 0; i < numSamples && i * samplesPerBucket < totalSamples; i++) {
                float maxVal = 0;
                for (int j = 0; j < samplesPerBucket; j++) {
                    int sampleIndex = (i * samplesPerBucket + j) * bytesPerSample * channels;
                    if (sampleIndex + 1 >= audioBytes.length) break;
                    short sample;
                    if (bytesPerSample == 2) {
                        sample = (short) ((audioBytes[sampleIndex + 1] << 8) | (audioBytes[sampleIndex] & 0xFF));
                    } else {
                        sample = (short) (audioBytes[sampleIndex] << 8);
                    }
                    float normalized = Math.abs(sample) / 32768f;
                    if (normalized > maxVal) maxVal = normalized;
                }
                waveform[i] = maxVal;
            }
            return waveform;
        } catch (Exception e) {
            return new float[numSamples];
        }
    }

    /**
     * Thread nền cập nhật vị trí phát mỗi 250ms.
     * setDaemon(true) = thread tự dừng khi app tắt.
     */
    private void startPositionUpdater() {
        stopPositionUpdater();
        positionThread = new Thread(() -> {
            while (state == PlayState.PLAYING && clip != null) {
                int current = getCurrentSeconds();
                // Thông báo cho tất cả listener (UI cập nhật thanh progress)
                for (PlayerListener l : listeners) l.onPositionChanged(current, totalDurationSeconds);
                try { Thread.sleep(250); } catch (InterruptedException e) { break; }
            }
        });
        positionThread.setDaemon(true);
        positionThread.start();
    }

    private void stopPositionUpdater() {
        if (positionThread != null) {
            positionThread.interrupt();
            positionThread = null;
        }
    }

    /** Thông báo tất cả listener khi trạng thái thay đổi */
    private void notifyStateChanged() {
        for (PlayerListener l : listeners) l.onStateChanged(state);
    }
}

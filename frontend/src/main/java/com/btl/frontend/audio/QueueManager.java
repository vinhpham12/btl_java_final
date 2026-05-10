package com.btl.frontend.audio;

import com.btl.frontend.api.ApiClient;
import com.btl.frontend.ui.PlayerBar;
import com.btl.frontend.util.JsonHelper;
import javax.swing.*;
import java.util.*;

/**
 * Quản lý hàng đợi phát nhạc (Queue).
 * Khi bài hát hiện tại kết thúc → tự động phát bài tiếp theo.
 */
public class QueueManager {

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private final List<Map<String, Object>> queue = new ArrayList<>();
    private int currentIndex = -1;
    private final List<QueueListener> listeners = new ArrayList<>();

    public interface QueueListener {
        void onQueueChanged();
    }

    public QueueManager(AudioPlayer player, PlayerBar playerBar) {
        this.player = player;
        this.playerBar = playerBar;

        // Khi bài hát kết thúc → phát bài tiếp theo
        player.addListener(new AudioPlayer.PlayerListener() {
            @Override
            public void onPositionChanged(int c, int t) {
            }

            @Override
            public void onStateChanged(AudioPlayer.PlayState state) {
            }

            @Override
            public void onTrackFinished() {
                SwingUtilities.invokeLater(() -> playNext());
            }
        });
    }

    /** Thêm bài vào cuối hàng đợi */
    public void addToQueue(Map<String, Object> track) {
        queue.add(track);
        notifyListeners();
    }

    /** Phát ngay 1 bài — thêm vào queue và nhảy tới vị trí đó */
    public void playNow(Map<String, Object> track) {
        // Thêm vào queue sau vị trí hiện tại
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            queue.add(currentIndex + 1, track);
            currentIndex++;
        } else {
            queue.add(track);
            currentIndex = queue.size() - 1;
        }
        loadAndPlay(queue.get(currentIndex));
        notifyListeners();
    }

    /** Phát bài tiếp theo trong hàng đợi */
    public void playNext() {
        if (currentIndex + 1 < queue.size()) {
            currentIndex++;
            loadAndPlay(queue.get(currentIndex));
            notifyListeners();
        }
    }

    /** Phát bài trước đó trong hàng đợi */
    public void playPrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            loadAndPlay(queue.get(currentIndex));
            notifyListeners();
        }
    }

    /** Xóa bài khỏi hàng đợi */
    public void removeFromQueue(int index) {
        if (index >= 0 && index < queue.size()) {
            queue.remove(index);
            if (index < currentIndex)
                currentIndex--;
            else if (index == currentIndex)
                currentIndex = Math.min(currentIndex, queue.size() - 1);
            notifyListeners();
        }
    }

    /** Xóa toàn bộ hàng đợi */
    public void clearQueue() {
        queue.clear();
        currentIndex = -1;
        notifyListeners();
    }

    /** Trộn ngẫu nhiên hàng đợi (shuffle) */
    public void shuffle() {
        if (queue.size() <= 1)
            return;
        // Giữ bài đang phát, shuffle phần còn lại
        Map<String, Object> current = currentIndex >= 0 ? queue.get(currentIndex) : null;
        Collections.shuffle(queue);
        if (current != null) {
            queue.remove(current);
            queue.add(0, current);
            currentIndex = 0;
        }
        notifyListeners();
    }

    public List<Map<String, Object>> getQueue() {
        return Collections.unmodifiableList(queue);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean hasNext() {
        return currentIndex + 1 < queue.size();
    }

    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    public void addListener(QueueListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (QueueListener l : listeners)
            l.onQueueChanged();
    }

    private void loadAndPlay(Map<String, Object> track) {
        int trackId = JsonHelper.getInt(track, "id");
        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", "Unknown");
        String localPath = JsonHelper.getString(track, "localPath");

        new Thread(() -> {
            try {
                byte[] audioData;

                if (localPath != null && !localPath.isEmpty()) {
                    // File local — đọc trực tiếp từ đĩa
                    audioData = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(localPath));
                } else {
                    // File server — gọi API
                    ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                    Map<String, Object> historyBody = new HashMap<>();
                    historyBody.put("trackId", trackId);
                    ApiClient.post("/history", historyBody);
                    audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                }

                if (audioData != null) {
                    final byte[] finalData = audioData;
                    SwingUtilities.invokeLater(() -> {
                        player.load(finalData);
                        player.play();
                        playerBar.setTrackInfo(title, artist);
                    });
                }
            } catch (Exception ex) {
                System.err.println("[QueueManager] Error playing track: " + ex.getMessage());
            }
        }).start();
    }
}

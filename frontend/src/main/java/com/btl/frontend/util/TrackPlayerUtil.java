package com.btl.frontend.util;

import com.btl.frontend.api.ApiClient;
import com.btl.frontend.audio.AudioPlayer;
import com.btl.frontend.ui.PlayerBar;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiện ích phát nhạc dùng chung cho tất cả các panel.
 * Tránh duplicate code playTrack() ở HomePanel, SearchPanel, ProfilePanel, PlaylistPanel.
 */
public class TrackPlayerUtil {

    /**
     * Phát 1 track: tăng play count, download audio, load và play.
     * Chạy trên background thread, cập nhật UI trên EDT.
     */
    public static void playTrack(Map<String, Object> track, AudioPlayer player, PlayerBar playerBar) {
        int trackId = JsonHelper.getInt(track, "id");
        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", "Unknown");

        new Thread(() -> {
            try {
                // Tăng play count
                ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                // Download audio data
                byte[] audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                if (audioData != null) {
                    SwingUtilities.invokeLater(() -> {
                        player.load(audioData);
                        player.play();
                        playerBar.setTrackInfo(title, artist);
                    });
                }
            } catch (Exception ex) {
                System.err.println("[TrackPlayerUtil] Error playing track: " + ex.getMessage());
            }
        }).start();
    }
}

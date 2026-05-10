package com.btl.frontend.ui;

import com.btl.frontend.audio.AudioPlayer;
import com.btl.frontend.audio.QueueManager;
import com.btl.frontend.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Panel quét nhạc từ thư mục trên máy tính.
 * Quét file WAV trong folder được chọn, hiển thị danh sách và phát trực tiếp.
 */
public class LocalMusicPanel extends JPanel {

    private static final String[] AUDIO_EXTENSIONS = {".wav", ".wave", ".aif", ".aiff", ".au"};
    private static final String LAST_FOLDER_FILE = "last_scan_folder.txt";

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private QueueManager queueManager;

    private JPanel tracksContainer;
    private JLabel statusLabel;
    private JLabel folderLabel;
    private JButton scanBtn;
    private File currentFolder;
    private List<File> audioFiles = new ArrayList<>();

    public LocalMusicPanel(AudioPlayer player, PlayerBar playerBar) {
        this.player = player;
        this.playerBar = playerBar;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
        loadLastFolder();
    }

    public void setQueueManager(QueueManager qm) { this.queueManager = qm; }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Header
        JLabel header = new JLabel("Local Music");
        header.setFont(UIConstants.FONT_TITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(header);
        content.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Scan and play audio files from your computer");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sub);
        content.add(Box.createVerticalStrut(20));

        // Folder selection row
        JPanel folderRow = new JPanel(new BorderLayout(12, 0));
        folderRow.setOpaque(false);
        folderRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        folderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        folderLabel = new JLabel("No folder selected");
        folderLabel.setFont(UIConstants.FONT_BODY);
        folderLabel.setForeground(UIConstants.TEXT_MUTED);
        folderLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        folderLabel.setBackground(UIConstants.BG_INPUT);
        folderLabel.setOpaque(true);

        scanBtn = new JButton("Choose Folder") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        scanBtn.setFont(UIConstants.FONT_BUTTON);
        scanBtn.setForeground(Color.WHITE);
        scanBtn.setBackground(UIConstants.PRIMARY);
        scanBtn.setBorderPainted(false);
        scanBtn.setContentAreaFilled(false);
        scanBtn.setFocusPainted(false);
        scanBtn.setPreferredSize(new Dimension(140, 36));
        scanBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        scanBtn.addActionListener(e -> chooseFolder());

        JButton refreshBtn = new JButton("Refresh") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        refreshBtn.setFont(UIConstants.FONT_BUTTON);
        refreshBtn.setForeground(UIConstants.PRIMARY);
        refreshBtn.setBackground(UIConstants.BG_CARD);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setContentAreaFilled(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setPreferredSize(new Dimension(90, 36));
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            if (currentFolder != null) scanFolder(currentFolder);
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(refreshBtn);
        btnPanel.add(scanBtn);

        folderRow.add(folderLabel, BorderLayout.CENTER);
        folderRow.add(btnPanel, BorderLayout.EAST);
        content.add(folderRow);
        content.add(Box.createVerticalStrut(8));

        // Play All + Add All to Queue
        JPanel bulkActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bulkActions.setOpaque(false);
        bulkActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        bulkActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JButton playAllBtn = createActionButton("Play All", UIConstants.PRIMARY);
        playAllBtn.addActionListener(e -> playAll());

        JButton queueAllBtn = createActionButton("Add All to Queue", UIConstants.ACCENT);
        queueAllBtn.addActionListener(e -> addAllToQueue());

        bulkActions.add(playAllBtn);
        bulkActions.add(queueAllBtn);
        content.add(bulkActions);
        content.add(Box.createVerticalStrut(16));

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(8));

        // Track list
        tracksContainer = new JPanel();
        tracksContainer.setLayout(new BoxLayout(tracksContainer, BoxLayout.Y_AXIS));
        tracksContainer.setOpaque(false);
        tracksContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(tracksContainer);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }

    private void chooseFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select music folder");
        if (currentFolder != null) fc.setCurrentDirectory(currentFolder);

        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = fc.getSelectedFile();
            saveLastFolder(folder);
            scanFolder(folder);
        }
    }

    public void scanFolder(File folder) {
        currentFolder = folder;
        folderLabel.setText(folder.getAbsolutePath());
        statusLabel.setText("Scanning...");
        tracksContainer.removeAll();
        tracksContainer.revalidate();
        tracksContainer.repaint();

        new Thread(() -> {
            try {
                List<File> found = new ArrayList<>();
                scanRecursive(folder, found, 3); // Max 3 levels deep
                found.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

                SwingUtilities.invokeLater(() -> {
                    audioFiles = found;
                    tracksContainer.removeAll();
                    if (found.isEmpty()) {
                        statusLabel.setText("No audio files found in this folder");
                    } else {
                        statusLabel.setText(found.size() + " audio files found");
                        for (int i = 0; i < found.size(); i++) {
                            tracksContainer.add(createLocalTrackCard(i, found.get(i)));
                            tracksContainer.add(Box.createVerticalStrut(2));
                        }
                    }
                    tracksContainer.revalidate();
                    tracksContainer.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Error scanning: " + e.getMessage()));
            }
        }).start();
    }

    private void scanRecursive(File dir, List<File> result, int depth) {
        if (depth < 0 || dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile() && isAudioFile(f.getName())) {
                result.add(f);
            } else if (f.isDirectory() && depth > 0) {
                scanRecursive(f, result, depth - 1);
            }
        }
    }

    private boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private JPanel createLocalTrackCard(int index, File file) {
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBackground(UIConstants.BG_SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Index
        JLabel indexLabel = new JLabel(String.valueOf(index + 1));
        indexLabel.setFont(UIConstants.FONT_HEADING);
        indexLabel.setForeground(UIConstants.TEXT_MUTED);
        indexLabel.setPreferredSize(new Dimension(30, 30));
        indexLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Play button
        JButton playBtn = IconFactory.iconButton(IconFactory.playIcon(18, UIConstants.PRIMARY));
        playBtn.setPreferredSize(new Dimension(36, 36));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(indexLabel);
        leftPanel.add(playBtn);

        // Info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        String fileName = file.getName();
        String nameWithoutExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        // Cố gắng tách Artist - Title từ tên file
        String displayTitle = nameWithoutExt;
        String displayArtist = "Local file";
        if (nameWithoutExt.contains(" - ")) {
            String[] parts = nameWithoutExt.split(" - ", 2);
            displayArtist = parts[0].trim();
            displayTitle = parts[1].trim();
        }

        JLabel titleLabel = new JLabel(displayTitle);
        titleLabel.setFont(UIConstants.FONT_HEADING);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        // Hiển thị subfolder nếu không ở root
        String relativePath = currentFolder != null ?
            file.getParentFile().getAbsolutePath().replace(currentFolder.getAbsolutePath(), "").replaceFirst("^[\\\\/]", "") : "";
        String detail = displayArtist + (relativePath.isEmpty() ? "" : " · " + relativePath)
            + " · " + formatFileSize(file.length());
        JLabel detailLabel = new JLabel(detail);
        detailLabel.setFont(UIConstants.FONT_SMALL);
        detailLabel.setForeground(UIConstants.TEXT_SECONDARY);

        info.add(titleLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(detailLabel);

        // Right buttons
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);

        JButton queueBtn = IconFactory.iconButton(IconFactory.queueIcon(14, UIConstants.TEXT_MUTED));
        queueBtn.setPreferredSize(new Dimension(28, 28));
        queueBtn.setToolTipText("Add to queue");
        queueBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) { queueBtn.setIcon(IconFactory.queueIcon(14, UIConstants.ACCENT)); }
            public void mouseExited(MouseEvent ev) { queueBtn.setIcon(IconFactory.queueIcon(14, UIConstants.TEXT_MUTED)); }
        });

        final String fTitle = displayTitle;
        final String fArtist = displayArtist;

        queueBtn.addActionListener(e -> {
            if (queueManager != null) {
                Map<String, Object> localTrack = createLocalTrackMap(file, fTitle, fArtist);
                queueManager.addToQueue(localTrack);
            }
        });

        right.add(queueBtn);

        card.add(leftPanel, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        // Hover
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.BG_HOVER); }
            public void mouseExited(MouseEvent e) { card.setBackground(UIConstants.BG_SURFACE); }
        });

        // Play
        playBtn.addActionListener(e -> playLocalFile(file, fTitle, fArtist));
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) playLocalFile(file, fTitle, fArtist);
            }
        });

        return card;
    }

    private void playLocalFile(File file, String title, String artist) {
        new Thread(() -> {
            try {
                byte[] audioData = Files.readAllBytes(file.toPath());
                SwingUtilities.invokeLater(() -> {
                    player.load(audioData);
                    player.play();
                    playerBar.setTrackInfo(title, artist);
                });
            } catch (Exception ex) {
                System.err.println("[LocalMusic] Error playing: " + ex.getMessage());
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Cannot play: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void playAll() {
        if (audioFiles.isEmpty() || queueManager == null) return;
        queueManager.clearQueue();
        for (File f : audioFiles) {
            String name = f.getName();
            String nameNoExt = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            String title = nameNoExt, artist = "Local";
            if (nameNoExt.contains(" - ")) {
                String[] p = nameNoExt.split(" - ", 2);
                artist = p[0].trim(); title = p[1].trim();
            }
            Map<String, Object> track = createLocalTrackMap(f, title, artist);
            queueManager.addToQueue(track);
        }
        // Phát bài đầu tiên
        if (!audioFiles.isEmpty()) {
            File first = audioFiles.get(0);
            String name = first.getName();
            String nameNoExt = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            String title = nameNoExt, artist = "Local";
            if (nameNoExt.contains(" - ")) {
                String[] p = nameNoExt.split(" - ", 2);
                artist = p[0].trim(); title = p[1].trim();
            }
            playLocalFile(first, title, artist);
        }
    }

    private void addAllToQueue() {
        if (audioFiles.isEmpty() || queueManager == null) return;
        for (File f : audioFiles) {
            String name = f.getName();
            String nameNoExt = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
            String title = nameNoExt, artist = "Local";
            if (nameNoExt.contains(" - ")) {
                String[] p = nameNoExt.split(" - ", 2);
                artist = p[0].trim(); title = p[1].trim();
            }
            queueManager.addToQueue(createLocalTrackMap(f, title, artist));
        }
        statusLabel.setText(audioFiles.size() + " tracks added to queue");
    }

    /**
     * Tạo Map giả lập track data cho file local (để QueueManager xử lý).
     * Dùng id = -1 và thêm "localPath" để QueueManager biết đây là file local.
     */
    private Map<String, Object> createLocalTrackMap(File file, String title, String artist) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", -1);
        map.put("title", title);
        map.put("artist", artist);
        map.put("localPath", file.getAbsolutePath());
        return map;
    }

    private JButton createActionButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(UIConstants.FONT_SMALL);
        btn.setForeground(Color.WHITE);
        btn.setBackground(color);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(130, 28));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    // ===== Lưu/Load folder cuối cùng =====

    private void saveLastFolder(File folder) {
        try {
            Files.writeString(Path.of(LAST_FOLDER_FILE), folder.getAbsolutePath());
        } catch (Exception ignored) {}
    }

    private void loadLastFolder() {
        try {
            Path p = Path.of(LAST_FOLDER_FILE);
            if (Files.exists(p)) {
                String path = Files.readString(p).trim();
                File f = new File(path);
                if (f.isDirectory()) {
                    currentFolder = f;
                    folderLabel.setText(path);
                }
            }
        } catch (Exception ignored) {}
    }

    /** Gọi khi panel được hiển thị — auto scan nếu đã có folder */
    public void onShow() {
        if (currentFolder != null && audioFiles.isEmpty()) {
            scanFolder(currentFolder);
        }
    }
}

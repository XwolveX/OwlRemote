import client.ClientApp;
import server.ServerApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class MainLauncher {

    // Bảng màu gradient hiện đại
    private static final Color COLOR_BG_START = new Color(0x1a1a2e);
    private static final Color COLOR_BG_END = new Color(0x16213e);
    private static final Color COLOR_CARD = new Color(0x0f3460);
    private static final Color COLOR_PRIMARY = new Color(0x00d4ff);
    private static final Color COLOR_SECONDARY = new Color(0x7c3aed);
    private static final Color COLOR_ACCENT = new Color(0xf72585);

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(MainLauncher::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame launcherFrame = new JFrame("OwlRemote");
        launcherFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        launcherFrame.setSize(500, 600);
        launcherFrame.setLocationRelativeTo(null);
        launcherFrame.setResizable(false);
        launcherFrame.setUndecorated(true);
        launcherFrame.setShape(new RoundRectangle2D.Double(0, 0, 500, 600, 30, 30));

        // Panel chính với gradient background
        GradientPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        // Header container
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        // Logo/Icon lớn
        JLabel iconLabel = new JLabel("🦉", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(iconLabel);

        headerPanel.add(Box.createVerticalStrut(20));

        // Tiêu đề chính
        JLabel titleLabel = new JLabel("OwlRemote");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 42));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(titleLabel);

        headerPanel.add(Box.createVerticalStrut(10));

        // Subtitle
        JLabel subtitleLabel = new JLabel("Remote Desktop Control");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(150, 170, 200));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Panel chứa các nút
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(40, 0, 0, 0));

        // Nút Host
        ModernButton hostButton = new ModernButton(
                "🖥️ Share This Screen",
                "Start sharing your screen",
                COLOR_PRIMARY
        );
        hostButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(hostButton);

        centerPanel.add(Box.createVerticalStrut(20));

        // Nút Client
        ModernButton clientButton = new ModernButton(
                "🎮 Control Remote Screen",
                "Connect to another computer",
                COLOR_ACCENT
        );
        clientButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(clientButton);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Footer với nút thoát
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);

        JButton exitButton = new JButton("✕");
        exitButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        exitButton.setForeground(new Color(200, 200, 200));
        exitButton.setBackground(new Color(40, 40, 50, 100));
        exitButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        exitButton.setFocusPainted(false);
        exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> System.exit(0));
        exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exitButton.setBackground(new Color(220, 53, 69));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exitButton.setBackground(new Color(40, 40, 50, 100));
            }
        });

        footerPanel.add(exitButton);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        // Event listeners
        hostButton.addActionListener(e -> {
            launcherFrame.dispose();
            new Thread(() -> ServerApp.start()).start();
        });

        clientButton.addActionListener(e -> {
            launcherFrame.dispose();
            new Thread(() -> ClientApp.start()).start();
        });

        launcherFrame.getContentPane().add(mainPanel);
        launcherFrame.setVisible(true);
    }

    // Panel với gradient background
    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            GradientPaint gradient = new GradientPaint(
                    0, 0, COLOR_BG_START,
                    0, getHeight(), COLOR_BG_END
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // Modern button với hover effects
    static class ModernButton extends JPanel {
        private final JLabel mainLabel;
        private final JLabel subLabel;
        private final Color baseColor;
        private Color currentColor;
        private boolean isHovered = false;

        public ModernButton(String mainText, String subText, Color color) {
            this.baseColor = color;
            this.currentColor = color;

            setLayout(new BorderLayout(15, 5));
            setOpaque(false);
            setPreferredSize(new Dimension(380, 90));
            setMaximumSize(new Dimension(380, 90));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Text container
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

            mainLabel = new JLabel(mainText);
            mainLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            mainLabel.setForeground(Color.WHITE);
            mainLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(mainLabel);

            textPanel.add(Box.createVerticalStrut(5));

            subLabel = new JLabel(subText);
            subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subLabel.setForeground(new Color(200, 200, 200));
            subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(subLabel);

            add(textPanel, BorderLayout.CENTER);

            // Hover effects
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    isHovered = true;
                    currentColor = baseColor.brighter();
                    repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    isHovered = false;
                    currentColor = baseColor;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Shadow effect
            if (isHovered) {
                g2d.setColor(new Color(0, 0, 0, 80));
                g2d.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 20, 20);
            }

            // Button background với gradient
            GradientPaint gradient = new GradientPaint(
                    0, 0, currentColor,
                    0, getHeight(), currentColor.darker()
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

            // Border
            g2d.setColor(currentColor.brighter());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

            super.paintComponent(g);
        }

        public void addActionListener(java.awt.event.ActionListener listener) {
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    listener.actionPerformed(new java.awt.event.ActionEvent(
                            ModernButton.this,
                            java.awt.event.ActionEvent.ACTION_PERFORMED,
                            "clicked"
                    ));
                }
            });
        }
    }
}
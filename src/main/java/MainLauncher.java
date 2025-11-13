import client.ClientApp;
import server.ServerApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.*;
import common.AppColors;

public class MainLauncher {

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
        launcherFrame.setSize(550, 680);
        launcherFrame.setLocationRelativeTo(null);
        launcherFrame.setResizable(false);
        launcherFrame.setUndecorated(true);
        launcherFrame.setShape(new RoundRectangle2D.Double(0, 0, 550, 680, 35, 35));

        // Main panel with gradient background
        GradientPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.setBorder(new EmptyBorder(45, 45, 45, 45));

        // Draggable window
        final Point[] mouseDownCompCoords = {null};
        mainPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                mouseDownCompCoords[0] = e.getPoint();
            }
            public void mouseReleased(MouseEvent e) {
                mouseDownCompCoords[0] = null;
            }
        });
        mainPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                launcherFrame.setLocation(currCoords.x - mouseDownCompCoords[0].x,
                        currCoords.y - mouseDownCompCoords[0].y);
            }
        });

        // Header container
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        // Container logo with animation effect
        JPanel logoContainer = new JPanel();
        logoContainer.setLayout(new BoxLayout(logoContainer, BoxLayout.Y_AXIS));
        logoContainer.setOpaque(false);
        logoContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Logo with circle background
        CircleLogoPanel logoPanel = new CircleLogoPanel();
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoContainer.add(logoPanel);

        headerPanel.add(logoContainer);
        headerPanel.add(Box.createVerticalStrut(25));

        // Main title with shadow effect
        JLabel titleLabel = new JLabel("OwlRemote") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // Text shadow
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x + 2, y + 2);

                // Main text
                g2d.setColor(getForeground());
                g2d.drawString(getText(), x, y);
            }
        };
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(titleLabel);

        headerPanel.add(Box.createVerticalStrut(12));

        // Subtitle with gradient text effect
        JLabel subtitleLabel = new JLabel("Remote Desktop Control");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        subtitleLabel.setForeground(new Color(160, 180, 210));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Panel contains buttons
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(45, 0, 0, 0));

        // Host button with a more beautiful icon
        ModernButton hostButton = new ModernButton(
                "🖥️  Share This Screen",
                "Allow others to view and control your desktop",
                AppColors.COLOR_PRIMARY,
                "HOST"
        );
        hostButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(hostButton);

        centerPanel.add(Box.createVerticalStrut(22));

        // Client node
        ModernButton clientButton = new ModernButton(
                "🎮  Control Remote Screen",
                "Connect and control another computer remotely",
                AppColors.COLOR_ACCENT,
                "CLIENT"
        );
        clientButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(clientButton);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Footer with information and exit button
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Version info
        JLabel versionLabel = new JLabel("v1.0.0");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        versionLabel.setForeground(new Color(100, 120, 150));
        versionLabel.setHorizontalAlignment(SwingConstants.LEFT);


        // Exit button with better effect
        JButton exitButton = new JButton("✕") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                // Text
                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), x, y);
            }
        };
        exitButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        exitButton.setForeground(new Color(200, 200, 200));
        exitButton.setBackground(new Color(40, 40, 50, 120));
        exitButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        exitButton.setFocusPainted(false);
        exitButton.setContentAreaFilled(false);
        exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exitButton.addActionListener(e -> {
            // Fade out animation before exiting
            Timer timer = new Timer(15, null);
            final float[] opacity = {1.0f};
            timer.addActionListener(evt -> {
                opacity[0] -= 0.05f;
                if (opacity[0] <= 0) {
                    timer.stop();
                    System.exit(0);
                }
                launcherFrame.setOpacity(opacity[0]);
            });
            timer.start();
        });
        exitButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                exitButton.setBackground(new Color(220, 53, 69));
                exitButton.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent evt) {
                exitButton.setBackground(new Color(40, 40, 50, 120));
                exitButton.setForeground(new Color(200, 200, 200));
            }
        });

        JPanel exitPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        exitPanel.setOpaque(false);
        exitPanel.add(exitButton);

        footerPanel.add(versionLabel, BorderLayout.WEST);
        footerPanel.add(exitPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        // Event listeners
        hostButton.addActionListener(e -> {
            JPanel hostPanel = new JPanel(new GridLayout(0,1,5,5));
            hostPanel.setBorder(new EmptyBorder(10,10,10,10));
            JTextField networkIdField = new JTextField(20);
            JPasswordField apiKeyField = new JPasswordField(20);
            hostPanel.add(new JLabel("Enter Network ID:"));
            hostPanel.add(networkIdField);
            hostPanel.add(new JLabel("Enter Central API Key:"));
            hostPanel.add(apiKeyField);

            int result = JOptionPane.showConfirmDialog(launcherFrame,hostPanel,"Host Setup",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String networkId = networkIdField.getText();
                String apiKey = new String(apiKeyField.getPassword());

                if (networkId.isEmpty() || apiKey.isEmpty()) {
                    JOptionPane.showMessageDialog(launcherFrame,"Both fields are required.",
                            "Error",JOptionPane.ERROR_MESSAGE);
                }
                else {
                    launcherFrame.dispose();
                    new Thread(() -> ServerApp.start(networkId, apiKey)).start();
                }
            }
        });

        clientButton.addActionListener(e -> {
            String networkId = JOptionPane.showInputDialog(launcherFrame,"Enter Host Network ID:",
                    "Client Setup", JOptionPane.PLAIN_MESSAGE);
            if (networkId != null && !networkId.isEmpty()) {
                launcherFrame.dispose();
                new Thread(() -> ClientApp.start(networkId)).start();
            }else if (networkId != null) {
                JOptionPane.showMessageDialog(launcherFrame, "Network ID is required.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        launcherFrame.getContentPane().add(mainPanel);
        launcherFrame.setVisible(true);
    }

    // Circle logo panel with gradient background
    static class CircleLogoPanel extends JPanel {
        public CircleLogoPanel() {
            setPreferredSize(new Dimension(120, 120));
            setMaximumSize(new Dimension(120, 120));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Shadow
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval(8, 8, 104, 104);

            // Gradient circle
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(100, 120, 255),
                    0, 120, new Color(60, 80, 200)
            );
            g2d.setPaint(gradient);
            g2d.fillOval(0, 0, 110, 110);

            // Border
            g2d.setColor(new Color(255, 255, 255, 50));
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(2, 2, 106, 106);

            // Emoji
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 60));
            FontMetrics fm = g2d.getFontMetrics();
            String emoji = "🦉";
            int x = (110 - fm.stringWidth(emoji)) / 2;
            int y = (110 + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(emoji, x, y);
        }
    }

    // Panel with gradient background
    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            GradientPaint gradient = new GradientPaint(
                    0, 0, AppColors.COLOR_BG_START,
                    0, getHeight(), AppColors.COLOR_BG_END
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // Modern button with more effects
    static class ModernButton extends JPanel {
        private final JLabel mainLabel;
        private final JLabel subLabel;
        private final JLabel badgeLabel;
        private final Color baseColor;
        private Color currentColor;
        private boolean isHovered = false;
        private float glowIntensity = 0f;

        public ModernButton(String mainText, String subText, Color color, String badge) {
            this.baseColor = color;
            this.currentColor = color;

            setLayout(new BorderLayout(15, 5));
            setOpaque(false);
            setPreferredSize(new Dimension(420, 100));
            setMaximumSize(new Dimension(420, 100));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Text container
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

            mainLabel = new JLabel(mainText);
            mainLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            mainLabel.setForeground(Color.WHITE);
            mainLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(mainLabel);

            textPanel.add(Box.createVerticalStrut(6));

            subLabel = new JLabel(subText);
            subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subLabel.setForeground(new Color(210, 210, 210));
            subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            textPanel.add(subLabel);

            add(textPanel, BorderLayout.CENTER);

            // Badge
            badgeLabel = new JLabel(badge);
            badgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
            badgeLabel.setForeground(new Color(255, 255, 255, 180));
            badgeLabel.setBorder(new EmptyBorder(0, 0, 0, 20));
            badgeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            add(badgeLabel, BorderLayout.EAST);

            // Hover animation
            Timer glowTimer = new Timer(30, e -> {
                if (isHovered && glowIntensity < 1f) {
                    glowIntensity += 0.1f;
                } else if (!isHovered && glowIntensity > 0f) {
                    glowIntensity -= 0.1f;
                }
                repaint();
            });
            glowTimer.start();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent evt) {
                    isHovered = true;
                    currentColor = baseColor.brighter();
                }

                @Override
                public void mouseExited(MouseEvent evt) {
                    isHovered = false;
                    currentColor = baseColor;
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Glow effect
            if (glowIntensity > 0) {
                int glowSize = (int)(8 * glowIntensity);
                for (int i = glowSize; i > 0; i--) {
                    int alpha = (int)(30 * glowIntensity * (1 - (float)i / glowSize));
                    g2d.setColor(new Color(currentColor.getRed(), currentColor.getGreen(),
                            currentColor.getBlue(), alpha));
                    g2d.fillRoundRect(-i, -i, getWidth() + 2*i, getHeight() + 2*i, 25, 25);
                }
            }

            // Shadow
            g2d.setColor(new Color(0, 0, 0, isHovered ? 100 : 60));
            g2d.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 22, 22);

            // Button background with gradient
            GradientPaint gradient = new GradientPaint(
                    0, 0, currentColor,
                    0, getHeight(), currentColor.darker()
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);

            // Glossy overlay
            GradientPaint glossy = new GradientPaint(
                    0, 0, new Color(255, 255, 255, 40),
                    0, getHeight() / 2, new Color(255, 255, 255, 0)
            );
            g2d.setPaint(glossy);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 22, 22);

            // Border
            g2d.setColor(new Color(255, 255, 255, isHovered ? 100 : 60));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);

            super.paintComponent(g);
        }

        public void addActionListener(ActionListener listener) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    listener.actionPerformed(new ActionEvent(
                            ModernButton.this,
                            ActionEvent.ACTION_PERFORMED,
                            "clicked"
                    ));
                }
            });
        }
    }
}
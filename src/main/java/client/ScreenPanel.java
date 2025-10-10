package client;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;

public class ScreenPanel extends JPanel {

    private BufferedImage currentScreen;
    private final PrintWriter commandSender;

    public ScreenPanel(PrintWriter commandSender) {
        this.commandSender = commandSender;
        setupListeners();
    }

    private void setupListeners() {
        // Yêu cầu focus để nhận sự kiện từ bàn phím
        setFocusable(true);
        requestFocusInWindow();

        // --- Bắt sự kiện chuột ---
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                sendMouseEvent("MOUSE_MOVE", e);
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                // Kéo chuột cũng là di chuyển
                sendMouseEvent("MOUSE_MOVE", e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sendMouseEvent("MOUSE_PRESS", e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                sendMouseEvent("MOUSE_RELEASE", e);
            }
        });

        addMouseWheelListener(this::mouseWheelMoved);

        // --- Bắt sự kiện bàn phím ---
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                sendKeyEvent("KEY_PRESS", e);
            }
            @Override
            public void keyReleased(KeyEvent e) {
                sendKeyEvent("KEY_RELEASE", e);
            }
        });
    }

    private void sendMouseEvent(String command, MouseEvent e) {
        if (currentScreen == null || commandSender == null) return;

        // Tính toán tọa độ thực trên màn hình server dựa trên tỉ lệ
        double scaleX = (double) currentScreen.getWidth() / getWidth();
        double scaleY = (double) currentScreen.getHeight() / getHeight();
        int serverX = (int) (e.getX() * scaleX);
        int serverY = (int) (e.getY() * scaleY);
        int button = e.getButton(); // 1=trái, 2=giữa, 3=phải

        commandSender.println(String.format("%s,%d,%d,%d", command, serverX, serverY, button));
    }

    private void mouseWheelMoved(MouseWheelEvent e) {
        if (commandSender == null) return;
        int rotation = e.getWheelRotation(); // -1 là cuộn lên, 1 là cuộn xuống
        commandSender.println(String.format("MOUSE_WHEEL,%d", rotation));
    }


    private void sendKeyEvent(String command, KeyEvent e) {
        if (commandSender == null) return;
        int keyCode = e.getKeyCode();
        commandSender.println(String.format("%s,%d", command, keyCode));
    }

    public void updateScreen(BufferedImage image) {
        this.currentScreen = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentScreen != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int panelWidth = getWidth();
            int panelHeight = getHeight();
            double imgAspectRatio = (double) currentScreen.getWidth() / currentScreen.getHeight();
            double panelAspectRatio = (double) panelWidth / panelHeight;

            int newWidth, newHeight;
            if (panelAspectRatio > imgAspectRatio) {
                newHeight = panelHeight;
                newWidth = (int) (panelHeight * imgAspectRatio);
            } else {
                newWidth = panelWidth;
                newHeight = (int) (panelWidth / imgAspectRatio);
            }

            int x = (panelWidth - newWidth) / 2;
            int y = (panelHeight - newHeight) / 2;

            g2d.drawImage(currentScreen, x, y, newWidth, newHeight, null);
            g2d.dispose();
        }
    }
}
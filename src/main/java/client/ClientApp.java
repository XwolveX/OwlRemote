package client;

import common.ZeroTierManager;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;

public class ClientApp {

    private static final int COMMAND_PORT = 12345;
    private static final int VIDEO_PORT = 12346;

    public static void start(String networkId) {
        try {
            ZeroTierManager zt = new ZeroTierManager();
            zt.joinNetwork(networkId);

            String serverIp = JOptionPane.showInputDialog("Enter Server IP:");
            if (serverIp == null || serverIp.isBlank()) return;
            String password = JOptionPane.showInputDialog("Enter Password:");
            if (password == null || password.isBlank()) return;

            Socket cmdSocket = new Socket(serverIp, COMMAND_PORT);
            PrintWriter pw = new PrintWriter(cmdSocket.getOutputStream(), true);
            pw.println(password);

            DataInputStream dis = new DataInputStream(cmdSocket.getInputStream());
            int width = dis.readInt();
            int height = dis.readInt();
            System.out.println("[Client] Screen size: " + width + "x" + height);

            Socket videoSocket = new Socket(serverIp, VIDEO_PORT);
            DataInputStream videoIn = new DataInputStream(
                    new BufferedInputStream(videoSocket.getInputStream()));

            JFrame frame = new JFrame("Remote Viewer - " + serverIp);
            ScreenPanel panel = new ScreenPanel(pw);
            frame.add(panel);
            frame.setSize(1280, 720);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            // --- Thread nhận frame ---
            new Thread(() -> {
                try {
                    while (true) {
                        int len = videoIn.readInt();
                        if (len <= 0) continue;
                        byte[] buf = new byte[len];
                        videoIn.readFully(buf);
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(buf));
                        if (img != null) {
                            SwingUtilities.invokeLater(() -> panel.updateScreen(img));
                            System.out.println("[Client] Frame received (" + len / 1024 + " KB)");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Client] Stream ended: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Connection failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

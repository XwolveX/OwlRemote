package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientApp {

    private static JFrame frame;
    private static ScreenPanel screenPanel;
    private static PrintWriter commandSender;

    public static void main(String[] args) {
        String serverIp = "127.0.0.1"; // Thay đổi IP này nếu Server ở máy khác
        int serverPort = 12345;

        try {
            // Kết nối mạng trước khi tạo UI
            Socket socket = new Socket(serverIp, serverPort);
            System.out.println("Đã kết nối tới server.");

            // Luồng gửi lệnh (PrintWriter)
            commandSender = new PrintWriter(socket.getOutputStream(), true); // true = autoFlush

            // Tạo UI trên Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                frame = new JFrame("Remote Desktop Viewer");

                // Khởi tạo ScreenPanel và truyền PrintWriter vào
                screenPanel = new ScreenPanel(commandSender);
                frame.add(screenPanel);

                frame.setSize(1280, 720);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                screenPanel.requestFocusInWindow();
            });

            // Luồng nhận hình ảnh (chạy trên luồng riêng)
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            while (socket.isConnected()) {
                int size = dis.readInt();
                byte[] imageBytes = new byte[size];
                dis.readFully(imageBytes);

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

                if (image != null && screenPanel != null) {
                    // Cập nhật UI một cách an toàn
                    SwingUtilities.invokeLater(() -> screenPanel.updateScreen(image));
                }
            }

        } catch (Exception e) {
            System.out.println("Lỗi client hoặc mất kết nối server.");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Không thể kết nối đến server.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        }
    }
}
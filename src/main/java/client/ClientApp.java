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
        // Hiện hộp thoại yêu cầu nhập IP
        String serverIp = JOptionPane.showInputDialog(
                null,
                "Nhập địa chỉ IP của Server:",
                "Kết nối đến Server",
                JOptionPane.QUESTION_MESSAGE
        );

        // Nếu người dùng không nhập gì hoặc nhấn Cancel, thoát chương trình
        if (serverIp == null || serverIp.trim().isEmpty()) {
            System.out.println("Không nhập IP, chương trình kết thúc.");
            return;
        }

        int serverPort = 12345;

        try {
            // Kết nối mạng trước khi tạo UI
            Socket socket = new Socket(serverIp, serverPort);
            System.out.println("Đã kết nối tới server.");

            // Luồng gửi lệnh (PrintWriter)
            commandSender = new PrintWriter(socket.getOutputStream(), true);

            // Tạo UI trên Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                frame = new JFrame("Remote Desktop Viewer - Đang kết nối tới " + serverIp);
                screenPanel = new ScreenPanel(commandSender);
                frame.add(screenPanel);

                frame.setSize(1280, 720);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                screenPanel.requestFocusInWindow(); // Yêu cầu focus sau khi cửa sổ hiển thị
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
            JOptionPane.showMessageDialog(null, "Không thể kết nối đến server tại IP: " + serverIp, "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        }
    }
}
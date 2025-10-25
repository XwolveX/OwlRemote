package client;

import common.ZeroTierManager;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

public class ClientApp {

    private static JFrame frame;
    private static ScreenPanel screenPanel;
    private static PrintWriter commandSender;

    public static void start(String networkId) {
        ZeroTierManager ztManager = new ZeroTierManager();
        // --- 2. TỰ ĐỘNG JOIN VÀ CHỜ CẤP PHÉP ---
        System.out.println("Đang tham gia mạng: " + networkId);
        ztManager.joinNetwork(networkId);

        // Hiển thị cửa sổ chờ
        JDialog waitingDialog = new JDialog();
        waitingDialog.setTitle("Đang chờ...");
        waitingDialog.add(new JLabel("Đã yêu cầu tham gia mạng. Vui lòng đợi Host cấp phép..."));
        waitingDialog.setSize(400, 100);
        waitingDialog.setLocationRelativeTo(null);
        waitingDialog.setModal(false);
        waitingDialog.setVisible(true);

        String clientIp = ztManager.getManagedIp(networkId);
        while (clientIp == null) {
            try {
                Thread.sleep(5000);
                clientIp = ztManager.getManagedIp(networkId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Được cấp phép! Đóng cửa sổ chờ.
        waitingDialog.dispose();
        System.out.println("Đã được cấp phép! IP của bạn là: " + clientIp);

        // --- 3. BẮT ĐẦU KẾT NỐI ---
        String serverIp = JOptionPane.showInputDialog(
                null,
                "Đã vào mạng! Nhập IP của Host (Server):",
                "Kết nối đến Server",
                JOptionPane.QUESTION_MESSAGE
        );
        if (serverIp == null || serverIp.trim().isEmpty()) {
            System.out.println("Không nhập IP, chương trình kết thúc.");
            return;
        }

        String password = JOptionPane.showInputDialog(
                null,
                "Nhập Mật khẩu kết nối:",
                "Xác thực",
                JOptionPane.QUESTION_MESSAGE
        );
        if (password == null || password.trim().isEmpty()) {
            System.out.println("Không nhập mật khẩu, chương trình kết thúc.");
            return;
        }

        int serverPort = 12345;

        try {
            // Kết nối mạng trước khi tạo UI
            Socket socket = new Socket(serverIp, serverPort);
            System.out.println("Đã kết nối tới server.");

            // Luồng gửi lệnh (PrintWriter)
            commandSender = new PrintWriter(socket.getOutputStream(), true);
            commandSender.println(password); // Gửi mật khẩu

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

            // Luồng nhận hình ảnh
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
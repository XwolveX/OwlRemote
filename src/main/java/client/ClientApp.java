package client;

//Java CV library
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import common.ZeroTierManager;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;

public class ClientApp {

    private static JFrame frame;
    private static ScreenPanel screenPanel;
    private static PrintWriter commandSender;

    // Đặt cổng thành hằng số
    private static final int COMMAND_PORT = 12345;
    private static final int VIDEO_PORT = 12346;

    public static void start(String networkId) {
        // --- Code ZeroTier (Phần 1, 2) ---
        // (Giữ nguyên code ZeroTier của bạn: join và vòng lặp chờ cấp phép)
        ZeroTierManager ztManager = new ZeroTierManager();
        System.out.println("Đang tham gia mạng: " + networkId);
        ztManager.joinNetwork(networkId);

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
        waitingDialog.dispose();
        System.out.println("Đã được cấp phép! IP của bạn là: " + clientIp);
        // --- Kết thúc code ZeroTier ---


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

        // Khai báo socket bên ngoài để đóng trong finally
        Socket commandSocket = null;
        Socket videoSocket = null;

        try {
            // --- SỬA LỖI: THIẾT LẬP 2 SOCKET ---

            // 1. Kết nối Socket Lệnh (Command)
            commandSocket = new Socket(serverIp, COMMAND_PORT);
            System.out.println("Đã kết nối lệnh tới server (cổng " + COMMAND_PORT + ").");

            // 2. Gửi mật khẩu qua Socket Lệnh
            commandSender = new PrintWriter(commandSocket.getOutputStream(), true);
            commandSender.println(password);

            // 3. Kết nối Socket Video
            System.out.println("Đang kết nối video stream (cổng " + VIDEO_PORT + ")...");
            videoSocket = new Socket(serverIp, VIDEO_PORT);
            System.out.println("Đã kết nối video stream.");

            // --- KẾT THÚC SỬA LỖI 2 SOCKET ---


            // Tạo UI trên Event Dispatch Thread (EDT)
            // SỬA LỖI: Truyền commandSender (từ commandSocket) vào ScreenPanel
            final Socket finalCommandSocket = commandSocket; // Cần cho lambda
            SwingUtilities.invokeLater(() -> {
                frame = new JFrame("Remote Desktop Viewer - Đang kết nối tới " + serverIp);
                screenPanel = new ScreenPanel(commandSender); // ScreenPanel dùng 'commandSender'
                frame.add(screenPanel);

                frame.setSize(1280, 720);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                screenPanel.requestFocusInWindow();

                // Thêm WindowListener để đóng socket khi tắt cửa sổ
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                        try {
                            if (finalCommandSocket != null) finalCommandSocket.close();
                            // videoSocket sẽ tự đóng khi luồng kia kết thúc
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                });
            });

            // Luồng nhận hình ảnh (Dùng videoSocket)
            FFmpegFrameGrabber grabber = null;
            Java2DFrameConverter converter = null;

            try {
                converter = new Java2DFrameConverter();
                // SỬA LỖI: Đọc từ videoSocket.getInputStream()
                grabber = new FFmpegFrameGrabber(videoSocket.getInputStream());

                // (Code cấu hình grabber của bạn giữ nguyên)
                grabber.setFormat("flv");
                grabber.setOption("probesize", "5000");
                grabber.setOption("analyzeduration", "50000");

                grabber.start();
                System.out.println("Bắt đầu nhận stream video...");

                Frame receivedFrame;
                while (videoSocket.isConnected() && (receivedFrame = grabber.grab()) != null) {
                    BufferedImage image = converter.convert(receivedFrame);
                    if (image != null && screenPanel != null) {
                        SwingUtilities.invokeLater(() -> screenPanel.updateScreen(image));
                    }
                }
            } catch (Exception e) {
                System.out.println("Lỗi luồng nhận video hoặc server đã ngắt kết nối.");
                // Không cần in stack trace ở đây trừ khi debug
            } finally {
                try {
                    if (converter != null) converter.close();
                    if (grabber != null) grabber.close();
                    if (videoSocket != null) videoSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("Đã dừng luồng nhận video.");
            }

        } catch (Exception e) {
            System.out.println("Lỗi client hoặc mất kết nối server (ví dụ: sai mật khẩu hoặc IP).");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Không thể kết nối đến server tại IP: " + serverIp, "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        } finally {
            // Đảm bảo đóng commandSocket khi kết thúc
            try {
                if (commandSocket != null) commandSocket.close();
            } catch (Exception e) { e.printStackTrace(); }
            System.out.println("Đã đóng kết nối lệnh.");
        }
    }
}
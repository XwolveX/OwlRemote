package server;

import common.ZeroTierManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Random;

public class ServerApp {
    public static void start(String networkId,String apiKey) {
        ZeroTierManager ztManager = new ZeroTierManager();

        // --- 2. TỰ ĐỘNG JOIN VÀ LẤY IP HOST ---
        ztManager.joinNetwork(networkId);
        String hostIp = ztManager.getManagedIp(networkId);
        while (hostIp == null) {
            int result = JOptionPane.showConfirmDialog(null, "Không thể lấy IP Host. Đã cấp phép cho Server trên my.zerotier.com chưa?\nThử lại?", "Lỗi ZeroTier", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.NO_OPTION) System.exit(0);
            hostIp = ztManager.getManagedIp(networkId);
        }

        // --- 3. BẮT ĐẦU LUỒNG KIỂM TRA CẤP PHÉP (CHO CLIENT) ---
        // Dùng một Set để theo dõi các client đã hỏi
        Set<String> pendingClients = new HashSet<>();
        String finalHostIp = hostIp;
        new Thread(() -> {
            try {
                while (true) {
                    JSONArray members = ztManager.listMembers(networkId, apiKey);
                    if (members == null) {
                        Thread.sleep(10000); // 10 giây
                        continue;
                    }
                    for (int i = 0; i < members.length(); i++) {
                        JSONObject member = members.getJSONObject(i);
                        boolean isAuthorized = member.getJSONObject("config").getBoolean("authorized");
                        String memberId = member.getString("nodeId");

                        // Nếu chưa cấp phép VÀ chưa từng hỏi
                        if (!isAuthorized && !pendingClients.contains(memberId)) {
                            pendingClients.add(memberId); // Đánh dấu là đã hỏi
                            String clientName = member.optString("name", "Không rõ");

                            // Hiển thị popup trên luồng UI
                            SwingUtilities.invokeLater(() -> {
                                int result = JOptionPane.showConfirmDialog(null,
                                        "Phát hiện Client [" + clientName + "] (" + memberId + ") muốn tham gia.\n" +
                                                "Bạn có đồng ý cấp phép không?",
                                        "Yêu cầu Cấp phép",
                                        JOptionPane.YES_NO_OPTION);

                                if (result == JOptionPane.OK_OPTION) {
                                    System.out.println("Đang cấp phép cho: " + memberId);
                                    ztManager.authorizeMember(networkId, apiKey, memberId);
                                }
                            });
                        }
                    }
                    Thread.sleep(10000); // Kiểm tra lại sau 10 giây
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        try {
            String serverPassword = String.format("%06d", new Random().nextInt(999999));

            String message = String.format("SERVER SẴN SÀNG (ZEROTIER)\n\n" +
                            "Gửi thông tin này cho bạn bè:\n" +
                            "IP Server: %s\n" +
                            "MẬT KHẨU: %s\n" +
                            "Cổng: 12345",
                    finalHostIp, serverPassword);
            JOptionPane.showMessageDialog(null, message, "Server Information", JOptionPane.INFORMATION_MESSAGE);

            // Mở cổng và chờ kết nối
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Đang chờ client kết nối...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client đã kết nối! Đang chờ xác thực...");

            // Dùng BufferedReader để đọc lệnh (và mật khẩu)
            BufferedReader commandReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Đọc dòng đầu tiên client gửi và kiểm tra mật khẩu
            String clientPassword = commandReader.readLine();
            if(clientPassword == null || !clientPassword.equals(serverPassword)) {
                System.out.println("Xác thực thất bại! Mật khẩu sai. Đóng kết nối.");
                clientSocket.close();
                return;
            }
            System.out.println("Xác thực thành công!");
            // --- KẾT THÚC XÁC THỰC ---


            Robot robot = new Robot();

            // --- Luồng 1: Gửi màn hình liên tục  ---
            new Thread(() -> {
                try {
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

                    // Chuẩn bị ImageWriter để tối ưu chất lượng ảnh
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                    ImageWriter writer = writers.next();
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed()) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(0.8f); // 80% chất lượng, giảm băng thông
                    }

                    while (clientSocket.isConnected()) {
                        BufferedImage screenshot = robot.createScreenCapture(screenRect);

                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {

                            writer.setOutput(ios);
                            writer.write(null, new javax.imageio.IIOImage(screenshot, null, null), param);

                            byte[] imageBytes = baos.toByteArray();
                            dos.writeInt(imageBytes.length);
                            dos.write(imageBytes);
                            dos.flush();
                        }
                        Thread.sleep(16); // Khoảng 60 FPS
                    }
                    writer.dispose();
                } catch (Exception e) {
                    System.out.println("Lỗi luồng gửi màn hình hoặc client đã ngắt kết nối.");
                }
            }).start();

            // --- Luồng 2 (Luồng chính): Nhận và xử lý lệnh điều khiển ---
            String command;
            while ((command = commandReader.readLine()) != null) {
                String[] parts = command.split(",");
                String action = parts[0];

                try {
                    switch (action) {
                        case "MOUSE_MOVE":
                            robot.mouseMove(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                            break;
                        case "MOUSE_PRESS":
                            robot.mousePress(InputEvent.getMaskForButton(Integer.parseInt(parts[3])));
                            break;
                        case "MOUSE_RELEASE":
                            robot.mouseRelease(InputEvent.getMaskForButton(Integer.parseInt(parts[3])));
                            break;
                        case "MOUSE_WHEEL":
                            robot.mouseWheel(Integer.parseInt(parts[1]));
                            break;
                        case "KEY_PRESS":
                            robot.keyPress(Integer.parseInt(parts[1]));
                            break;
                        case "KEY_RELEASE":
                            robot.keyRelease(Integer.parseInt(parts[1]));
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi thực thi lệnh: " + command);
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi server hoặc client đã ngắt kết nối.");
            e.printStackTrace();
        }
    }
}
package server;

import common.ZeroTierManager;
import org.json.JSONArray;
import org.json.JSONObject;

// Java CV library
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.io.DataOutputStream;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class ServerApp {

    private static final int COMMAND_PORT = 12345;
    private static final int VIDEO_PORT = 12346;
    // --- ZeroTier ---
    public static void start(String networkId, String apiKey) {
        ZeroTierManager ztManager = new ZeroTierManager();
        ztManager.joinNetwork(networkId);
        String hostIp = ztManager.getManagedIp(networkId);
        while (hostIp == null) {
            int result = JOptionPane.showConfirmDialog(null, "Không thể lấy IP Host. Đã cấp phép cho Server trên my.zerotier.com chưa?\nThử lại?", "Lỗi ZeroTier", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.NO_OPTION) System.exit(0);
            hostIp = ztManager.getManagedIp(networkId);
        }
        String finalHostIp = hostIp;
        new Thread(() -> {
            try {
                Set<String> pendingClients = new HashSet<>();
                while (true) {
                    JSONArray members = ztManager.listMembers(networkId, apiKey);
                    if (members != null) {
                        for (int i = 0; i < members.length(); i++) {
                            JSONObject member = members.getJSONObject(i);
                            boolean isAuthorized = member.getJSONObject("config").getBoolean("authorized");
                            String memberId = member.getString("nodeId");
                            if (!isAuthorized && !pendingClients.contains(memberId)) {
                                pendingClients.add(memberId);
                                String clientName = member.optString("name", "Không rõ");
                                SwingUtilities.invokeLater(() -> {
                                    int result = JOptionPane.showConfirmDialog(null,
                                            "Phát hiện Client [" + clientName + "] (" + memberId + ") muốn tham gia.\n" +
                                                    "Bạn có đồng ý cấp phép không?",
                                            "Yêu cầu Cấp phép",
                                            JOptionPane.YES_NO_OPTION);
                                    if (result == JOptionPane.OK_OPTION) {
                                        ztManager.authorizeMember(networkId, apiKey, memberId);
                                    }
                                });
                            }
                        }
                    }
                    Thread.sleep(10000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        // --- ZeroTier ---


        // Khai báo các socket bên ngoài try-catch để đóng trong finally
        ServerSocket commandSocketServer = null;
        Socket commandSocket = null;
        ServerSocket videoSocketServer = null;
        Socket videoSocket = null;

        try {
            String serverPassword = String.format("%06d", new Random().nextInt(999999));
            String message = String.format("SERVER SẴN SÀNG (ZEROTIER)\n\n" +
                            "Gửi thông tin này cho bạn bè:\n" +
                            "IP Server: %s\n" +
                            "MẬT KHẨU: %s\n" +
                            "Cổng (Lệnh): %d\n" + // Cổng chính cho xác thực
                            "Cổng (Video): %d",   // Cổng phụ cho video
                    finalHostIp, serverPassword, COMMAND_PORT, VIDEO_PORT);
            JOptionPane.showMessageDialog(null, message, "Server Information", JOptionPane.INFORMATION_MESSAGE);

            // --- THIẾT LẬP 2 SOCKET ---

            // 1. Mở Socket Lệnh (Command)
            commandSocketServer = new ServerSocket(COMMAND_PORT);
            System.out.println("Đang chờ client kết nối lệnh (cổng " + COMMAND_PORT + ")...");
            commandSocket = commandSocketServer.accept();
            System.out.println("Client lệnh đã kết nối! Đang chờ xác thực...");

            // 2. Xác thực qua Socket Lệnh
            BufferedReader commandReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
            String clientPassword = commandReader.readLine();

            if (clientPassword == null || !clientPassword.equals(serverPassword)) {
                System.out.println("Xác thực thất bại! Mật khẩu sai. Đóng kết nối.");
                commandSocket.close();
                commandSocketServer.close();
                return;
            }
            System.out.println("Xác thực thành công!");
            // 3. Gửi kích thước màn hình cho Client qua socket LỆNH
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            DataOutputStream commandDos = new DataOutputStream(commandSocket.getOutputStream());
            commandDos.writeInt(screenRect.width);
            commandDos.writeInt(screenRect.height);
            commandDos.flush(); // Gửi đi ngay lập tức
            System.out.println("Đã gửi kích thước màn hình: " + screenRect.width + "x" + screenRect.height);

            // 4. Mở Socket Video (CHỈ SAU KHI XÁC THỰC)
            videoSocketServer = new ServerSocket(VIDEO_PORT);
            System.out.println("Đang chờ client kết nối video (cổng " + VIDEO_PORT + ")...");
            videoSocket = videoSocketServer.accept();
            System.out.println("Client video đã kết nối!");

            Robot robot = new Robot();

            // --- Luồng 1: Gửi màn hình (Dùng videoSocket) ---
            final Socket finalVideoSocket = videoSocket; // Cần biến final để dùng trong lambda
            new Thread(() -> {
                FFmpegFrameRecorder recorder = null;
                Java2DFrameConverter converter = null;
                try {
                    converter = new Java2DFrameConverter();
                    recorder = new FFmpegFrameRecorder(
                            finalVideoSocket.getOutputStream(),
                            screenRect.width,
                            screenRect.height
                    );

                    recorder.setFormat("flv");
                    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                    recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
                    recorder.setFrameRate(30);
                    recorder.setVideoBitrate(2000 * 1024);
                    recorder.setVideoOption("tune", "zerolatency");
                    recorder.setVideoOption("preset", "ultrafast");

                    recorder.start();
                    System.out.println("Bắt đầu stream video...");

                    while (finalVideoSocket.isConnected()) {
                        BufferedImage screenshot = robot.createScreenCapture(screenRect);
                        Frame frame = converter.convert(screenshot);
                        recorder.record(frame);
                    }
                } catch (Exception e) {
                    System.out.println("Lỗi luồng gửi màn hình hoặc client đã ngắt kết nối.");
                } finally {
                    try {
                        if (converter != null) converter.close();
                        if (recorder != null) recorder.close(); // Tự động stop/release
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println("Đã dừng luồng stream video.");
                }
            }).start();

            // --- Luồng 2 (Luồng chính): Nhận lệnh điều khiển (Dùng commandReader) ---
            String command;
            while ((command = commandReader.readLine()) != null) {
                // (Code switch-case của bạn giữ nguyên)
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
        } finally {
            // Đảm bảo đóng tất cả socket
            try {
                if(commandSocket != null) commandSocket.close();
                if(commandSocketServer != null) commandSocketServer.close();
                if(videoSocket != null) videoSocket.close();
                if(videoSocketServer != null) videoSocketServer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
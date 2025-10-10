package server;

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

public class ServerApp {
    public static void main(String[] args) {
        try {
            // Hiển thị IP của Server để Client biết đường kết nối
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            String message = String.format("Server đang chạy tại IP: %s\nChờ client kết nối tại cổng: 12345", ipAddress);
            JOptionPane.showMessageDialog(null, message, "Server Information", JOptionPane.INFORMATION_MESSAGE);

            // Mở cổng và chờ kết nối
            ServerSocket serverSocket = new ServerSocket(12345);
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client đã kết nối!");

            Robot robot = new Robot();

            // --- Luồng 1: Gửi màn hình liên tục (Đã tối ưu) ---
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
                        param.setCompressionQuality(0.7f); // 70% chất lượng, giảm băng thông
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
            BufferedReader commandReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
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
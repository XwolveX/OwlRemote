package server;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {
    public static void main(String[] args) {
        try {
            // Mở cổng và chờ kết nối
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Server đang chờ client kết nối tại cổng 12345...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client đã kết nối!");

            Robot robot = new Robot();

            // --- Luồng 1: Gửi màn hình liên tục ---
            new Thread(() -> {
                try {
                    DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                    Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

                    while (clientSocket.isConnected()) {
                        BufferedImage screenshot = robot.createScreenCapture(screenRect);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(screenshot, "jpeg", baos);
                        byte[] imageBytes = baos.toByteArray();

                        dos.writeInt(imageBytes.length);
                        dos.write(imageBytes);
                        dos.flush();

                        Thread.sleep(16); // Khoảng 60 FPS
                    }
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
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            robot.mouseMove(x, y);
                            break;
                        case "MOUSE_PRESS":
                            int buttonPress = Integer.parseInt(parts[3]);
                            robot.mousePress(InputEvent.getMaskForButton(buttonPress));
                            break;
                        case "MOUSE_RELEASE":
                            int buttonRelease = Integer.parseInt(parts[3]);
                            robot.mouseRelease(InputEvent.getMaskForButton(buttonRelease));
                            break;
                        case "MOUSE_WHEEL":
                            int rotation = Integer.parseInt(parts[1]);
                            robot.mouseWheel(rotation);
                            break;
                        case "KEY_PRESS":
                            int keyPressCode = Integer.parseInt(parts[1]);
                            robot.keyPress(keyPressCode);
                            break;
                        case "KEY_RELEASE":
                            int keyReleaseCode = Integer.parseInt(parts[1]);
                            robot.keyRelease(keyReleaseCode);
                            break;
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi thực thi lệnh: " + command);
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("Lỗi server hoặc client đã ngắt kết nối.");
            e.printStackTrace();
        }
    }
}
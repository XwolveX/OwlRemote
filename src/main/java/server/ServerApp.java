package server;

import common.ZeroTierManager;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class ServerApp {

    private static final int COMMAND_PORT = 12345;
    private static final int VIDEO_PORT = 12346;

    public static void start(String networkId, String apiKey) {
        ZeroTierManager ztManager = new ZeroTierManager();
        ztManager.joinNetwork(networkId);

        String hostIp = ztManager.getManagedIp(networkId);
        while (hostIp == null) {
            int result = JOptionPane.showConfirmDialog(null,
                    "Cannot get Host IP. Retry?", "ZeroTier Error",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.NO_OPTION) System.exit(0);
            hostIp = ztManager.getManagedIp(networkId);
        }

        String finalHostIp = hostIp;

        // --- Monitor clients waiting for authentication ---
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
                                SwingUtilities.invokeLater(() -> {
                                    int r = JOptionPane.showConfirmDialog(null,
                                            "Client " + memberId + " requests access. Authorize?",
                                            "ZeroTier", JOptionPane.YES_NO_OPTION);
                                    if (r == JOptionPane.YES_OPTION)
                                        ztManager.authorizeMember(networkId, apiKey, memberId);
                                });
                            }
                        }
                    }
                    Thread.sleep(10000);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        try (ServerSocket cmdServer = new ServerSocket(COMMAND_PORT);
             ServerSocket videoServer = new ServerSocket(VIDEO_PORT)) {

            String password = String.format("%06d", new Random().nextInt(999999));
            JOptionPane.showMessageDialog(null,
                    "SERVER READY\n\nIP: " + finalHostIp +
                            "\nPASSWORD: " + password +
                            "\nCommand Port: " + COMMAND_PORT +
                            "\nVideo Port: " + VIDEO_PORT,
                    "Server Info", JOptionPane.INFORMATION_MESSAGE);

            System.out.println("[Server] Waiting for command connection...");
            Socket cmdSocket = cmdServer.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(cmdSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(cmdSocket.getOutputStream(), true);

            String clientPass = reader.readLine();
            if (!password.equals(clientPass)) {
                System.out.println("[Server] Wrong password, closing.");
                cmdSocket.close(); return;
            }

            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            DataOutputStream cmdOut = new DataOutputStream(cmdSocket.getOutputStream());
            cmdOut.writeInt(screenRect.width);
            cmdOut.writeInt(screenRect.height);
            cmdOut.flush();

            System.out.println("[Server] Waiting for video connection...");
            Socket videoSocket = videoServer.accept();
            System.out.println("[Server] Video client connected!");

            Robot robot = new Robot();
            DataOutputStream videoOut = new DataOutputStream(
                    new BufferedOutputStream(videoSocket.getOutputStream()));

            // Set up JPEG compression
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter jpgWriter = writers.next();
            ImageWriteParam param = jpgWriter.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.8f); // 0.0–1.0 (image quality)

            // --- Thread sends frame ---
            new Thread(() -> {
                try {
                    while (true) {
                        BufferedImage capture = robot.createScreenCapture(screenRect);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        jpgWriter.setOutput(new MemoryCacheImageOutputStream(baos));
                        jpgWriter.write(null, new javax.imageio.IIOImage(capture, null, null), param);
                        byte[] bytes = baos.toByteArray();
                        videoOut.writeInt(bytes.length);
                        videoOut.write(bytes);
                        videoOut.flush();
                        System.out.println("[Server] Sent frame (" + bytes.length / 1024 + " KB)");
                        Thread.sleep(1000 / 60); // ~60fps
                    }
                } catch (Exception e) {
                    System.err.println("[Server] Stream stopped: " + e.getMessage());
                }
            }).start();

            // --- Receive control commands ---
            String cmd;
            while ((cmd = reader.readLine()) != null) {
                try {
                    String[] p = cmd.split(",");
                    switch (p[0]) {
                        case "MOUSE_MOVE" -> robot.mouseMove(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                        case "MOUSE_PRESS" -> robot.mousePress(InputEvent.getMaskForButton(Integer.parseInt(p[3])));
                        case "MOUSE_RELEASE" -> robot.mouseRelease(InputEvent.getMaskForButton(Integer.parseInt(p[3])));
                        case "KEY_PRESS" -> robot.keyPress(Integer.parseInt(p[1]));
                        case "KEY_RELEASE" -> robot.keyRelease(Integer.parseInt(p[1]));
                    }
                } catch (Exception ex) {
                    System.err.println("[Server] Invalid cmd: " + cmd);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

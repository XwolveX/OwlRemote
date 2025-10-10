package server;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ServerApp {

    public static void main(String[] args) {
        try {
            // 1. Khởi tạo Robot để tương tác với màn hình
            Robot robot = new Robot();
            String format = "jpg";
            String fileName = "screenshot." + format;

            // 2. Lấy kích thước toàn bộ màn hình
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            // 3. Chụp màn hình
            BufferedImage screenCapture = robot.createScreenCapture(screenRect);

            // 4. Lưu ảnh vừa chụp vào một file
            ImageIO.write(screenCapture, format, new File(fileName));

            System.out.println("Đã chụp và lưu màn hình thành công vào file: " + fileName);

        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}

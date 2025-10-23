package common;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ZeroTierManager {

    private static String authToken;
    private final HttpClient client = HttpClient.newHttpClient();

    public ZeroTierManager() {
        try {
            this.authToken = findAuthToken();
        } catch (Exception e) {
            System.err.println("Không thể tìm thấy authtoken.secret. ZeroTier đã được cài đặt chưa?");
            this.authToken = null;
        }
    }

    /**
     * Đọc file config.properties
     */
    public static Properties readConfig(String filePath) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            props.load(fis);
        } catch (Exception e) {
            System.err.println("Không tìm thấy file: " + filePath);
        }
        return props;
    }

    /**
     * Tìm auth token trên máy local
     */
    private String findAuthToken() throws Exception {
        String tokenPathStr = "";
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            tokenPathStr = "C:\\ProgramData\\ZeroTier\\One\\authtoken.secret";
        } else if (os.contains("mac")) {
            tokenPathStr = "/Library/Application Support/ZeroTier/One/authtoken.secret";
        } else if (os.contains("nix") || os.contains("nux")) {
            tokenPathStr = "/var/lib/zerotier-one/authtoken.secret";
        } else {
            throw new Exception("Hệ điều hành không được hỗ trợ");
        }
        return Files.readString(Path.of(tokenPathStr)).trim();
    }

    /**
     * Gửi yêu cầu tham gia mạng (Local API)
     */
    public boolean joinNetwork(String networkId) {
        if (authToken == null) return false;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:9993/network/" + networkId))
                    .header("X-ZT1-Auth", authToken)
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Lỗi khi join mạng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy IP ảo hiện tại của máy (Local API)
     */
    public String getManagedIp(String networkId) {
        if (authToken == null) return null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:9993/network/" + networkId))
                    .header("X-ZT1-Auth", authToken)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return null;

            JSONObject json = new JSONObject(response.body());
            if (json.has("assignedAddresses")) {
                JSONArray addresses = json.getJSONArray("assignedAddresses");
                if (addresses.length() > 0) {
                    return addresses.getString(0).split("/")[0]; // Lấy IP, bỏ /mask
                }
            }
        } catch (Exception e) {
            // ...
        }
        return null; // Chưa được cấp IP
    }

    // --- CÁC HÀM API TRUNG TÂM (CHỈ DÙNG CHO SERVER) ---

    /**
     * Lấy danh sách thành viên trong mạng (Central API)
     */
    public JSONArray listMembers(String networkId, String apiKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://my.zerotier.com/api/network/" + networkId + "/member"))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return new JSONArray(response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cấp phép cho một thành viên (Central API)
     */
    public boolean authorizeMember(String networkId, String apiKey, String memberId) {
        try {
            // Body của request chỉ cần "authorized: true"
            String jsonBody = new JSONObject().put("config", new JSONObject().put("authorized", true)).toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://my.zerotier.com/api/network/" + networkId + "/member/" + memberId))
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
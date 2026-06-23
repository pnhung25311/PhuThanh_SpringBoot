package com.example.apiServer.controller.file;

import com.example.apiServer.model.file.FileItem;
import com.example.apiServer.service.file.FileService;
import com.example.apiServer.service.warehouse.DynamicTableService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api") // Cấu trúc định tuyến thống nhất cho ApiClient Flutter
public class FileController {

    @Autowired
    private FileService fileService;
    @Autowired
    private DynamicTableService dynamicTableService;

    /**
     * Hàm tiện ích: Lấy tự động Account ID từ Spring Security JWT Context
     */
    private int getCurrentAccountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new RuntimeException("Chưa đăng nhập hệ thống!");
        }

        String username = auth.getName(); // Lấy tên đăng nhập (Ví dụ: "admin", "hungit")
        System.out.println("Username lấy từ Token là: " + username);

        // Truy vấn bảng người dùng đang hoạt động để lấy AccountID kiểu số nguyên
        Object accountIdObj = dynamicTableService.findActiveUserByUser(username).get("AccountID");
        if (accountIdObj == null) {
            throw new RuntimeException("Không tìm thấy ID tương ứng với tài khoản: " + username);
        }
        return Integer.parseInt(accountIdObj.toString());
    }

    /**
     * 1. API LẤY CÂY THƯ MỤC HOẶC Ổ ĐĨA GỐC
     * URL: GET /api/files/tree?path=drive-c/Thư mục test&test con
     */
    @GetMapping("/files/tree")
    public ResponseEntity<List<FileItem>> getTree(HttpServletRequest request) throws IOException {
        int accountId = getCurrentAccountId(); // Tự động nhận diện ID gác cổng từ token

        // Lấy trọn vẹn chuỗi Query thô đằng sau dấu ?
        String queryString = request.getQueryString();
        String path = "";

        // Bóc tách an toàn tuyệt đối chuỗi path nằm sau cụm "path=" (Bảo vệ dấu & và
        // khoảng trắng)
        if (queryString != null && queryString.contains("path=")) {
            path = queryString.substring(queryString.indexOf("path=") + 5);
        }

        // Nếu path rỗng hoặc khoảng trắng, tự động tải danh sách ổ đĩa gốc được cấp
        // quyền xem
        if (path.isBlank()) {
            return ResponseEntity.ok(fileService.getAvailableDrivesAsItems(accountId));
        }

        // Vào sâu bên trong cấu trúc cây thư mục
        return ResponseEntity.ok(fileService.getTree(accountId, path));
    }

    /**
     * 2. API UPLOAD TỆP TIN VẬT LÝ (Kiểm tra phân quyền writeAccountIds)
     * URL: POST /api/file/upload
     */
    @PostMapping("/file/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("path") String remotePath,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Dữ liệu tệp tải lên trống!");
            }

            int accountId = getCurrentAccountId(); // Lấy ID tài khoản an toàn từ Token

            String aliasName = "";
            String targetPath = "";

            // Phân tách AliasName và RelativePath từ chuỗi remotePath
            if (remotePath.contains("/")) {
                int firstSlash = remotePath.indexOf("/");
                aliasName = remotePath.substring(0, firstSlash);
                targetPath = remotePath.substring(firstSlash + 1);
            } else {
                aliasName = remotePath;
            }

            // Gọi hàm xử lý ghi đè/tạo mới dữ liệu vật lý đệ quy an toàn
            fileService.storeFile(accountId, aliasName, targetPath, file.getOriginalFilename(), file.getBytes());
            return ResponseEntity.ok("Upload file lên hệ thống Phú Thành thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Tải lên thất bại: " + e.getMessage());
        }
    }

    /**
     * 3. API DOWNLOAD FILE VẬT LÝ
     * URL: GET /api/file/download?path=drive-c/Folder/file.txt
     */
    @GetMapping("/file/download")
    public ResponseEntity<?> downloadFile(HttpServletRequest request) {
        try {
            int accountId = getCurrentAccountId();

            // 1. Lấy trọn vẹn chuỗi Query thô đằng sau dấu ? (Giúp bảo vệ dấu & không bị
            // cắt đoạn)
            String queryString = request.getQueryString();
            String rawPath = "";

            if (queryString != null && queryString.contains("path=")) {
                rawPath = queryString.substring(queryString.indexOf("path=") + 5);
                // Nếu có tham số khác đi kèm phía sau, hãy cắt bỏ nó
                if (rawPath.contains("&") && !rawPath.contains("path=" + rawPath)) {
                    // Chỉ cắt nếu dấu & đó thuộc về một tham số khác, không phải của tên thư mục
                    // Tuy nhiên vì Flutter chỉ truyền duy nhất path, ta giải mã trực tiếp rawPath
                    // luôn là an toàn nhất
                }
            }

            if (rawPath.isBlank()) {
                return ResponseEntity.badRequest().body("Đường dẫn tải file không hợp lệ!");
            }

            // 2. Giải mã URL thủ công (Biến %C3%B2ng và %20 thành chữ tiếng Việt và khoảng
            // trắng chuẩn)
            String decodedPath = java.net.URLDecoder.decode(rawPath, java.nio.charset.StandardCharsets.UTF_8);

            String aliasName = "";
            String relativePath = "";
            if (decodedPath.contains("/")) {
                int firstSlash = decodedPath.indexOf("/");
                aliasName = decodedPath.substring(0, firstSlash);
                relativePath = decodedPath.substring(firstSlash + 1);
            } else {
                aliasName = decodedPath;
            }

            // 3. Lấy file vật lý từ ổ đĩa
            Path filePhysical = fileService.getFileForDownload(accountId, aliasName, relativePath);
            byte[] data = Files.readAllBytes(filePhysical);

            // Mã hóa lại tên file ở Header để trình duyệt/thiết bị di động hiển thị đúng
            // tiếng Việt có dấu
            String fileNameEncoded = java.net.URLEncoder
                    .encode(filePhysical.getFileName().toString(), java.nio.charset.StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileNameEncoded)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Không thể tải file: " + e.getMessage());
        }
    }

    /**
     * 4. API XÓA FILE HOẶC THƯ MỤC VẬT LÝ (Kiểm tra phân quyền writeAccountIds đệ
     * quy ngược)
     * URL: DELETE /api/file/delete?path=drive-c/Folder/Mục cần xóa
     */
    @DeleteMapping("/file/delete")
    public ResponseEntity<?> deleteFile(HttpServletRequest request) {
        try {
            int accountId = getCurrentAccountId(); // Lấy ID tự động từ Token, triệt tiêu lỗ hổng bypass ID

            // Đọc Query string thô đằng sau dấu ?
            String queryString = request.getQueryString();
            String path = "";

            if (queryString != null && queryString.contains("path=")) {
                path = queryString.substring(queryString.indexOf("path=") + 5);
            }

            if (path.isBlank()) {
                return ResponseEntity.badRequest().body("Đường dẫn tệp hoặc thư mục cần xóa không hợp lệ!");
            }

            // Thực thi lệnh xóa vật lý đệ quy qua Service
            boolean isDeleted = fileService.deleteFileOrFolder(accountId, path);

            if (isDeleted) {
                return ResponseEntity.ok("Xóa mục dữ liệu vật lý thành công!");
            } else {
                return ResponseEntity.status(500).body("Máy chủ từ chối hành động xóa vật lý ngoài ổ đĩa!");
            }

        } catch (Exception e) {
            return ResponseEntity.status(403).body("Lỗi khi thực hiện xóa: " + e.getMessage());
        }
    }

    /**
     * 5. API LẤY DANH SÁCH Ổ ĐĨA GỐC KHÔNG QUA TRUYỀN ĐƯỜNG DẪN
     */
    @GetMapping("/files/drives")
    public ResponseEntity<List<FileItem>> getDrives() {
        int accountId = getCurrentAccountId();
        return ResponseEntity.ok(fileService.getAvailableDrivesAsItems(accountId));
    }

    /**
     * 6. API TẢI BẢN CẬP NHẬT PHẦN MỀM DESKTOP HỆ THỐNG WMS
     */
    @GetMapping("/file/update/download-version")
    public ResponseEntity<Resource> downloadVersion() {
        try {
            Resource resource = fileService.loadUpdateFile();
            String contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * 4b. API SAO CHÉP (COPY) FILE HOẶC THƯ MỤC
     * Endpoint: POST /api/files/copy?sourcePath=...&targetDirectoryPath=...
     */
    @PostMapping("/files/copy")
    public ResponseEntity<String> copyFileOrFolder(
            @RequestParam("sourcePath") String sourcePath,
            @RequestParam("targetDirectoryPath") String targetDirectoryPath) {
        try {
            int accountId = getCurrentAccountId();
            fileService.copyFileOrFolder(accountId, sourcePath, targetDirectoryPath);
            return ResponseEntity.ok("Sao chép dữ liệu thành công!");
        } catch (Exception e) {
            // Trả về mã lỗi 403 nếu vi phạm phân quyền canWrite hoặc không tìm thấy đường dẫn
            return ResponseEntity.status(403).body("Lỗi khi sao chép: " + e.getMessage());
        }
    }

    /**
     * 4c. API DI CHUYỂN / CẮT (MOVE / CUT) FILE HOẶC THƯ MỤC
     * Endpoint: POST /api/files/move?sourcePath=...&targetDirectoryPath=...
     */
    @PostMapping("/files/move")
    public ResponseEntity<String> moveFileOrFolder(
            @RequestParam("sourcePath") String sourcePath,
            @RequestParam("targetDirectoryPath") String targetDirectoryPath) {
        try {
            int accountId = getCurrentAccountId();
            fileService.moveFileOrFolder(accountId, sourcePath, targetDirectoryPath);
            return ResponseEntity.ok("Di chuyển dữ liệu thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Lỗi khi di chuyển: " + e.getMessage());
        }
    }

    // API tạo thư mục mới
    @PostMapping("/file/create-folder")
    public ResponseEntity<?> createFolder(
            @RequestBody Map<String, String> payload) {
        try {
            String parentPath = payload.get("parentPath");
            String folderName = payload.get("folderName");
            int accountId = Integer.parseInt( payload.get("accountId"));
            fileService.createFolder(accountId, parentPath, folderName);
            return ResponseEntity.ok("Tạo thư mục thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // API đổi tên file hoặc thư mục
    @PostMapping("/file/rename")
    public ResponseEntity<?> renameItem(
            @RequestBody Map<String, String> payload) {
        try {
            String currentPath = payload.get("currentPath");
            String newName = payload.get("newName");
            int accountId = Integer.parseInt( payload.get("accountId"));
            fileService.renameItem(accountId, currentPath, newName);
            return ResponseEntity.ok("Đổi tên thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
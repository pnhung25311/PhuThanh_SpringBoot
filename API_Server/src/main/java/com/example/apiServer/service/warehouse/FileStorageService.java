package com.example.apiServer.service.warehouse;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

@Service
public class FileStorageService {

    // Thư mục thực tế lưu file trên server
    private static final String BASE_DIR = "E:/Images Server/PYS Images/";
    private static final String BASE_GUARANTEE = "E:/Images Server/PYS Images-Guarantee/";

    // URL public trả về client (khớp với ResourceHandler)
    private static final String BASE_URL = "http://192.168.1.54:2010/PYS Images";
    private static final String BASE_URL_GUARANTEE = "http://192.168.1.54:2010/PYS Images-Guarantee";

    // Upload file
    // Upload file với logic xóa file trùng tên gốc
    public String uploadFile(MultipartFile file, String productID) throws IOException {
        Path productDir = Paths.get(BASE_DIR, productID);
        if (!Files.exists(productDir)) {
            Files.createDirectories(productDir);
        }

        // Lấy tên gốc của file upload
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.lastIndexOf('.') != -1) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }

        // Kiểm tra xem có file trùng tên gốc không
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(productDir)) {
            for (Path entry : stream) {
                String existingName = entry.getFileName().toString();
                if (existingName.equals(originalName)) {
                    Files.delete(entry); // Xóa file cũ
                    System.out.println("🗑 Deleted old file: " + existingName);
                    break;
                }
            }
        }

        // Nếu tên file gốc trùng thì giữ nguyên tên, nếu không thì đặt theo
        // img{n}.{ext}
        Path newFilePath;
        if (originalName != null) {
            newFilePath = productDir.resolve(originalName);
        } else {
            // Lấy danh sách index hiện có
            Set<Integer> existingIndexes = new HashSet<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(productDir)) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString();
                    if (name.matches("img\\d+(\\..+)?")) {
                        String numPart = name.replaceAll("img(\\d+)(\\..*)?", "$1");
                        try {
                            existingIndexes.add(Integer.parseInt(numPart));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            int nextIndex = 1;
            while (existingIndexes.contains(nextIndex)) {
                nextIndex++;
            }
            String newFileName = "img" + nextIndex + extension;
            newFilePath = productDir.resolve(newFileName);
        }

        // Lưu file lên ổ đĩa
        Files.copy(file.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);

        // URL trả về client
        String fileUrl = BASE_URL + "/" + productID + "/" + newFilePath.getFileName();
        System.out.println("✅ Uploaded to: " + fileUrl);
        return fileUrl;
    }

    public String uploadFileGuarantee(MultipartFile file, String productID) throws IOException {
        Path productDir = Paths.get(BASE_GUARANTEE, productID);
        if (!Files.exists(productDir)) {
            Files.createDirectories(productDir);
        }

        // Lấy tên gốc của file upload
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.lastIndexOf('.') != -1) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }

        // Kiểm tra xem có file trùng tên gốc không
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(productDir)) {
            for (Path entry : stream) {
                String existingName = entry.getFileName().toString();
                if (existingName.equals(originalName)) {
                    Files.delete(entry); // Xóa file cũ
                    System.out.println("🗑 Deleted old file: " + existingName);
                    break;
                }
            }
        }

        // Nếu tên file gốc trùng thì giữ nguyên tên, nếu không thì đặt theo
        // img{n}.{ext}
        Path newFilePath;
        if (originalName != null) {
            newFilePath = productDir.resolve(originalName);
        } else {
            // Lấy danh sách index hiện có
            Set<Integer> existingIndexes = new HashSet<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(productDir)) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString();
                    if (name.matches("img\\d+(\\..+)?")) {
                        String numPart = name.replaceAll("img(\\d+)(\\..*)?", "$1");
                        try {
                            existingIndexes.add(Integer.parseInt(numPart));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            int nextIndex = 1;
            while (existingIndexes.contains(nextIndex)) {
                nextIndex++;
            }
            String newFileName = "img" + nextIndex + extension;
            newFilePath = productDir.resolve(newFileName);
        }

        // Lưu file lên ổ đĩa
        Files.copy(file.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);

        // URL trả về client
        String fileUrl = BASE_URL_GUARANTEE + "/" + productID + "/" + newFilePath.getFileName();
        System.out.println("✅ Uploaded to: " + fileUrl);
        return fileUrl;
    }

    // Xóa file theo tên (img1, img2,...)
    public void deleteFile(String productID, String fileNameWithoutExt) throws IOException {
        Path productDir = Paths.get(BASE_DIR, productID);
        if (!Files.exists(productDir)) {
            throw new IOException("Thư mục không tồn tại: " + productDir);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(productDir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (name.startsWith(fileNameWithoutExt + ".")) {
                    Files.delete(entry);
                    System.out.println("🗑 Deleted file: " + entry);
                    return;
                }
            }
        }

        throw new IOException("Không tìm thấy file có tên bắt đầu bằng: " + fileNameWithoutExt);
    }

    // Xóa file theo tên (img1, img2,...)
    public void deleteFileGuarantee(String productID, String fileNameWithoutExt) throws IOException {
        Path productDir = Paths.get(BASE_GUARANTEE, productID);
        if (!Files.exists(productDir)) {
            throw new IOException("Thư mục không tồn tại: " + productDir);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(productDir)) {
            for (Path entry : stream) {
                String name = entry.getFileName().toString();
                if (name.startsWith(fileNameWithoutExt + ".")) {
                    Files.delete(entry);
                    System.out.println("🗑 Deleted file: " + entry);
                    return;
                }
            }
        }

        throw new IOException("Không tìm thấy file có tên bắt đầu bằng: " + fileNameWithoutExt);
    }
}

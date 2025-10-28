package com.example.apiServer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

@Service
public class FileStorageService {

    // Thư mục thực tế lưu file trên server
    private static final String BASE_DIR = "D:/Data/Storage/images";

    // URL public trả về client (khớp với ResourceHandler)
    private static final String BASE_URL = "http://192.168.1.11:8080/images";

    // Upload file
    public String uploadFile(MultipartFile file, String productID) throws IOException {
        Path productDir = Paths.get(BASE_DIR, productID);
        if (!Files.exists(productDir)) {
            Files.createDirectories(productDir);
        }

        // Lấy danh sách file hiện có để tìm index img{n} tiếp theo
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

        // Lấy phần mở rộng gốc
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.lastIndexOf('.') != -1) {
            extension = originalName.substring(originalName.lastIndexOf('.'));
        }

        // Tên file: img{index}.{ext}
        String newFileName = "img" + nextIndex + extension;
        Path newFilePath = productDir.resolve(newFileName);

        // Lưu file lên ổ đĩa
        Files.copy(file.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);

        // URL trả về client (không nhân đôi /images)
        String fileUrl = BASE_URL + "/" + productID + "/" + newFileName;
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
}

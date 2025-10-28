package com.example.apiServer.controller;

import com.example.apiServer.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/")
public class FileUploadController {

    @Autowired
    private FileStorageService sftpService;

    // POST /api/sftp/upload/{productID}
    // Trả về plain String (URL) nếu thành công, hoặc lỗi (status 500 + message)
    @PostMapping("/upload/{productID}")
    public ResponseEntity<String> uploadFile(
            @PathVariable String productID,
            @RequestParam("file") MultipartFile file) {

        try {
            String fileUrl = sftpService.uploadFile(file, productID);
            // Trả về plain string URL
            return ResponseEntity.ok(fileUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // DELETE /api/sftp/delete/{productID}?fileName=img1.jpg
    // Trả về plain String message
    @DeleteMapping("/delete/{productID}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String productID,
            @RequestParam("fileName") String fileName) {

        try {
            sftpService.deleteFile(productID, fileName);
            return ResponseEntity.ok("Deleted: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error deleting file: " + e.getMessage());
        }
    }
}

package com.example.apiServer.controller.file;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

import java.io.*;

@RestController
@RequestMapping("/update")
public class FileController {

    @GetMapping("/download")
    public void download(HttpServletResponse response) throws IOException {

        File file = new File("I:\\Phòng IT-Hưng\\UpdateVersionApp\\PhuThanh_DesktopApp-0.0.1-SNAPSHOT.jar");        
        // File file = new File("C:\\project\\PhuThanh_WareHouseDeskTopApp\\PhuThanh_DesktopApp\\target\\PhuThanh_DesktopApp-0.0.1-SNAPSHOT.jar");


        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
        response.setContentLengthLong(file.length());

        try (InputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int len;

            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
    }
}
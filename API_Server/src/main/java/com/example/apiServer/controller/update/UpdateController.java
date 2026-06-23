package com.example.apiServer.controller.update;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.apiServer.service.warehouse.DynamicTableService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/update")
public class UpdateController {

    private final DynamicTableService dynamicTableService;

    public UpdateController(DynamicTableService dynamicTableService) {
        this.dynamicTableService = dynamicTableService;
    }

    @GetMapping("/info")
    public Map<String, Object> getUpdateInfo() {
        Map<String, Object> updateInfo = dynamicTableService.findOne("SELECT TOP 1 * FROM AppVersion ORDER BY AppVersionAID DESC", "AppVersion"); // Gọi service để lấy dữ liệu từ database (nếu cần)

        Map<String, Object> response = new HashMap<>();
        // String linkUpdate = "http://localhost:8080/update/download";
        String linkUpdate = "http://192.168.1.54:2010/api/file/update/download-version";

        response.put("latestVersion", updateInfo.get("Version"));
        response.put("downloadUrl", linkUpdate);
        response.put("changeLog", updateInfo.get("Note"));

        return response;
    }
}

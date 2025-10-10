package com.example.apiServer.controller;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.apiServer.model.ImportExport;
import com.example.apiServer.repository.ImportExportRepository;

@RestController
@RequestMapping("/importexport")
public class ImportExportController {

    private final ImportExportRepository repo;

    public ImportExportController(ImportExportRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/get-all")
    public List<ImportExport> getAll() {
        return repo.findAllNative();
        // return repo.findAll(); // ✅ Trả về JSON danh sách
    }

    @PostMapping("get-by-barcode")
    public List<ImportExport> getByBarcode(@RequestBody Map<String, String> body) {
        // String barcode = body.get("barcode");
        return repo.findByBarcode(body.get("barcode"));
    }

    @GetMapping("/debug")
    public String debug() {
        long count = repo.count();
        return "Record count: " + count;
    }

    @Autowired
    private DataSource dataSource;

    @GetMapping("/test-connection")
    public String testConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            return "Connected to: " + conn.getCatalog(); // tên DB
        }
    }

}

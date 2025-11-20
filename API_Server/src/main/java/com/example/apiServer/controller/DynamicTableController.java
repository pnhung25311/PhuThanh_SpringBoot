package com.example.apiServer.controller;

import com.example.apiServer.service.DynamicTableService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/dynamic")
public class DynamicTableController {
    private final DynamicTableService service;

    public DynamicTableController(DynamicTableService service) {
        this.service = service;
    }

    @GetMapping("/get-all/{table}")
    public List<Map<String, Object>> getAll(@PathVariable String table) {
        long start = System.currentTimeMillis(); // ⏱️ bắt đầu đo

        List<Map<String, Object>> result = service.findAll(table);

        long end = System.currentTimeMillis(); // ⏱️ kết thúc đo
        long duration = end - start;

        System.out.println("⏱️ Thời gian thực thi findAllPagedParallel: " + duration + " ms");

        return result;
    }

    @GetMapping("/get-all/{table}/{limit}")
    public List<Map<String, Object>> getAllLimit(
            @PathVariable String table,
            @PathVariable String limit) {

        return service.findAllLimit(table, limit);
    }

    @PostMapping({ "/insert/{table}", "/insert/{table}/{warehouse}" })
    public ResponseEntity<?> insert(
            @PathVariable String table,
            @RequestBody Map<String, Object> body,
            @PathVariable Optional<String> warehouse) {

        service.insert(table, body, warehouse.orElse("")); // 👈 dùng orElse
        return ResponseEntity.ok("Inserted successfully into " + table);
    }

    @PutMapping("/update/{table}/{keyColumn}/{id}")
    public ResponseEntity<?> update(
            @PathVariable String table,
            @PathVariable String keyColumn,
            @PathVariable Object id,
            @RequestBody Map<String, Object> body) {
        service.update(table, keyColumn, id, body);
        return ResponseEntity.ok("Updated successfully in " + table);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Map<String, Object> user = service.login(username, password);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Sai tên đăng nhập hoặc mật khẩu"));
        }

        return ResponseEntity.ok(user);
    }

    @PostMapping("/find/{tableName}")
    public Map<String, Object> findByColumn(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new RuntimeException("Request body is empty");
        }

        // Lấy cột và giá trị đầu tiên trong JSON
        String keyColumn = body.keySet().iterator().next();
        Object value = body.get(keyColumn);
        System.out.println(tableName);
        System.out.println(body);

        Map<String, Object> record = service.findByColumn(tableName, keyColumn, value);

        if (record == null) {
            throw new RuntimeException("Record not found in table: " + tableName);
        }

        return record;
    }

    @GetMapping("/search/{table}/{keyWord}")
    public List<Map<String, Object>> searchWareHouse(@PathVariable String table, @PathVariable String keyWord) {
        return service.searchWareHouse(table, keyWord);
    }

    // @GetMapping("/tables")
    // public List<Map<String, Object>> getTables() {
    // return service.getItemWarehouse();
    // }

    @GetMapping("/tables")
    public List<String> getTables() {
        return service.getItemWarehouse();
    }

    @GetMapping("/find/{table}/{column}/{condition}")
    public List<Map<String, Object>> findItemByCondition(@PathVariable String table, @PathVariable String column,
            @PathVariable String condition) {
        return service.findItemByCondition(table, column, condition);
    }

    
    @GetMapping("/find-history/{table}/{column}/{condition}")
    public List<Map<String, Object>> findItemByConditionHistory(@PathVariable String table, @PathVariable String column,
            @PathVariable String condition) {
        return service.findItemByConditionHistory(table, column, condition);
    }

    @GetMapping("/get-all/pages/{table}")
    public ResponseEntity<List<Map<String, Object>>> findAll(
            @PathVariable String table,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "8") int threads) {

        long start = System.currentTimeMillis(); // ⏱️ bắt đầu đo
        try {
            List<Map<String, Object>> result = service.findAllPagedParallel(table, size, threads);
            long end = System.currentTimeMillis(); // ⏱️ kết thúc đo
            long duration = end - start;

            System.out.println("⏱️ Thời gian thực thi findAllPagedParallel: " + duration + " ms");

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            long end = System.currentTimeMillis();
            System.err.println("❌ Lỗi sau " + (end - start) + " ms: " + e.getMessage());

            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of(Map.of("error", e.getMessage())));
        }
    }

    @PostMapping("/check-exists/{table}")
    public boolean checkExists(
            @PathVariable String table,
            @RequestBody Map<String, Object> body) {

        String value = body.get("ProductID").toString();
        return service.checkByProductID(table, value);
    }

}

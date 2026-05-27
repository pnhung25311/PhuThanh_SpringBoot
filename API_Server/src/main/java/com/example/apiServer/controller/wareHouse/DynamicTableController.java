package com.example.apiServer.controller.wareHouse;

import com.example.apiServer.security.JwtUtil;
import com.example.apiServer.service.warehouse.DynamicTableService;

import org.springframework.web.bind.annotation.DeleteMapping;
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

    @GetMapping("/version-warehouse")
    public Map<String, String> getVersion() {
        return service.getVersionAPP();
    }

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
        System.out.println("Limit received: " + limit);

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

    @PostMapping({ "/insert-batch/{table}", "/insert-batch/{table}/{warehouse}" })
    public ResponseEntity<?> insertBatch(
            @PathVariable String table,
            @RequestBody List<Map<String, Object>> body,
            @PathVariable Optional<String> warehouse) {

        try {
            service.insertBatch(table, body);

            return ResponseEntity.ok(
                    Map.of(
                            "statusCode", 200,
                            "message", "Inserted batch successfully into " + table,
                            "rows", body.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "statusCode", 500,
                            "message", e.getMessage()));
        }
    }

    @PostMapping({ "/insert/Cart" })
    public ResponseEntity<?> insertCart(
            @RequestBody Map<String, Object> body,
            @PathVariable Optional<String> warehouse) {

        service.createOrder(body); // 👈 dùng orElse
        return ResponseEntity.ok("Inserted successfully into ");
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

    @PutMapping("/confirm-cart/{userId}/{id}")
    public ResponseEntity<?> confirmCart(@PathVariable String userId, @PathVariable Object id, @RequestBody Map<String, Object> body) {
        // TODO: process POST request
        service.confirmCart(userId, id, body);
        return ResponseEntity.ok("Updated successfully in Cart");
    }

    @DeleteMapping("/delete/{tableName}")
    public ResponseEntity<?> delete(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> body) {
        try {
            service.delete(tableName, body);
            return ResponseEntity.ok("Delete success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update-batch/{table}/{keyColumn}")
    public ResponseEntity<?> updateBatchDynamic(
            @PathVariable String table,
            @PathVariable String keyColumn,
            @RequestBody List<Map<String, Object>> body) {

        int updated = service.updateBatchDynamic(table, keyColumn, body);

        return ResponseEntity.ok(Map.of(
                "updated", updated,
                "table", table));
    }

    /*
     * body JSON:
     * {
     * "ids": [1, 2, 3],
     * "data": {
     * "status": "DONE",
     * "last_user": "hung01"
     * }
     * }
     */
    @PutMapping("/update-in/{table}/{keyColumn}")
    public ResponseEntity<?> updateIn(
            @PathVariable String table,
            @PathVariable String keyColumn,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> ids = (List<Object>) body.get("ids");

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");

        if (ids == null || ids.isEmpty())
            return ResponseEntity.badRequest().body("ids rỗng");

        if (data == null || data.isEmpty())
            return ResponseEntity.badRequest().body("data rỗng");

        int updated = service.updateDynamicIn(table, keyColumn, ids, data);

        return ResponseEntity.ok(
                Map.of(
                        "table", table,
                        "updatedRows", updated));
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

        // ✅ tạo token
        String token = JwtUtil.generateToken(
                username,
                Map.of(
                        "accountID", user.get("AccountID"),
                        "role", user.get("Role"),
                        "status", user.get("Status")));
        System.out.println("Generated token: " + token);

        return ResponseEntity.ok(
                Map.of(
                        "token", token,
                        "Account", user));
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

    @PostMapping("/findlist/{tableName}")
    public List<Map<String, Object>> findByColumnList(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            throw new RuntimeException("Request body is empty");
        }

        System.out.println("Table: " + tableName);
        System.out.println("Conditions: " + conditions);

        return service.findByColumnList(tableName, conditions);
    }

    @PostMapping("/findone/{tableName}")
    public Map<String, Object> findOneByColumnLists(
            @PathVariable String tableName,
            @RequestBody Map<String, Object> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            throw new RuntimeException("Request body is empty");
        }

        System.out.println("Table: " + tableName);
        System.out.println("Conditions: " + conditions);

        return service.findOneByColumnList(tableName, conditions);
    }

    // Trả về tất cả bản ghi thỏa mãn điều kiện
    @PostMapping("/find-array/{tableName}")
    public List<Map<String, Object>> findByColumnArray(
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

        // Lấy tất cả bản ghi từ service
        List<Map<String, Object>> records = service.findByColumnArray(tableName, keyColumn, value);

        return records; // trả về tất cả bản ghi
    }

    @GetMapping("/search/{table}/{keyWord}")
    public List<Map<String, Object>> searchWareHouse(@PathVariable String table, @PathVariable String keyWord) {
        return service.searchWareHouse(table, keyWord);
    }

    @GetMapping("/tables")
    public List<Map<String, Object>> getTables() {
        return service.getItemWarehouse();
    }

    // @GetMapping("/tables")
    // public List<String> getTables() {
    // return service.getItemWarehouse();
    // }

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

    @GetMapping("/getAID/{tableName}/{column}/{columnCondition}/{condition}")
    public int getReturnAID(
            @PathVariable String tableName,
            @PathVariable String column,
            @PathVariable String columnCondition,
            @PathVariable String condition) {
        return service.returnAID(tableName, column, columnCondition, condition);
    }

    @GetMapping("/returnQty/{tableName}/{condition}")
    public double returnQty(
            @PathVariable String tableName,
            @PathVariable String condition) {
        return service.returnQty(tableName, condition);
    }

}

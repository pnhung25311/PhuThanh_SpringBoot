package com.example.apiServer.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.example.apiServer.utils.SupportHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.Query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
// import java.util.HashMap;

@Service
@SuppressWarnings("unchecked") // Thêm vào để tắt cảnh báo kiểu không kiểm tra
public class DynamicTableService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    // Lấy toàn bộ dữ liệu của bảng
    public List<Map<String, Object>> findAll(String tableName) {
        String sql = (tableName.contains("WareHouse") || tableName.contains("Product"))
                ? "SELECT * FROM " + tableName + " ORDER BY LastTime DESC"
                : "SELECT * FROM " + tableName;
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        return SupportHelper.mapResults(entityManager, results, columns);
    }
    // Lấy toàn bộ dữ liệu của bảng có giới hạn

    public List<Map<String, Object>> findAllLimit(String tableName, String limit) {
        String sql = "SELECT TOP " + limit + " * FROM " + tableName + " ORDER BY LastTime DESC";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        return SupportHelper.mapResults(entityManager, results, columns);
    }

    // Thêm dữ liệu mới
    @Transactional
    public void insert(String tableName, Map<String, Object> data, String wh) {
        String columns = String.join(",", data.keySet());
        String values = data.keySet().stream()
                .map(k -> ":" + k)
                .collect(Collectors.joining(","));
        String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        Query query = entityManager.createNativeQuery(sql);
        data.forEach(query::setParameter);
        query.executeUpdate();
        String condition = "Datawarehouse";
        if (tableName.toLowerCase().contains(condition.toLowerCase())) {
            Object DataWareHouseAIDObj = data.get("DataWareHouseAID");
            if (DataWareHouseAIDObj != null) {
                String DataWareHouseAID = DataWareHouseAIDObj.toString();
                Double sumQty = SupportHelper.getSumQtyByProduct(entityManager, DataWareHouseAID, tableName);
                SupportHelper.updateWareHouseQty(entityManager, wh, DataWareHouseAID, sumQty);
            }
        }
    }

    // Cập nhật dữ liệu
    @Transactional
    public void update(String tableName, String keyColumn, Object keyValue, Map<String, Object> data) {
        data.remove(keyColumn);

        String setClause = data.keySet().stream()
                .map(k -> k + " = :" + k)
                .collect(Collectors.joining(", "));

        String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + keyColumn + " = :keyValue";
        Query query = entityManager.createNativeQuery(sql);
        data.forEach(query::setParameter);
        query.setParameter("keyValue", keyValue);

        query.executeUpdate();
    }

    // Kiểm tra thông tin đăng nhập
    public Map<String, Object> login(String username, String password) {
        String sql = "SELECT * FROM Account WHERE username = :username AND password = :password";
        Query query = entityManager.createNativeQuery(sql);

        query.setParameter("username", username);
        query.setParameter("password", password);

        List<Object[]> result = query.getResultList();

        if (result.isEmpty()) {
            return null; // Sai username hoặc password
        }

        // Lấy tên cột để ánh xạ kết quả
        List<String> columns = SupportHelper.getColumnNames(entityManager, "Account");
        List<Map<String, Object>> mapped = SupportHelper.mapResults(entityManager, result, columns);

        // Trả về user đầu tiên
        return mapped.get(0);
    }

    // Tìm dữ liệu theo cột khóa bất kỳ
    public Map<String, Object> findByColumn(String tableName, String keyColumn, Object value) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + keyColumn + " = :value";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("value", value);

        List<Object[]> results = query.getResultList();
        if (results.isEmpty()) {
            return Collections.emptyMap(); // hoặc return null;
        }

        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        List<Map<String, Object>> mapped = SupportHelper.mapResults(entityManager, results, columns);

        return mapped.get(0); // Trả về bản ghi đầu tiên
    }

    public List<Map<String, Object>> searchWareHouse(String tableName, String keyWord) {
        String sql = "EXEC dbo.SearchWareHouse " + tableName + ", '" + keyWord + "'";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        System.out.println(results);
        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        System.out.println(columns);

        return SupportHelper.mapResults(entityManager, results, columns);
    }

    public List<String> getItemWarehouse() {
        String sql = "SELECT name FROM sys.tables WHERE name LIKE 'DataWareHouse%'";
        Query query = entityManager.createNativeQuery(sql);

        return query.getResultList();

    }

    // public List<Map<String, Object>> getItemWarehouse() {
    // String sql = "SELECT * FROM WareHouseTable";
    // Query query = entityManager.createNativeQuery(sql);
    // List<Object[]> results = query.getResultList();
    // List<String> columns = SupportHelper.getColumnNames(entityManager,
    // "WareHouseTable");
    // return SupportHelper.mapResults(entityManager, results, columns);
    // // return query.getResultList();

    // }

    public List<Map<String, Object>> findItemByCondition(String table, String column, String condition) {
        String sql = "SELECT * FROM " + table + " WHERE " + column + " = " + "'" + condition + "'";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager, table);

        return SupportHelper.mapResults(entityManager, results, columns);
    }

    public List<Map<String, Object>> findItemByConditionHistory(String table, String column, String condition) {
        String sql = "SELECT * FROM " + table + " WHERE " + column + " = " + "'" + condition + "' ORDER BY Time";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager, table);

        return SupportHelper.mapResults(entityManager, results, columns);
    }

    public List<Map<String, Object>> findAllPagedParallel(String tableName, int size, int maxThreads)
            throws InterruptedException {
        // Đếm tổng số dòng
        String countSql = "SELECT COUNT(*) FROM " + tableName;
        Number totalElements = (Number) entityManager.createNativeQuery(countSql).getSingleResult();
        int totalPages = (int) Math.ceil(totalElements.doubleValue() / size);

        System.out.println("Total elements: " + totalElements + ", total pages: " + totalPages);

        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        List<CompletableFuture<List<Map<String, Object>>>> futures = new ArrayList<>();

        for (int page = 0; page < totalPages; page++) {
            final int currentPage = page;

            CompletableFuture<List<Map<String, Object>>> future = CompletableFuture.supplyAsync(() -> {
                EntityManager em = entityManagerFactory.createEntityManager();
                try {
                    int offset = currentPage * size;

                    String baseSql = "SELECT * FROM " + tableName;
                    if (tableName.contains("WareHouse") || tableName.contains("Product")) {
                        baseSql += " ORDER BY LastTime DESC";
                    } else {
                        baseSql += " ORDER BY (SELECT NULL)";
                    }

                    String pagedSql = baseSql + " OFFSET " + offset + " ROWS FETCH NEXT " + size + " ROWS ONLY";
                    Query query = em.createNativeQuery(pagedSql);
                    List<Object[]> results = query.getResultList();

                    List<String> columns = SupportHelper.getColumnNames(em, tableName);
                    return SupportHelper.mapResults(em, results, columns);
                } finally {
                    if (em.isOpen()) {
                        em.clear(); // Clear cache Hibernate
                        em.close(); // Close EntityManager
                    }
                }
            }, executor);

            futures.add(future);
        }

        // Gộp toàn bộ kết quả
        List<Map<String, Object>> allItems = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Shutdown executor và đợi hoàn tất
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }

        System.out.println("✅ Loaded " + allItems.size() + " records from " + totalPages + " pages.");
        return allItems;
    }

    public boolean checkByProductID(String table, String productCode) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE ProductID = '" + productCode + "'";
        Query query = entityManager.createNativeQuery(sql);

        Number count = (Number) query.getSingleResult();
        return count != null && count.intValue() > 0;
    }

}
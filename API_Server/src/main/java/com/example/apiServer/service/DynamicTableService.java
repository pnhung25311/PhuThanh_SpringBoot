package com.example.apiServer.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.apiServer.utils.SupportHelper;

import org.springframework.stereotype.Service;

import jakarta.persistence.Query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Transactional;

@Service
@SuppressWarnings("unchecked") // Thêm vào để tắt cảnh báo kiểu không kiểm tra
public class DynamicTableService {
    @PersistenceContext
    private EntityManager entityManager;

    // Lấy toàn bộ dữ liệu của bảng
    public List<Map<String, Object>> findAll(String tableName) {
        String sql = tableName.contains("WareHouse")
                ? "SELECT * FROM " + tableName + " ORDER BY LastModifiedTime DESC"
                : "SELECT * FROM " + tableName;
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        return SupportHelper.mapResults(entityManager, results, columns);
    }
    // Lấy toàn bộ dữ liệu của bảng có giới hạn

    public List<Map<String, Object>> findAllLimit(String tableName, String limit) {
        String sql = "SELECT TOP " + limit + " * FROM " + tableName;
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
        System.out.println("================================2");

        if (tableName.equalsIgnoreCase("History")) {
            System.out.println("================================1");

            Object productIdObj = data.get("productID");
            System.out.println(" ===> productIdObj: " + productIdObj);
            // String wh = data.get("wh").toString();
            // System.out.println(wh);
            if (productIdObj != null) {
                String productID = productIdObj.toString();
                Double sumQty = SupportHelper.getSumQtyByProduct(entityManager, productID);
                System.out.println(sumQty);
                SupportHelper.updateWareHouseQty(entityManager, wh, productID, sumQty);
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
            return null;
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
        String sql = "SELECT name FROM sys.tables WHERE name LIKE 'warehouse%'";
        Query query = entityManager.createNativeQuery(sql);

        return query.getResultList();

    }

    public List<Map<String, Object>> findItemByCondition(String table, String column, String condition) {
        String sql = "SELECT * FROM " + table + " WHERE " + column + " = " + "'" + condition + "'";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager, table);

        return SupportHelper.mapResults(entityManager, results, columns);
    }

}

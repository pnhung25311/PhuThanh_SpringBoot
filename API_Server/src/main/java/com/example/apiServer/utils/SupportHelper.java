package com.example.apiServer.utils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@SuppressWarnings("unchecked") // Thêm vào để tắt cảnh báo kiểu không kiểm tra
public class SupportHelper {

    public static Double getSumQtyByProduct(EntityManager entityManager, String productID) {
        String sqlSum = "SELECT SUM(qty) FROM History GROUP BY productID HAVING ProductID = '" + productID + "'";
        Query query = entityManager.createNativeQuery(sqlSum);
        Object result = query.getSingleResult();
        return result != null ? ((Number) result).doubleValue() : 0.0;
    }

    public static void updateWareHouseQty(EntityManager entityManager, String table, String productID, Double sumQty) {
        Query query = entityManager.createNativeQuery(
                "UPDATE " + table + " SET Qty = :qty, LastModifiedTime = :time WHERE productID = :id");
        query.setParameter("qty", sumQty);
        query.setParameter("time", LocalDate.now());
        query.setParameter("id", productID);
        query.executeUpdate();

    }

    // Lấy danh sách tên cột của bảng
    public static List<String> getColumnNames(EntityManager entityManager, String tableName) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = :table";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("table", tableName);

        return query.getResultList();
    }

    // Map kết quả SQL thành danh sách JSON-friendly
    public static List<Map<String, Object>> mapResults(EntityManager entityManager, List<Object[]> rows,
            List<String> cols) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < cols.size(); i++) {
                map.put(cols.get(i), row[i]);
            }
            list.add(map);
        }
        return list;
    }
}

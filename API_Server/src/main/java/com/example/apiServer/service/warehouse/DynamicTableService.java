package com.example.apiServer.service.warehouse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.example.apiServer.telegram.service.TelegramService;
import com.example.apiServer.utils.SupportHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.Query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
// import java.util.HashMap;
import java.util.HashMap;
import java.nio.file.Files;

@Service
@SuppressWarnings("unchecked") // Thêm vào để tắt cảnh báo kiểu không kiểm tra
public class DynamicTableService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;
    private final TelegramService telegramService;

    public DynamicTableService(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    private final String FILE_PATH_UPDATE = "L:\\Phòng CN&KT-Hưng\\update_version\\update_version.txt";

    public Map<String, String> getVersionAPP() {

        Map<String, String> result = new HashMap<>();

        try {

            List<String> lines = Files.readAllLines(Paths.get(FILE_PATH_UPDATE));

            for (String line : lines) {

                String[] parts = line.split(":");

                if (parts.length == 2) {
                    result.put(parts[0].trim(), parts[1].trim());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    // Lấy toàn bộ dữ liệu của bảng
    public List<Map<String, Object>> findAll(String tableName) {
        String sql = (tableName.contains("WareHouse") || tableName.contains("Product") || tableName.contains("Cart"))
                ? "SELECT * FROM " + tableName + " ORDER BY LastTime DESC"
                : "SELECT * FROM " + tableName;
        // String sql = "SELECT * FROM " + tableName + " ORDER BY LastTime DESC";
        System.out.println(sql);
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        return SupportHelper.mapResults(entityManager, results, columns);
    }
    // Lấy toàn bộ dữ liệu của bảng có giới hạn

    public List<Map<String, Object>> findAllLimit(String tableName, String limit) {
        String sql = "SELECT TOP " + limit + " * FROM " + tableName + " ORDER BY LastTime DESC";
        System.out.println(sql);
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
    }

    @Transactional
    public void insertBatch(String tableName, List<Map<String, Object>> rows) {

        if (rows == null || rows.isEmpty())
            return;

        Map<String, Object> firstRow = rows.get(0);
        List<String> columns = new ArrayList<>(firstRow.keySet());
        String columnSql = String.join(",", columns);

        StringBuilder valuesSql = new StringBuilder();
        Map<String, Object> allParams = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {

            Map<String, Object> row = rows.get(i);
            final int index = i;

            String rowParams = columns.stream()
                    .map(col -> {
                        String paramName = col + "_" + index;
                        allParams.put(paramName, row.get(col));
                        return ":" + paramName;
                    })
                    .collect(Collectors.joining(","));

            valuesSql.append("(").append(rowParams).append(")");
            if (i < rows.size() - 1)
                valuesSql.append(",");
        }

        String sql = "INSERT INTO " + tableName +
                " (" + columnSql + ") VALUES " + valuesSql;

        Query query = entityManager.createNativeQuery(sql);
        allParams.forEach(query::setParameter);
        query.executeUpdate();
    }

    // Cập nhật dữ liệu
    @Transactional
    public int update(String tableName, String keyColumn, Object keyValue, Map<String, Object> data) {

        if (data == null || data.isEmpty())
            return 0;

        // copy để KHÔNG làm bẩn data gốc
        Map<String, Object> updateData = new HashMap<>(data);
        updateData.remove(keyColumn);

        if (updateData.isEmpty())
            return 0;

        // ✅ Chuyển tất cả string "null" thành null thật
        updateData.replaceAll((k, v) -> {
            if (v instanceof String && "null".equals(v)) {
                return null;
            }
            return v;
        });

        // Tạo SET clause
        String setClause = updateData.keySet().stream()
                .map(k -> k + " = :" + k)
                .collect(Collectors.joining(", "));

        String sql = "UPDATE " + tableName +
                " SET " + setClause +
                " WHERE " + keyColumn + " = :keyValue";

        Query query = entityManager.createNativeQuery(sql);

        // bind giá trị (Hibernate sẽ convert null thành SQL NULL)
        updateData.forEach((k, v) -> query.setParameter(k, v));
        query.setParameter("keyValue", keyValue);

        return query.executeUpdate(); // trả về số dòng update
    }

    @Transactional
    public void delete(String tableName, Map<String, Object> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Delete must have conditions!");
        }

        String whereClause = conditions.keySet().stream()
                .map(k -> k + " = :" + k)
                .collect(Collectors.joining(" AND "));

        String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;

        Query query = entityManager.createNativeQuery(sql);
        conditions.forEach(query::setParameter);

        query.executeUpdate();
    }

    @Transactional
    public int updateDynamicIn(String tableName, String keyColumn, List<?> keyValues, Map<String, Object> updateData) {

        // 1️⃣ Validate
        if (tableName == null || tableName.isBlank())
            throw new IllegalArgumentException("tableName rỗng");

        if (keyColumn == null || keyColumn.isBlank())
            throw new IllegalArgumentException("keyColumn rỗng");

        if (keyValues == null || keyValues.isEmpty())
            return 0;

        if (updateData == null || updateData.isEmpty())
            return 0;

        // 2️⃣ Build SET clause dynamic
        String setClause = updateData.keySet().stream()
                .map(col -> col + " = :" + col)
                .collect(Collectors.joining(", "));
        System.out.println("setClause");
        System.out.println(setClause);

        // 3️⃣ Build SQL
        String sql = "UPDATE " + tableName +
                " SET " + setClause +
                " WHERE " + keyColumn + " IN (:keys)";
        System.out.println("SQL");
        System.out.println(sql);

        // 4️⃣ Create query
        Query query = entityManager.createNativeQuery(sql);

        // 5️⃣ Set params SET
        updateData.forEach(query::setParameter);

        // 6️⃣ Set params IN
        query.setParameter("keys", keyValues);

        // 7️⃣ Execute
        return query.executeUpdate();
    }

    @Transactional
    public int updateBatchDynamic(String tableName, String keyColumn, List<Map<String, Object>> rows) {

        if (rows == null || rows.isEmpty())
            return 0;

        int totalUpdated = 0;

        for (Map<String, Object> row : rows) {

            // 1️⃣ kiểm tra key
            if (!row.containsKey(keyColumn))
                continue;

            Object keyValue = row.get(keyColumn);

            // 2️⃣ copy để không làm bẩn data gốc
            Map<String, Object> updateData = new HashMap<>(row);
            updateData.remove(keyColumn);

            if (updateData.isEmpty())
                continue;

            // 3️⃣ build SET clause dynamic
            String setClause = updateData.keySet().stream()
                    .map(col -> col + " = :" + col)
                    .collect(Collectors.joining(", "));

            String sql = "UPDATE " + tableName +
                    " SET " + setClause +
                    " WHERE " + keyColumn + " = :keyValue";

            Query query = entityManager.createNativeQuery(sql);

            // 4️⃣ set params
            updateData.forEach(query::setParameter);
            query.setParameter("keyValue", keyValue);

            // 5️⃣ execute
            totalUpdated += query.executeUpdate();
        }

        return totalUpdated;
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

    public Map<String, Object> findOne(String sql, String tableName) {
        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();
        if (results.isEmpty()) {
            return Collections.emptyMap(); // hoặc return null;
        }

        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        List<Map<String, Object>> mapped = SupportHelper.mapResults(entityManager, results, columns);

        return mapped.get(0); // Trả về bản ghi đầu tiên
    }

    // Tìm dữ liệu theo cột khóa bất kỳ
    // Tìm dữ liệu theo cột khóa bất kỳ, trả về tất cả các bản ghi
    public List<Map<String, Object>> findByColumnArray(String tableName, String keyColumn, Object value) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + keyColumn + " = :value ORDER BY LastTime DESC";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("value", value);

        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        return SupportHelper.mapResults(entityManager, results, columns);
    }

    public List<Map<String, Object>> findByColumnList(
            String tableName,
            Map<String, Object> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Conditions must not be empty");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM dbo.").append(tableName).append(" WHERE ");

        int index = 0;
        for (String column : conditions.keySet()) {
            if (index > 0) {
                sql.append(" AND ");
            }
            // RTRIM để tránh lỗi CHAR/NCHAR
            sql.append("RTRIM(").append(column).append(") = :").append(column);
            index++;
        }

        sql.append(" ORDER BY LastTime DESC");

        Query query = entityManager.createNativeQuery(sql.toString());

        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            Object value = entry.getValue();

            // ÉP KIỂU STRING để tránh lỗi bind (SQL Server rất hay bị)
            String stringValue = value == null ? null : value.toString();

            query.setParameter(entry.getKey(), stringValue);

            System.out.println(
                    " - " + entry.getKey()
                            + " (" + (stringValue != null ? stringValue.getClass().getSimpleName() : "null")
                            + ") = " + stringValue);
        }

        List<Object[]> results = query.getResultList();

        System.out.println("Result size: " + results.size());
        System.out.println("========================================");

        if (results.isEmpty()) {
            System.out.println("bị rỗng");
            return Collections.emptyList();
        }
        System.out.println(results);

        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);
        System.out.println(SupportHelper.mapResults(entityManager, results, columns));
        return SupportHelper.mapResults(entityManager, results, columns);
    }

    public Map<String, Object> findOneByColumnList(
            String tableName,
            Map<String, Object> conditions) {

        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Conditions must not be empty");
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM dbo.")
                .append(tableName)
                .append(" WHERE ");

        int index = 0;
        for (String column : conditions.keySet()) {
            if (index > 0) {
                sql.append(" AND ");
            }
            // RTRIM để tránh lỗi CHAR / NCHAR
            sql.append("RTRIM(").append(column).append(") = :").append(column);
            index++;
        }

        sql.append(" ORDER BY LastTime DESC");

        Query query = entityManager.createNativeQuery(sql.toString());

        // bind parameter
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            Object value = entry.getValue();
            String stringValue = value == null ? null : value.toString();
            query.setParameter(entry.getKey(), stringValue);
        }

        // 🔒 chỉ lấy 1 dòng
        query.setMaxResults(1);

        List<Object[]> results = query.getResultList();

        if (results.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> columns = SupportHelper.getColumnNames(entityManager, tableName);

        List<Map<String, Object>> mapped = SupportHelper.mapResults(entityManager, results, columns);

        return mapped.get(0); // ✅ record mới nhất
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

    public List<Map<String, Object>> getItemWarehouse() {
        String sql = "SELECT * FROM WareHouseTable WHERE WareHouseShowHide IS NULL";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();
        List<String> columns = SupportHelper.getColumnNames(entityManager,
                "WareHouseTable");
        return SupportHelper.mapResults(entityManager, results, columns);
        // return query.getResultList();

    }

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

    public double returnQty(String table, String DataWareHouseAID) {
        String sqlSum = "SELECT COALESCE(SUM(qty), 0) FROM " + table + " WHERE DataWareHouseAID = :aid";
        Query query = entityManager.createNativeQuery(sqlSum);
        query.setParameter("aid", DataWareHouseAID);
        Object result = query.getSingleResult();
        return ((Number) result).doubleValue();

    }

    public int returnAID(
            String tableName,
            String column,
            String columnCondition,
            String codeAID) {
        try {
            String sql = "SELECT " + column +
                    " FROM " + tableName +
                    " WHERE " + columnCondition + " = ?";

            System.out.println(sql);

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, codeAID);

            Object result = query.getSingleResult();
            System.out.println(result);
            System.out.println("---------------------");
            return result != null ? Integer.parseInt(result.toString()) : 0;

        } catch (NoResultException e) {
            return 0; // không có dữ liệu
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public Map<String, Object> findActiveUserById(Long userId) {

        String sql = """
                    SELECT *
                    FROM Account
                    WHERE AccountID = :id
                      AND Status = :status
                """;
        System.out.println(sql);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", userId);
        query.setParameter("status", "ACTIVE_STATUS");
        List<Object[]> result = query.getResultList();

        if (result.isEmpty()) {
            return null; // user không tồn tại hoặc đã inactive
        }

        List<String> columns = SupportHelper.getColumnNames(entityManager, "Account");

        List<Map<String, Object>> mapped = SupportHelper.mapResults(entityManager, result, columns);

        return mapped.get(0);
    }

    @Transactional
    public void createOrder(Map<String, Object> data) {

        // 1. Gọi hàm insert generic của bạn
        insert("Cart", data, null);

        // 2. Lấy userId từ data
        Long userId = Long.valueOf(data.get("AccountID").toString());

        // 3. Lấy thông tin user
        Map<String, Object> user = findActiveUserById(userId);

        if (user == null) {
            throw new RuntimeException("User not found");
        }
        telegramService.sendToChat("-1003842084129", "Bạn vừa có 1 đơn hàng mới từ " + user.get("FullName"));
    }

    @Transactional
    public void confirmCart(String userId, Object id, Map<String, Object> data) {

        // 1. Gọi hàm insert generic của bạn
        update("Cart", "CartAID", id, data);

        // 2. Lấy userId từ data
        Long Id = Long.valueOf(userId.toString().trim());

        // 3. Lấy thông tin user
        Map<String, Object> user = findActiveUserById(Id);

        if (user == null) {
            throw new RuntimeException("User not found");
        }
        telegramService.sendToChat("5639159074", user.get("FullName") + " đã xác nhận đơn hàng " + id.toString());
    }

}
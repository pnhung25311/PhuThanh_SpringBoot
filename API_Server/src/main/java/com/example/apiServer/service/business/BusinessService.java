package com.example.apiServer.service.business;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BusinessService {

    // private final String FILE_PATH = "D:\\Data\\Danh muc.txt";
    // private final String FILE_PATH = "D:\\SERVER DATA\\TAFU DATA\\Fast and Orca\\Fast\\Items\\Danh muc.txt";
    private final String FILE_PATH = "D:\\SERVER DATA\\TAFU DATA\\Fast and Pys\\Fast\\Items\\Danh muc.txt";

    private final JdbcTemplate fastJdbcTemplate;

    public BusinessService(@Qualifier("fastJdbcTemplate") JdbcTemplate fastJdbcTemplate) {
        this.fastJdbcTemplate = fastJdbcTemplate;
    }

    public List<Map<String, String>> readFile() {

        List<Map<String, String>> result = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {

            // đọc header
            String headerLine = br.readLine();

            if (headerLine == null) {
                return result;
            }

            String[] headers = headerLine.split(";");

            String line;

while ((line = br.readLine()) != null) {
    // Nếu dòng trống hoàn toàn thì bỏ qua
    if (line.trim().isEmpty()) continue; 

    String[] values = line.split(";", -1);
    Map<String, String> row = new LinkedHashMap<>();

    // Sử dụng Math.min để tránh lỗi nếu values dài hơn hoặc ngắn hơn headers
    int limit = Math.min(headers.length, values.length);

    for (int i = 0; i < headers.length; i++) {
        // Trim() header để xóa khoảng trắng thừa nếu có
        String key = headers[i].trim(); 
        String value = "";
        
        if (i < values.length) {
            value = values[i].trim(); // Trim luôn giá trị để sạch dữ liệu
        }
        if (key.equals("Thông số kỹ thuật")) {
            System.out.println(value);
        }

        row.put(key, value);
    }

    // --- ĐOẠN DEBUG: Thêm dòng này vào để kiểm tra trên Console ---
    // System.out.println("DEBUG DÒNG: " + row); 
    // -------------------------------------------------------------

    result.add(row);
}

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public List<Map<String, Object>> getCT90() {

        String sql = "SELECT * FROM vwct90";

        return fastJdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getCT70Y() {

        String sql = "SELECT * FROM vwct70y";

        return fastJdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getCT90History(String maVT) {

        String sql = "SELECT * FROM dbo.vwct90 WHERE ma_vt LIKE ? AND sl_nhap > 0 AND gia > 10 AND ma_kh NOT LIKE '%KHO%'";

        return fastJdbcTemplate.queryForList(sql, "%" + maVT + "%");
    }

    public List<Map<String, Object>> getCT70YHistory(String maVT) {

        String sql = "SELECT * FROM dbo.vwct70y WHERE ma_vt LIKE ? AND sl_nhap > 0 AND gia > 10 AND ma_kh NOT LIKE '%KHO%'";

        return fastJdbcTemplate.queryForList(sql, "%" + maVT + "%");
    }

}
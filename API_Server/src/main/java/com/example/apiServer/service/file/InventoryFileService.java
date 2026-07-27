package com.example.apiServer.service.file;

import com.example.apiServer.model.warehouse.InventoryWarningModel;
import com.example.apiServer.repository.InventoryRepository;
import com.example.apiServer.repository.interfaceModel.StockProjection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

@Service
public class InventoryFileService {

    private final InventoryRepository inventoryRepository;

    public InventoryFileService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public void processMultipleCsvFiles(String inputFolderPath, String outputFolderPath) throws IOException {
        Path inputDir = Paths.get(inputFolderPath);
        Path baseOutputDir = Paths.get(outputFolderPath);

        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            throw new IOException("Không tìm thấy thư mục gốc tại: " + inputDir.toAbsolutePath());
        }

        if (!Files.exists(baseOutputDir)) {
            Files.createDirectories(baseOutputDir);
        }

        try (Stream<Path> walk = Files.walk(inputDir, 1)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                    .forEach(inputFilePath -> {
                        String fileName = inputFilePath.getFileName().toString();

                        // 1. Tách hãng xe từ tên file
                        String carBrand = extractCarBrandFromFileName(fileName);

                        // 2. Tạo đường dẫn thư mục con tương ứng hãng xe (VD: outputFolder/CAT/)
                        Path brandOutputDir = baseOutputDir.resolve(carBrand);

                        try {
                            if (!Files.exists(brandOutputDir)) {
                                Files.createDirectories(brandOutputDir);
                            }

                            // 3. Tạo đường dẫn file xuất chi tiết trong folder hãng
                            Path outputFilePath = brandOutputDir.resolve(fileName);

                            System.out
                                    .println("\n===> Bắt đầu xử lý file: " + fileName + " (Dòng xe: " + carBrand + ")");
                            processAndExportInventory(inputFilePath, outputFilePath);

                        } catch (IOException e) {
                            System.err.println("❌ Lỗi nghiêm trọng khi xử lý file " + fileName + ": " + e.getMessage());
                        }
                    });
        }

        System.out.println("\n🎉 HOÀN THÀNH TOÀN BỘ TIẾN TRÌNH XỬ LÝ HÀNG LOẠT!");
    }

    /**
     * Đọc CSV -> Gom Batch Query SQL 1 lần -> Map dữ liệu -> Xuất CSV
     */
    private void processAndExportInventory(Path inputPath, Path outputPath) throws IOException {
        List<InventoryWarningModel> processedItems = new ArrayList<>();
        Set<String> uniquePartNos = new LinkedHashSet<>();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .build();

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.IGNORE)
                .onUnmappableCharacter(CodingErrorAction.IGNORE);

        // 1. ĐỌC FILE CSV VÀ THU THẬP TẤT CẢ PART NO
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputPath.toFile()), decoder));
                CSVParser csvParser = csvFormat.parse(reader)) {

            for (CSVRecord record : csvParser) {
                if (record.size() < 8)
                    continue;

                InventoryWarningModel item = new InventoryWarningModel();
                String partNo = record.get(0).trim();

                item.setPartNo(partNo);
                item.setVietnameseName(record.get(1).trim());
                item.setEnglishName(record.get(2).trim());
                item.setDepartment(record.get(3).trim());
                item.setUnit(record.get(4).trim());
                item.setRemark(record.get(5).trim());
                item.setQuantity(parseIntegerOrDefault(record.get(6).trim(), 0));
                item.setMinStock(parseIntegerOrDefault(record.get(7).trim(), 0));
                item.setMaxStock(parseIntegerOrDefault(record.get(8).trim(), 0));

                processedItems.add(item);
                if (!partNo.isEmpty()) {
                    uniquePartNos.add(partNo);
                }
            }
        }

        // 2. TRUY VẤN BATCH DUY NHẤT VÀO DATABASE
        Map<String, StockProjection> stockMap = new HashMap<>();
        if (!uniquePartNos.isEmpty()) {
            try {
                List<String> partList = new ArrayList<>(uniquePartNos);
                int batchSize = 1000;

                for (int i = 0; i < partList.size(); i += batchSize) {
                    List<String> subList = partList.subList(i, Math.min(i + batchSize, partList.size()));
                    String joinedPartNos = String.join(",", subList);

                    List<StockProjection> stockResults = inventoryRepository.getStockDataBatch(joinedPartNos);
                    if (stockResults != null) {
                        for (StockProjection stock : stockResults) {
                            if (stock != null && stock.getPartNo() != null) {
                                stockMap.put(stock.getPartNo(), stock);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi truy vấn DB Batch: " + e.getMessage());
            }
        }

        // 3. GHÉP DỮ LIỆU TỒN KHO VÀO DANH SÁCH BAN ĐẦU
        for (InventoryWarningModel item : processedItems) {
            StockProjection stock = stockMap.get(item.getPartNo());
            if (stock != null) {
                item.setTotalStock(getSafeInteger(stock.getTotalStock()));
                item.setStockDetail(stock.getStockDetail() != null ? stock.getStockDetail() : "");
                item.setMainWarehouseStock(getSafeInteger(stock.getMainWarehouseStock()));
                item.setMineralWarehouseStock(getSafeInteger(stock.getMineralWarehouseStock()));
            } else {
                item.setTotalStock(0);
                item.setStockDetail("");
                item.setMainWarehouseStock(0);
                item.setMineralWarehouseStock(0);
            }
        }

        // 4. XUẤT FILE CSV UTF-8 CHUẨN (KÈM BOM)
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            writer.write('\ufeff');

            try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                csvPrinter.printRecord(
                        "PartNo",
                        "VietnameseName",
                        "EnglishName",
                        "Department",
                        "Unit",
                        "Remark",
                        "Quantity",
                        "MinStock",
                        "MaxStock",
                        "TotalStock",
                        "StockDetail",
                        "MainWarehouseStock",
                        "MineralWarehouseStock");

                for (InventoryWarningModel item : processedItems) {
                    csvPrinter.printRecord(
                            item.getPartNo(),
                            item.getVietnameseName(),
                            item.getEnglishName(),
                            item.getDepartment(),
                            item.getUnit(),
                            item.getRemark(),
                            item.getQuantity(),
                            item.getMinStock(),
                            item.getMaxStock(),
                            item.getTotalStock(),
                            item.getStockDetail() != null ? item.getStockDetail() : "",
                            item.getMainWarehouseStock(),
                            item.getMineralWarehouseStock());
                }
                csvPrinter.flush();
            }
        }

        System.out.println("--> Xuất file thành công tại: " + outputPath.toAbsolutePath());
    }

    public void processDynamicGroupingWithoutDb(String inputFolderPath, String outputFolderPath) throws IOException {
        Path inputDir = Paths.get(inputFolderPath);
        Path outputDir = Paths.get(outputFolderPath);

        if (!Files.exists(inputDir) || !Files.isDirectory(inputDir)) {
            throw new IOException("Không tìm thấy thư mục gốc tại: " + inputDir.toAbsolutePath());
        }

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        Map<String, Map<String, InventoryWarningModel>> brandDataGroupMap = new LinkedHashMap<>();

        try (Stream<Path> walk = Files.walk(inputDir, 1)) {
            walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                    .filter(path -> !path.getFileName().toString().toLowerCase().endsWith("_tonghop.csv"))
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        String carBrand = extractCarBrandFromFileName(fileName);

                        if (!carBrand.isEmpty()) {
                            brandDataGroupMap.putIfAbsent(carBrand, new LinkedHashMap<>());
                            System.out.println("--> Gom dữ liệu dòng xe [" + carBrand + "] từ file: " + fileName);
                            readAndAggregateToMap(filePath, brandDataGroupMap.get(carBrand));
                        }
                    });
        }

        if (brandDataGroupMap.isEmpty()) {
            System.out.println("⚠️ Không tìm thấy file CSV hợp lệ nào!");
            return;
        }

        for (Map.Entry<String, Map<String, InventoryWarningModel>> entry : brandDataGroupMap.entrySet()) {
            String carBrand = entry.getKey();
            Map<String, InventoryWarningModel> dataMap = entry.getValue();

            if (dataMap.isEmpty())
                continue;

            String outputFileName = carBrand + "_TongHop.csv";
            Path outputPath = outputDir.resolve(outputFileName);

            writeMapToCsvFile(outputPath, dataMap);
            System.out.println("✅ Đã xuất thành công file dòng xe [" + carBrand + "]: " + outputPath.toAbsolutePath());
        }

        System.out.println("\n🎉 HOÀN THÀNH TẤT CẢ TIẾN TRÌNH TỔNG HỢP!");
    }

    private String extractCarBrandFromFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty())
            return "UNKNOWN";
        String cleanName = fileName.replaceAll("(?i)\\.csv$", "").trim();
        String[] parts = cleanName.split("[\\s_\\-]+");
        return parts.length > 0 ? parts[0].toUpperCase() : cleanName.toUpperCase();
    }

    private void readAndAggregateToMap(Path inputPath, Map<String, InventoryWarningModel> targetMap) {
        // 1. Dùng CSVFormat cơ bản, không bật setHeader() để giữ chuẩn chỉ số cột theo
        // 0-indexed
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.IGNORE)
                .onUnmappableCharacter(CodingErrorAction.IGNORE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputPath.toFile()), decoder));
                CSVParser csvParser = csvFormat.parse(reader)) {

            boolean isFirstRecord = true;

            for (CSVRecord record : csvParser) {
                // Kiểm tra kích thước cột tối thiểu (ít nhất 9 cột từ 0 đến 8)
                if (record.size() < 9) {
                    continue;
                }

                String partNo = record.get(0).trim();

                // 2. Bỏ qua dòng Header nếu phát hiện dòng đầu tiên chứa tên tiêu đề chữ
                if (isFirstRecord) {
                    isFirstRecord = false;
                    // Nếu cột 0 chứa các từ khóa tiêu đề quen thuộc -> Bỏ qua dòng này
                    if (partNo.equalsIgnoreCase("PartNo") ||
                            partNo.equalsIgnoreCase("Danh điểm") ||
                            partNo.equalsIgnoreCase("Part No") ||
                            partNo.equalsIgnoreCase("STT")) {
                        continue;
                    }
                }

                if (partNo.isEmpty()) {
                    continue;
                }

                // 3. Đọc dữ liệu từ cột 6, 7, 8
                String newDept = record.get(3).trim();
                int quantity = parseIntegerOrDefault(record.get(6), 0); // Cột 6 (Số lượng)
                int minStock = parseIntegerOrDefault(record.get(7), 0); // Cột 7 (Tồn tối thiểu)
                int maxStock = parseIntegerOrDefault(record.get(8), 0); // Cột 8 (Tồn tối đa)

                if (targetMap.containsKey(partNo)) {
                    // ĐÃ TỒN TẠI -> TỔNG HỢP CỘT 6, 7, 8
                    InventoryWarningModel existingItem = targetMap.get(partNo);
                    // existingItem.setQuantity(existingItem.getQuantity() + quantity);
                    existingItem.setMinStock(existingItem.getMinStock() + minStock);
                    existingItem.setMaxStock(existingItem.getMaxStock() + maxStock);

                    // Xử lý nối chuỗi Department nếu khác nhau
                    String currentDept = existingItem.getDepartment();
                    if (currentDept == null) {
                        currentDept = "";
                    }

                    if (!newDept.isEmpty()) {
                        if (currentDept.isEmpty()) {
                            existingItem.setDepartment(newDept);
                        } else {
                            // Tách các bộ phận hiện tại ra để kiểm tra trùng lặp
                            List<String> existingDepts = Arrays.asList(currentDept.split("\\s*/\\s*"));
                            if (!existingDepts.contains(newDept)) {
                                existingItem.setDepartment(currentDept + " / " + newDept);
                            }
                        }
                    }
                } else {
                    // CHƯA TỒN TẠI -> TẠO MỚI
                    InventoryWarningModel item = new InventoryWarningModel();
                    item.setPartNo(partNo);
                    item.setVietnameseName(record.get(1).trim());
                    item.setEnglishName(record.get(2).trim());
                    item.setDepartment(record.get(3).trim());
                    item.setUnit(record.get(4).trim());
                    item.setRemark(record.get(5).trim());
                    item.setQuantity(quantity);
                    item.setMinStock(minStock);
                    item.setMaxStock(maxStock);

                    targetMap.put(partNo, item);
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc file " + inputPath.getFileName() + ": " + e.getMessage());
        }
    }

    private void writeMapToCsvFile(Path outputPath, Map<String, InventoryWarningModel> dataMap) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            writer.write('\ufeff');

            try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                csvPrinter.printRecord(
                        "Danh điểm",
                        "Tên tiếng Việt",
                        "Tên tiếng Anh",
                        "Bộ phận",
                        "Đơn vị tính",
                        "Ghi chú",
                        "Số lượng",
                        "Tồn tối thiểu",
                        "Tồn tối đa");

                for (InventoryWarningModel item : dataMap.values()) {
                    csvPrinter.printRecord(
                            item.getPartNo(),
                            item.getVietnameseName(),
                            item.getEnglishName(),
                            item.getDepartment(),
                            item.getUnit(),
                            item.getRemark(),
                            item.getQuantity(),
                            item.getMinStock(),
                            item.getMaxStock());
                }
                csvPrinter.flush();
            }
        }
    }

    private int parseIntegerOrDefault(String val, int defaultValue) {
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            // Loại bỏ khoảng trắng, dấu phẩy phân cách hàng nghìn (ví dụ: "1,000" ->
            // "1000")
            String cleanVal = val.replaceAll("[,\\s]", "");

            // Nếu chứa dấu chấm thập phân (ví dụ "10.0" -> lấy 10)
            if (cleanVal.contains(".")) {
                return (int) Math.round(Double.parseDouble(cleanVal));
            }

            return Integer.parseInt(cleanVal);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getSafeInteger(Integer value) {
        return value != null ? value : 0;
    }
}
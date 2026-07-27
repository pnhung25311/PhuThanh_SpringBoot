package com.example.apiServer.controller.wareHouse;

import java.io.IOException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.apiServer.service.file.InventoryFileService;

@Component
public class InventoryWarning {

    private final InventoryFileService inventoryFileService;

    public InventoryWarning(InventoryFileService inventoryFileService) {
        this.inventoryFileService = inventoryFileService;
    }

    @Scheduled(fixedRate = 600000)
    public void runEveryTenMinutes() {
        // String input = "D:\\Data\\filecsv\\input";
        // String output = "D:\\Data\\filecsv\\output";
        String input = "D:\\SERVER DATA\\InventoryWarning\\input";
        String output = "D:\\SERVER DATA\\InventoryWarning\\output";
        try {
            inventoryFileService.processDynamicGroupingWithoutDb(input, input);
            System.out.println("Tổng hợp thành công các file");

            inventoryFileService.processMultipleCsvFiles(input, output);
            System.out.println("Ghi thành công các file");
        } catch (IOException e) {
            System.err.println("Lỗi xử lý file CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
package com.example.apiServer.repository.interfaceModel;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.beans.factory.annotation.Value;

public interface StockProjection {

    // Lấy mã danh điểm để map dữ liệu Batch
    // @Value("#{target.PartNo}")
    String getPartNo();

    // Map cột 'Total' trong SQL vào hàm getTotalStock()
    // @Value("#{target.Total}")
    Integer getTotalStock(); 

    // Map cột 'TotalResult' trong SQL vào hàm getStockDetail()
    // @Value("#{target.TotalResult}")
    String getStockDetail();

    // Map cột 'TotalC' trong SQL vào hàm getMainWarehouseStock()
    // @Value("#{target.TotalC}")
    Integer getMainWarehouseStock();

    // Map cột 'TotalKS' trong SQL vào hàm getMineralWarehouseStock()
    // @Value("#{target.TotalKS}")
    Integer getMineralWarehouseStock();
}
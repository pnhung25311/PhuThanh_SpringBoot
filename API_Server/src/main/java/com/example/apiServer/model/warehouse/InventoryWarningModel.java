package com.example.apiServer.model.warehouse;

public class InventoryWarningModel {
    // DANH ĐIỂM
    private String partNo;

    // Tên tiếng việt
    private String vietnameseName;

    // Tên tiếng anh
    private String englishName;

    // BỘ PHẬN
    private String department;

    // Đơn vị tính
    private String unit;

    // Số lượng
    private int quantity;

    // Giới hạn tồn tối thiểu
    private int minStock;

    // Giới hạn tồn tối đa
    private int maxStock;

    //Ghi chú
    private String remark;

    // Tổng tồn kho (Kho chính + Kho Khoáng sản)
    private int totalStock;

    // Chi tiết tồn kho
    private String stockDetail;

    // Kho chính
    private int mainWarehouseStock;

    // Kho Khoáng sản
    private int mineralWarehouseStock;

    public InventoryWarningModel() {
    }

    public InventoryWarningModel(String partNo,
                         String vietnameseName,
                         String englishName,
                         String department,
                         String unit,
                         int quantity,
                         int minStock,
                         int maxStock,
                         String remark,
                         int totalStock,
                         String stockDetail,
                         int mainWarehouseStock,
                         int mineralWarehouseStock) {
        this.partNo = partNo;
        this.vietnameseName = vietnameseName;
        this.englishName = englishName;
        this.department = department;
        this.unit = unit;
        this.quantity = quantity;
        this.minStock = minStock;
        this.maxStock = maxStock;
        this.remark = remark;
        this.totalStock = totalStock;
        this.stockDetail = stockDetail;
        this.mainWarehouseStock = mainWarehouseStock;
        this.mineralWarehouseStock = mineralWarehouseStock;
    }

    public String getPartNo() {
        return partNo;
    }

    public void setPartNo(String partNo) {
        this.partNo = partNo;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }

    public void setVietnameseName(String vietnameseName) {
        this.vietnameseName = vietnameseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public int getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(int maxStock) {
        this.maxStock = maxStock;
    }

    public int getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(int totalStock) {
        this.totalStock = totalStock;
    }

    public String getStockDetail() {
        return stockDetail;
    }

    public void setStockDetail(String stockDetail) {
        this.stockDetail = stockDetail;
    }

    public int getMainWarehouseStock() {
        return mainWarehouseStock;
    }

    public void setMainWarehouseStock(int mainWarehouseStock) {
        this.mainWarehouseStock = mainWarehouseStock;
    }

    public int getMineralWarehouseStock() {
        return mineralWarehouseStock;
    }

    public void setMineralWarehouseStock(int mineralWarehouseStock) {
        this.mineralWarehouseStock = mineralWarehouseStock;
    }

    

    @Override
    public String toString() {
        return "InventoryItem{" +
                "partNo='" + partNo + '\'' +
                ", vietnameseName='" + vietnameseName + '\'' +
                ", englishName='" + englishName + '\'' +
                ", department='" + department + '\'' +
                ", unit='" + unit + '\'' +
                ", quantity=" + quantity +
                ", minStock=" + minStock +
                ", maxStock=" + maxStock +
                ", totalStock=" + totalStock +
                ", stockDetail='" + stockDetail + '\'' +
                ", mainWarehouseStock=" + mainWarehouseStock +
                ", mineralWarehouseStock=" + mineralWarehouseStock +
                '}';
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
package com.example.apiServer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// import org.springframework.stereotype.Repository;

import com.example.apiServer.repository.interfaceModel.DummyEntity;
import com.example.apiServer.repository.interfaceModel.StockProjection;

// Bạn có thể kế thừa từ bất kỳ Entity nào hiện có, hoặc tạo 1 entity dummy
// @Repository
public interface InventoryRepository extends JpaRepository<DummyEntity, Long> {

    // Gọi hàm SQL trả về bảng (Table-valued Function)
    @Query(value = "SELECT * FROM fn_SearchPartInWarehouses(:partNo)", nativeQuery = true)
    StockProjection getStockDataFromFunction(@Param("partNo") String partNo);

    // HÀM MỚI (Query hàng loạt - Batch): Truyền chuỗi danh sách PartNo phân cách bằng dấu phẩy
    @Query(nativeQuery = true, value = 
        "DECLARE @TablePart dbo.PartNoListType; " +
        "INSERT INTO @TablePart (PartNo) " +
        "SELECT DISTINCT value FROM STRING_SPLIT(:partNos, ','); " +
        "EXEC dbo.sp_GetStockDataInBatch @PartList = @TablePart;")
    List<StockProjection> getStockDataBatch(@Param("partNos") String partNos);
}
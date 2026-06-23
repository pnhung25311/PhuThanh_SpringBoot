package com.example.apiServer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.apiServer.model.file.FolderPermission;

import java.util.List;

public interface FolderPermissionRepository extends JpaRepository<FolderPermission, Long> {
    // Tìm tất cả các quyền của một Account cụ thể trên một Ổ đĩa cụ thể
    // List<FolderPermission> findByAccountIdAndShareRootId(int accountId, Long shareRootId);

    // List<FolderPermission> findByAccountId(Long accountId);

// ✅ HÀM MỚI 1: Tìm tất cả các thư mục mà User này có quyền xem dựa trên ShareRoot Id
    @Query("SELECT f FROM FolderPermission f WHERE f.shareRoot.id = :shareRootId " +
           "AND f.allowedAccountIds LIKE %:accountIdToken%")
    List<FolderPermission> findPermissionsForUser(@Param("shareRootId") Long shareRootId, 
                                                  @Param("accountIdToken") String accountIdToken);

    // ✅ HÀM MỚI 2: Tìm tất cả cấu hình quyền của User này trên toàn bộ hệ thống để lấy danh sách ổ đĩa gốc
    @Query("SELECT f FROM FolderPermission f WHERE f.allowedAccountIds LIKE %:accountIdToken%")
    List<FolderPermission> findAllAccessibleRoots(@Param("accountIdToken") String accountIdToken);
}
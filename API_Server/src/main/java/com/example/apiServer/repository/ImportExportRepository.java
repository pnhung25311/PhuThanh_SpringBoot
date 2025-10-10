package com.example.apiServer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.apiServer.model.ImportExport;

@Repository
public interface ImportExportRepository extends JpaRepository<ImportExport, Integer> {
    @Query(value = "SELECT * FROM ImportExport", nativeQuery = true)
    List<ImportExport> findAllNative();

    @Query(value = "SELECT * FROM ImportExport WHERE Barcode=:barcode", nativeQuery = true)
    List<ImportExport> findByBarcode(@Param("barcode") String barcode);

}

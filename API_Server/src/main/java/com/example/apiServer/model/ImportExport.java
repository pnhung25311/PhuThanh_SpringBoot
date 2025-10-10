package com.example.apiServer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ImportExport", schema = "dbo")
public class ImportExport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private int id;

    @Column(name = "ID_Product")
    private String idProduct;

    @Column(name = "Barcode")
    private String barcode;

    @Column(name = "Qty")
    private Double qty;

    @Column(name = "ID_User")
    private String idUser;

    @Column(name = "Remark")
    private String remark;

    @Column(name = "Time")
    private LocalDateTime time;

    @Column(name = "Time_Update")
    private LocalDateTime timeUpdate;

    @Column(name = "ID_Update")
    private String idUpdate;

    public ImportExport() {} // ✅ Bắt buộc phải có constructor trống

    // Getter/Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getIdProduct() { return idProduct; }
    public void setIdProduct(String idProduct) { this.idProduct = idProduct; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }

    public String getIdUser() { return idUser; }
    public void setIdUser(String idUser) { this.idUser = idUser; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getTime() { return time; }
    public void setTime(LocalDateTime time) { this.time = time; }

    public LocalDateTime getTimeUpdate() { return timeUpdate; }
    public void setTimeUpdate(LocalDateTime timeUpdate) { this.timeUpdate = timeUpdate; }

    public String getIdUpdate() { return idUpdate; }
    public void setIdUpdate(String idUpdate) { this.idUpdate = idUpdate; }
}

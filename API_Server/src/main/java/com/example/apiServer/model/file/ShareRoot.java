package com.example.apiServer.model.file;

import jakarta.persistence.*;

@Entity
@Table(name = "ShareRoots") // Khớp với tên bảng SQL viết hoa chữ cái đầu
public class ShareRoot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "AliasName", nullable = false, unique = true, length = 50)
    private String aliasName;

    @Column(name = "ActualPath", nullable = false, length = 255)
    private String actualPath;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAliasName() { return aliasName; }
    public void setAliasName(String aliasName) { this.aliasName = aliasName; }
    public String getActualPath() { return actualPath; }
    public void setActualPath(String actualPath) { this.actualPath = actualPath; }
}
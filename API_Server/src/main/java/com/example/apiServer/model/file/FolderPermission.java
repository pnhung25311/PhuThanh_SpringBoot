package com.example.apiServer.model.file;

import jakarta.persistence.*;

@Entity
@Table(name = "FolderPermissions")
public class FolderPermission {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ShareRootId", nullable = false)
    private ShareRoot shareRoot;

    @Column(name = "RelativePath", length = 255)
    private String relativePath;

    // Chuỗi lưu danh sách các ID tài khoản được quyền XEM (Ví dụ: ",4,5,22,")
    @Column(name = "AllowedAccountIds", length = 1000)
    private String allowedAccountIds;

    // Chuỗi lưu danh sách các ID tài khoản được quyền GHI (Ví dụ: ",4,")
    @Column(name = "WriteAccountIds", length = 1000)
    private String writeAccountIds;

    // --- GETTERS AND SETTERS ---
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public ShareRoot getShareRoot() { return shareRoot; }
    public void setShareRoot(ShareRoot shareRoot) { this.shareRoot = shareRoot; }
    
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    
    public String getAllowedAccountIds() { return allowedAccountIds; }
    public void setAllowedAccountIds(String allowedAccountIds) { this.allowedAccountIds = allowedAccountIds; }
    
    public String getWriteAccountIds() { return writeAccountIds; }
    public void setWriteAccountIds(String writeAccountIds) { this.writeAccountIds = writeAccountIds; }
}
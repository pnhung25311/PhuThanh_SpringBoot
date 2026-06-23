package com.example.apiServer.model.file;

import java.util.ArrayList;
import java.util.List;

public class FileItem {

    private String name;
    private String path;
    private boolean folder;
    private long size;
    private String formattedSize; // Thêm trường hiển thị size dạng dễ đọc (KB, MB, GB...)
    private List<FileItem> children = new ArrayList<>();
    private boolean hasChildren;
    private boolean isDirectory; // Đang đại diện cho canWrite dựa theo code cũ của bạn
    
    // THÊM CÁC THUỘC TÍNH MỚI
    private long lastModified;    // Ngày sửa đổi cuối (Timestamp)
    private long createdTime;     // Ngày tạo (Timestamp)
    private int childCount;       // Số lượng file/folder con bên trong (Nếu là folder)

    public FileItem() {
    }

    // Constructor cũ (giữ lại để tránh lỗi các hàm getRootDrives nếu có gọi)
    public FileItem(String name, String path, boolean folder, long size, boolean hasChildren, boolean isDirectory) {
        this.name = name;
        this.path = path;
        this.folder = folder;
        this.size = size;
        this.hasChildren = hasChildren;
        this.isDirectory = isDirectory;
    }

    // Getter và Setter cho các thuộc tính mới
    public String getFormattedSize() {
        return formattedSize;
    }

    public void setFormattedSize(String formattedSize) {
        this.formattedSize = formattedSize;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    // --- Các Getter/Setter cũ giữ nguyên ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public boolean isFolder() { return folder; }
    public void setFolder(boolean folder) { this.folder = folder; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public List<FileItem> getChildren() { return children; }
    public void setChildren(List<FileItem> children) { this.children = children; }
    public boolean isHasChildren() { return hasChildren; }
    public void setHasChildren(boolean hasChildren) { this.hasChildren = hasChildren; }
    public void setIsDirectory(boolean isDirectory) { this.folder = isDirectory; }
    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean isDirectory) { this.isDirectory = isDirectory; }
}
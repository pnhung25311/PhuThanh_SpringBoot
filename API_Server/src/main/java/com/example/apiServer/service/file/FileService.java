package com.example.apiServer.service.file;

import com.example.apiServer.model.file.FileItem;
import com.example.apiServer.model.file.FolderPermission;
import com.example.apiServer.model.file.ShareRoot;
import com.example.apiServer.repository.FolderPermissionRepository;
import com.example.apiServer.repository.ShareRootRepository;
import com.example.apiServer.service.warehouse.DynamicTableService;

import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class FileService {

    private final ShareRootRepository shareRootRepository;
    private final FolderPermissionRepository folderPermissionRepository;

    private final DynamicTableService dynamicTableService;

    // Constructor Injection
    public FileService(ShareRootRepository shareRootRepository,
            FolderPermissionRepository folderPermissionRepository, DynamicTableService dynamicTableService) {
        this.shareRootRepository = shareRootRepository;
        this.folderPermissionRepository = folderPermissionRepository;
        this.dynamicTableService = dynamicTableService;
    }

    /**
     * LOGIC LÕI CẢI TIẾN: Kiểm tra và lấy danh sách quyền hợp lệ (Không bốc bừa
     * phần tử đầu tiên nữa)
     */
    private List<FolderPermission> getValidPermissions(int accountId, String aliasName, String relativePath) {
        String cleanAlias = aliasName.replaceAll(
                "[^a-zA-Z0-9\\-_\\s&àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]",
                "").trim();
        ShareRoot root = shareRootRepository.findByAliasName(cleanAlias)
                .orElseThrow(() -> new RuntimeException("Vùng lưu trữ [" + cleanAlias + "] không tồn tại!"));

        String rawPath = (relativePath == null || relativePath.isBlank()) ? "" : relativePath.trim().replace("\\", "/");

        if (rawPath.startsWith("/"))
            rawPath = rawPath.substring(1);
        if (rawPath.endsWith("/"))
            rawPath = rawPath.substring(0, rawPath.length() - 1);

        rawPath = rawPath.replaceAll("\\s+", " ");
        final String reqPath = rawPath;
        String accountToken = "," + accountId + ",";

        List<FolderPermission> protectedFolders = folderPermissionRepository.findPermissionsForUser(root.getId(),
                accountToken);

        if (protectedFolders.isEmpty()) {
            throw new RuntimeException("Access Denied: Bạn không có quyền truy cập ổ đĩa này!");
        }

        List<FolderPermission> validPermissions = protectedFolders.stream()
                .filter(p -> {
                    String allowed = p.getRelativePath() == null ? "" : p.getRelativePath().trim().replace("\\", "/");
                    if (allowed.startsWith("/"))
                        allowed = allowed.substring(1);
                    if (allowed.endsWith("/"))
                        allowed = allowed.substring(0, allowed.length() - 1);

                    allowed = allowed.replaceAll("\\s+", " ");

                    if (allowed.isEmpty())
                        return true;

                    String reqPathLower = reqPath.toLowerCase();
                    String allowedLower = allowed.toLowerCase();

                    if (reqPathLower.equals(allowedLower))
                        return true;
                    if (reqPathLower.startsWith(allowedLower + "/"))
                        return true;
                    if (reqPathLower.isEmpty() || allowedLower.startsWith(reqPathLower + "/"))
                        return true;
                    if (allowedLower.contains(reqPathLower) || reqPathLower.contains(allowedLower))
                        return true;

                    return false;
                })
                .toList();

        if (validPermissions.isEmpty()) {
            throw new RuntimeException(
                    "Access Denied: Bạn không có quyền truy cập thư mục này! (Path: '" + reqPath + "')");
        }

        return validPermissions;
    }

    private Path getPhysicalSafePath(String aliasName, String relativePath) {
        String cleanAlias = aliasName.replaceAll(
                "[^a-zA-Z0-9\\-_\\s&àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]",
                "").trim();
        ShareRoot root = shareRootRepository.findByAliasName(cleanAlias)
                .orElseThrow(() -> new RuntimeException("Vùng lưu trữ [" + cleanAlias + "] không tồn tại!"));

        Path baseDir = Paths.get(root.getActualPath()).toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(relativePath == null ? "" : relativePath).normalize();

        if (!targetDir.startsWith(baseDir)) {
            throw new RuntimeException("Cảnh báo bảo mật: Path Traversal Detected!");
        }
        return targetDir;
    }

    public List<FileItem> getTree(int accountId, String path) throws IOException {
        if (path == null || path.isBlank()) {
            return getRootDrives(accountId);
        }

        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8).trim();
        decodedPath = decodedPath.replace("\\", "/");

        if (decodedPath.startsWith("/"))
            decodedPath = decodedPath.substring(1);
        if (decodedPath.endsWith("/"))
            decodedPath = decodedPath.substring(0, decodedPath.length() - 1);

        if (decodedPath.isEmpty() || decodedPath.equals("/")) {
            return getRootDrives(accountId);
        }

        String aliasName;
        String relativePath;
        int firstSlash = decodedPath.indexOf("/");

        if (firstSlash == -1) {
            aliasName = decodedPath;
            relativePath = "";
        } else {
            aliasName = decodedPath.substring(0, firstSlash);
            relativePath = decodedPath.substring(firstSlash + 1);
        }

        // 1. Lấy toàn bộ các quyền hợp lệ cho folder này
        List<FolderPermission> validPermissions = getValidPermissions(accountId, aliasName, relativePath);

        String cleanAlias = aliasName.replaceAll(
                "[^a-zA-Z0-9\\-_\\s&àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]",
                "").trim();
        ShareRoot root = shareRootRepository.findByAliasName(cleanAlias).orElseThrow();
        String accountToken = "," + accountId + ",";
        List<FolderPermission> userPermissions = folderPermissionRepository.findPermissionsForUser(root.getId(),
                accountToken);

        Path currentPhysical = getPhysicalSafePath(aliasName, relativePath);
        Path rootPhysical = getPhysicalSafePath(aliasName, "");

        if (!Files.isDirectory(currentPhysical)) {
            return List.of();
        }

        // 2. Kiểm tra xem TRONG SỐ các quyền hợp lệ, có cái nào cho phép WRITE không
        boolean hasWritePerm = validPermissions.stream().anyMatch(p -> {
            String allowed = p.getRelativePath() == null ? "" : p.getRelativePath().trim().replace("\\", "/");
            if (allowed.startsWith("/"))
                allowed = allowed.substring(1);
            if (allowed.endsWith("/"))
                allowed = allowed.substring(0, allowed.length() - 1);

            boolean tokenMatch = p.getWriteAccountIds() != null && p.getWriteAccountIds().contains(accountToken);
            if (!tokenMatch)
                return false;

            if (allowed.isEmpty() || relativePath.equalsIgnoreCase(allowed)
                    || relativePath.toLowerCase().startsWith(allowed.toLowerCase() + "/")) {
                return true;
            }
            return false;
        });

        // 3. Đọc dữ liệu từ ổ đĩa
        try (Stream<Path> stream = Files.list(currentPhysical)) {
            return stream
                    .filter(p -> {
                        String itemName = p.getFileName().toString().trim().toLowerCase().replaceAll("\\s+", " ");
                        Path abs = p.toAbsolutePath().normalize();
                        Path rootAbs = rootPhysical.toAbsolutePath().normalize();

                        String absStr = abs.toString().replace("\\", "/").toLowerCase().replaceAll("\\s+", " ");
                        String rootAbsStr = rootAbs.toString().replace("\\", "/").toLowerCase().replaceAll("\\s+", " ");

                        String currentStr = absStr.substring(rootAbsStr.length());
                        if (currentStr.startsWith("/"))
                            currentStr = currentStr.substring(1);

                        final String finalCurrentStr = currentStr;

                        boolean hasSpecificPerm = userPermissions.stream().anyMatch(perm -> {
                            String allowedStr = perm.getRelativePath() == null ? "" : perm.getRelativePath().trim();
                            if (allowedStr.isBlank())
                                return false;

                            String allowedTargetStr = allowedStr.replace("\\", "/").toLowerCase().replaceAll("\\s+",
                                    " ");
                            if (allowedTargetStr.startsWith("/"))
                                allowedTargetStr = allowedTargetStr.substring(1);
                            if (allowedTargetStr.endsWith("/"))
                                allowedTargetStr = allowedTargetStr.substring(0, allowedTargetStr.length() - 1);

                            String reqPathLower = relativePath.toLowerCase().trim().replace("\\", "/")
                                    .replaceAll("\\s+", " ");

                            if (reqPathLower.equals(allowedTargetStr)
                                    || reqPathLower.startsWith(allowedTargetStr + "/")) {
                                return true;
                            }

                            if (relativePath.isBlank()) {
                                return allowedTargetStr.equals(itemName)
                                        || allowedTargetStr.startsWith(itemName + "/")
                                        || itemName.contains(allowedTargetStr)
                                        || allowedTargetStr.contains(itemName);
                            }

                            return finalCurrentStr.equals(allowedTargetStr)
                                    || finalCurrentStr.startsWith(allowedTargetStr + "/")
                                    || allowedTargetStr.startsWith(finalCurrentStr + "/");
                        });

                        if (hasSpecificPerm)
                            return true;

                        return userPermissions.stream().anyMatch(perm -> {
                            String allowedStr = perm.getRelativePath() == null ? "" : perm.getRelativePath().trim();
                            return allowedStr.isBlank();
                        });
                    })
                    .map(p -> mapToItem(aliasName, p, hasWritePerm))
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private List<FileItem> getRootDrives(int accountId) {
        String accountToken = "," + accountId + ",";
        List<FolderPermission> permissions = folderPermissionRepository.findAllAccessibleRoots(accountToken);

        return permissions.stream()
                .map(FolderPermission::getShareRoot)
                .filter(Objects::nonNull)
                .distinct()
                .map(root -> {
                    String alias = root.getAliasName();
                    return new FileItem(alias, alias, true, 0, true, false);
                })
                .toList();
    }

    private FileItem mapToItem(String aliasName, Path p, boolean canWrite) {
        try {
            Path abs = p.toAbsolutePath().normalize();
            boolean isDir = Files.isDirectory(abs);
            Path rootPhysical = getPhysicalSafePath(aliasName, "");

            String relativePart = rootPhysical.relativize(abs).toString().replace("\\", "/");
            String virtualPath = aliasName + "/" + relativePart;

            FileItem item = new FileItem();
            item.setName(p.getFileName().toString());
            item.setPath(virtualPath);
            item.setFolder(isDir);
            item.setDirectory(canWrite); // canWrite được gán vào trường isDirectory theo logic cũ của bạn

            // Đọc các thuộc tính metadata từ hệ thống file (Ngày tạo, ngày sửa, kích thước)
            java.nio.file.attribute.BasicFileAttributes attrs = Files.readAttributes(abs,
                    java.nio.file.attribute.BasicFileAttributes.class);
            item.setLastModified(attrs.lastModifiedTime().toMillis());
            item.setCreatedTime(attrs.creationTime().toMillis());

            if (isDir) {
                // Đối với thư mục
                item.setSize(0);
                item.setFormattedSize("-");

                // Kiểm tra xem có thư mục/file con hay không
                boolean hasKids = hasChildren(abs);
                item.setHasChildren(hasKids);

                // Đếm số lượng item con trực tiếp bên trong thư mục (Cấp 1)
                if (hasKids) {
                    try (Stream<Path> countStream = Files.list(abs)) {
                        item.setChildCount((int) countStream.count());
                    } catch (Exception e) {
                        item.setChildCount(0);
                    }
                } else {
                    item.setChildCount(0);
                }
            } else {
                // Đối với file tệp tin
                long fileSize = attrs.size();
                item.setSize(fileSize);
                item.setFormattedSize(formatFileSize(fileSize)); // Định dạng hiển thị ví dụ: "12.5 KB"
                item.setHasChildren(false);
                item.setChildCount(0);
            }

            return item;
        } catch (Exception e) {
            // Trả về null nếu có file bị lỗi phân quyền hệ thống cấp OS, tránh làm crash cả
            // cây thư mục
            return null;
        }
    }

    // Thêm hàm định dạng dung lượng file cho đẹp mắt ở Frontend (Bỏ qua nếu
    // Front-end tự format)
    private String formatFileSize(long size) {
        if (size <= 0)
            return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " "
                + units[digitGroups];
    }

    private boolean hasChildren(Path path) {
        try (Stream<Path> s = Files.list(path)) {
            return s.findFirst().isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public Path getFileForDownload(int accountId, String aliasName, String relativePath) {
        Path file = getPhysicalSafePath(aliasName, relativePath);
        // Path parentPhysical = file.getParent();
        // String parentRelPath = parentPhysical != null
        // ? getPhysicalSafePath(aliasName, "").relativize(parentPhysical).toString()
        // : "";
        // getValidPermissions(accountId, aliasName, parentRelPath);
        return file;
    }

    /**
     * Chức năng xóa file/folder sửa lỗi phân quyền WRITE
     */
    public boolean deleteFileOrFolder(int accountId, String path) throws IOException {
        if (path == null || path.isBlank()) {
            throw new RuntimeException("Đường dẫn xóa không được để trống!");
        }

        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8).trim();
        decodedPath = decodedPath.replace("\\", "/");

        if (decodedPath.startsWith("/"))
            decodedPath = decodedPath.substring(1);
        if (decodedPath.endsWith("/"))
            decodedPath = decodedPath.substring(0, decodedPath.length() - 1);

        int firstSlash = decodedPath.indexOf('/');
        String aliasName = firstSlash != -1 ? decodedPath.substring(0, firstSlash) : decodedPath;
        String relativePath = firstSlash != -1 ? decodedPath.substring(firstSlash + 1) : "";

        Path targetPhysical = getPhysicalSafePath(aliasName, relativePath).toAbsolutePath().normalize();

        if (!Files.exists(targetPhysical)) {
            throw new RuntimeException("File hoặc thư mục không tồn tại trên hệ thống vật lý!");
        }

        Path parentPhysical = targetPhysical.getParent();
        Path rootDrivePhysical = getPhysicalSafePath(aliasName, "").toAbsolutePath().normalize();

        if (targetPhysical.equals(rootDrivePhysical) || rootDrivePhysical.startsWith(targetPhysical)) {
            throw new RuntimeException("Cảnh báo bảo mật cực đoan: Không được phép xóa thư mục gốc của ổ đĩa!");
        }

        String parentRelPath = "";
        if (parentPhysical != null && !parentPhysical.equals(rootDrivePhysical)) {
            parentRelPath = rootDrivePhysical.relativize(parentPhysical).toString().replace("\\", "/");
        }

        // SỬA TẠI ĐÂY: Duyệt qua tất cả các quyền hợp lệ của thư mục cha xem có cái nào
        // chứa Token Write không
        List<FolderPermission> permissions = getValidPermissions(accountId, aliasName, parentRelPath);
        String accountToken = "," + accountId + ",";

        boolean hasWritePerm = permissions.stream()
                .anyMatch(p -> p.getWriteAccountIds() != null && p.getWriteAccountIds().contains(accountToken));

        if (!hasWritePerm) {
            throw new RuntimeException("Access Denied: Tài khoản của bạn không có quyền XÓA tại vùng lưu trữ này!");
        }

        if (Files.isDirectory(targetPhysical)) {
            try (Stream<Path> walk = Files.walk(targetPhysical)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                System.err.println("Không thể xóa mục vật lý: " + p + " - " + e.getMessage());
                            }
                        });
            }
        } else {
            Files.delete(targetPhysical);
        }

        return !Files.exists(targetPhysical);
    }

    /**
     * Chức năng Upload sửa lỗi phân quyền WRITE
     */
    public void storeFile(int accountId, String aliasName, String targetDirectoryPath, String fileName, byte[] content)
            throws IOException {

        String cleanDirectoryPath = (targetDirectoryPath == null) ? "" : targetDirectoryPath.trim().replace("\\", "/");
        if (cleanDirectoryPath.startsWith("/"))
            cleanDirectoryPath = cleanDirectoryPath.substring(1);
        if (cleanDirectoryPath.endsWith("/"))
            cleanDirectoryPath = cleanDirectoryPath.substring(0, cleanDirectoryPath.length() - 1);

        // SỬA TẠI ĐÂY: Lấy tất cả các quyền hợp lệ thay vì lấy phần tử đầu tiên
        List<FolderPermission> permissions = getValidPermissions(accountId, aliasName, cleanDirectoryPath);
        String accountToken = "," + accountId + ",";

        boolean canWrite = permissions.stream()
                .anyMatch(p -> p.getWriteAccountIds() != null && p.getWriteAccountIds().contains(accountToken));

        if (!canWrite) {
            throw new RuntimeException(
                    "Access Denied: Bạn chỉ có quyền Xem, không có quyền Ghi/Upload tại thư mục này!");
        }

        Path targetDir = getPhysicalSafePath(aliasName, cleanDirectoryPath).toAbsolutePath().normalize();
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path rootDrivePath = getPhysicalSafePath(aliasName, "").toAbsolutePath().normalize();
        Path targetFilePath = targetDir.resolve(fileName).toAbsolutePath().normalize();

        if (!targetFilePath.startsWith(rootDrivePath)) {
            throw new RuntimeException("Cảnh báo bảo mật: Tên đường dẫn file hoặc cấu trúc tệp không hợp lệ!");
        }



        Files.write(targetFilePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        System.out.println("Đường dẫn upload là: " + targetFilePath.toString());
        // saveHistoryAction("Uplo", accountId);
    }

    public List<FileItem> getAvailableDrivesAsItems(int accountId) {
        String accountToken = "," + accountId + ",";
        List<FolderPermission> permissions = folderPermissionRepository.findAllAccessibleRoots(accountToken);

        return permissions.stream()
                .map(FolderPermission::getShareRoot)
                .filter(Objects::nonNull)
                .distinct()
                .map(root -> {
                    String alias = root.getAliasName();
                    return new FileItem(alias, alias + "/", true, 0, true, false);
                })
                .toList();
    }

    public Resource loadUpdateFile() {
        try {
            String filePathString = "I:\\Phòng IT-Hưng\\UpdateVersionApp\\PhuThanh_DesktopApp-0.0.1-SNAPSHOT.jar";
            Path filePath = Paths.get(filePathString);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotFoundException("Không tìm thấy file hoặc file không thể đọc: " + filePathString);
            }

            return resource;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi trong quá trình chuẩn bị file tải về: " + e.getMessage());
        }
    }

    /**
     * Chức năng Sao chép (Copy) file hoặc thư mục sang thư mục đích
     */
    public void copyFileOrFolder(int accountId, String sourcePath, String targetDirectoryPath) throws IOException {
        if (sourcePath == null || sourcePath.isBlank() || targetDirectoryPath == null) {
            throw new RuntimeException("Đường dẫn nguồn hoặc đích không được để trống!");
        }

        // 1. Phân tích và kiểm tra phân quyền nguồn (Cần quyền Read)
        String decodedSrc = URLDecoder.decode(sourcePath, StandardCharsets.UTF_8).trim().replace("\\", "/");
        if (decodedSrc.startsWith("/"))
            decodedSrc = decodedSrc.substring(1);
        int srcSlash = decodedSrc.indexOf('/');
        String srcAlias = srcSlash != -1 ? decodedSrc.substring(0, srcSlash) : decodedSrc;
        String srcRel = srcSlash != -1 ? decodedSrc.substring(srcSlash + 1) : "";

        Path srcPhysical = getPhysicalSafePath(srcAlias, srcRel).toAbsolutePath().normalize();
        if (!Files.exists(srcPhysical)) {
            throw new RuntimeException("Thành phần nguồn không tồn tại trên hệ thống!");
        }
        // Gọi hàm kiểm tra quyền đọc nguồn (sẽ ném lỗi nếu Access Denied)
        getValidPermissions(accountId, srcAlias, srcRel);

        // 2. Phân tích và kiểm tra phân quyền thư mục đích (Cần quyền WRITE)
        String decodedTarget = URLDecoder.decode(targetDirectoryPath, StandardCharsets.UTF_8).trim().replace("\\", "/");
        if (decodedTarget.startsWith("/"))
            decodedTarget = decodedTarget.substring(1);
        if (decodedTarget.endsWith("/"))
            decodedTarget = decodedTarget.substring(0, decodedTarget.length() - 1);

        int targetSlash = decodedTarget.indexOf('/');
        String targetAlias = targetSlash != -1 ? decodedTarget.substring(0, targetSlash) : decodedTarget;
        String targetRel = targetSlash != -1 ? decodedTarget.substring(targetSlash + 1) : "";

        List<FolderPermission> targetPerms = getValidPermissions(accountId, targetAlias, targetRel);
        String accountToken = "," + accountId + ",";
        boolean canWriteTarget = targetPerms.stream()
                .anyMatch(p -> p.getWriteAccountIds() != null && p.getWriteAccountIds().contains(accountToken));

        if (!canWriteTarget) {
            throw new RuntimeException("Access Denied: Bạn không có quyền ghi (Dán) vào thư mục đích này!");
        }

        Path targetDirPhysical = getPhysicalSafePath(targetAlias, targetRel).toAbsolutePath().normalize();
        if (!Files.exists(targetDirPhysical)) {
            Files.createDirectories(targetDirPhysical);
        }

        Path destPhysical = targetDirPhysical.resolve(srcPhysical.getFileName());

        // Ngăn chặn copy thư mục vào chính nó
        if (destPhysical.startsWith(srcPhysical)) {
            throw new RuntimeException("Không thể sao chép thư mục vào bên trong chính nó!");
        }

        // 3. Tiến hành Copy vật lý
        if (Files.isDirectory(srcPhysical)) {
            try (Stream<Path> stream = Files.walk(srcPhysical)) {
                stream.forEach(source -> {
                    try {
                        Path destination = destPhysical.resolve(srcPhysical.relativize(source));
                        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Lỗi sao chép thư mục con: " + e.getMessage());
                    }
                });
            }
        } else {
            Files.copy(srcPhysical, destPhysical, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Chức năng Di chuyển (Move / Cut) file hoặc thư mục sang thư mục đích
     */
    public void moveFileOrFolder(int accountId, String sourcePath, String targetDirectoryPath) throws IOException {
        if (sourcePath == null || sourcePath.isBlank() || targetDirectoryPath == null) {
            throw new RuntimeException("Đường dẫn nguồn hoặc đích không được để trống!");
        }

        // 1. Phân tích nguồn và kiểm tra quyền XÓA tại nguồn (Vì di chuyển sẽ làm mất
        // file nguồn)
        String decodedSrc = URLDecoder.decode(sourcePath, StandardCharsets.UTF_8).trim().replace("\\", "/");
        if (decodedSrc.startsWith("/"))
            decodedSrc = decodedSrc.substring(1);
        int srcSlash = decodedSrc.indexOf('/');
        String srcAlias = srcSlash != -1 ? decodedSrc.substring(0, srcSlash) : decodedSrc;
        String srcRel = srcSlash != -1 ? decodedSrc.substring(srcSlash + 1) : "";

        Path srcPhysical = getPhysicalSafePath(srcAlias, srcRel).toAbsolutePath().normalize();
        if (!Files.exists(srcPhysical)) {
            throw new RuntimeException("Thành phần nguồn không tồn tại!");
        }

        Path srcParent = srcPhysical.getParent();
        Path srcRootDrive = getPhysicalSafePath(srcAlias, "").toAbsolutePath().normalize();
        if (srcPhysical.equals(srcRootDrive) || srcRootDrive.startsWith(srcPhysical)) {
            throw new RuntimeException("Cảnh báo bảo mật: Không được di chuyển thư mục gốc!");
        }

        String srcParentRel = "";
        if (srcParent != null && !srcParent.equals(srcRootDrive)) {
            srcParentRel = srcRootDrive.relativize(srcParent).toString().replace("\\", "/");
        }

        List<FolderPermission> srcPerms = getValidPermissions(accountId, srcAlias, srcParentRel);
        String accountToken = "," + accountId + ",";
        boolean canWriteSrc = srcPerms.stream()
                .anyMatch(p -> p.getWriteAccountIds() != null && p.getWriteAccountIds().contains(accountToken));

        if (!canWriteSrc) {
            throw new RuntimeException("Access Denied: Bạn không có quyền CẮT / XÓA phần tử này từ nguồn!");
        }

        // 2. Phân tích đích và kiểm tra quyền GHI tại đích
        String decodedTarget = URLDecoder.decode(targetDirectoryPath, StandardCharsets.UTF_8).trim().replace("\\", "/");
        if (decodedTarget.startsWith("/"))
            decodedTarget = decodedTarget.substring(1);
        if (decodedTarget.endsWith("/"))
            decodedTarget = decodedTarget.substring(0, decodedTarget.length() - 1);

        int targetSlash = decodedTarget.indexOf('/');
        String targetAlias = targetSlash != -1 ? decodedTarget.substring(0, targetSlash) : decodedTarget;
        String targetRel = targetSlash != -1 ? decodedTarget.substring(targetSlash + 1) : "";

        List<FolderPermission> targetPerms = getValidPermissions(accountId, targetAlias, targetRel);
        boolean canWriteTarget = targetPerms.stream()
                .anyMatch(p -> p.getWriteAccountIds() != null && p.getWriteAccountIds().contains(accountToken));

        if (!canWriteTarget) {
            throw new RuntimeException("Access Denied: Bạn không có quyền GHI (Dán) vào thư mục đích này!");
        }

        Path targetDirPhysical = getPhysicalSafePath(targetAlias, targetRel).toAbsolutePath().normalize();
        Path destPhysical = targetDirPhysical.resolve(srcPhysical.getFileName());

        if (destPhysical.startsWith(srcPhysical)) {
            throw new RuntimeException("Không thể di chuyển thư mục vào bên trong chính nó!");
        }

        // 3. Tiến hành di chuyển vật lý
        if (!Files.exists(targetDirPhysical)) {
            Files.createDirectories(targetDirPhysical);
        }

        // Sử dụng ATOMIC_MOVE nếu cùng ổ đĩa, hoặc REPLACE_EXISTING nếu ghi đè file
        // trùng tên trùng cấu trúc
        Files.move(srcPhysical, destPhysical, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Chức năng tạo thư mục mới
     */
    public void createFolder(int accountId, String parentPath, String folderName) throws IOException {
        if (parentPath == null || folderName == null || folderName.isBlank()) {
            throw new RuntimeException("Đường dẫn cha hoặc tên thư mục không được để trống!");
        }

        // 1. Phân tích alias và relative path từ parentPath
        String decodedParent = URLDecoder.decode(parentPath, StandardCharsets.UTF_8).trim().replace("\\", "/");
        if (decodedParent.startsWith("/"))
            decodedParent = decodedParent.substring(1);

        int firstSlash = decodedParent.indexOf('/');
        String aliasName = firstSlash != -1 ? decodedParent.substring(0, firstSlash) : decodedParent;
        String relativePath = firstSlash != -1 ? decodedParent.substring(firstSlash + 1) : "";

        // 2. Kiểm tra quyền WRITE tại thư mục cha
        List<FolderPermission> permissions = getValidPermissions(accountId, aliasName, relativePath);
        String accountToken = "," + accountId + ",";
        boolean canWrite = permissions.stream()
                .anyMatch(p -> p.getWriteAccountIds() != null && p.getWriteAccountIds().contains(accountToken));

        if (!canWrite) {
            throw new RuntimeException("Access Denied: Bạn không có quyền tạo thư mục tại đây!");
        }

        // 3. Tạo đường dẫn vật lý và kiểm tra tồn tại
        Path parentDir = getPhysicalSafePath(aliasName, relativePath).toAbsolutePath().normalize();
        Path newFolderPath = parentDir.resolve(folderName).normalize();

        if (Files.exists(newFolderPath)) {
            throw new RuntimeException("Thư mục đã tồn tại: " + folderName);
        }

        Files.createDirectory(newFolderPath);
    }

    /**
     * Chức năng đổi tên file hoặc thư mục
     */
    public void renameItem(int accountId, String currentPath, String newName) throws IOException {
        if (currentPath == null || newName == null || newName.isBlank()) {
            throw new RuntimeException("Đường dẫn hoặc tên mới không được để trống!");
        }

        // 1. Phân tích đường dẫn hiện tại
        String decodedPath = URLDecoder.decode(currentPath, StandardCharsets.UTF_8).trim().replace("\\", "/");
        if (decodedPath.startsWith("/"))
            decodedPath = decodedPath.substring(1);

        int firstSlash = decodedPath.indexOf('/');
        String aliasName = firstSlash != -1 ? decodedPath.substring(0, firstSlash) : decodedPath;
        String oldRelativePath = firstSlash != -1 ? decodedPath.substring(firstSlash + 1) : "";

        Path oldPhysical = getPhysicalSafePath(aliasName, oldRelativePath).toAbsolutePath().normalize();
        if (!Files.exists(oldPhysical)) {
            throw new RuntimeException("Đối tượng nguồn không tồn tại!");
        }

        // 2. Kiểm tra quyền WRITE tại thư mục cha của đối tượng cần đổi tên
        Path parentDir = oldPhysical.getParent();
        Path rootDir = getPhysicalSafePath(aliasName, "").toAbsolutePath().normalize();

        // Nếu file nằm ngay gốc, parentRelPath sẽ là rỗng
        String parentRelPath = "";
        if (parentDir != null && !parentDir.equals(rootDir)) {
            parentRelPath = rootDir.relativize(parentDir).toString().replace("\\", "/");
        }

        List<FolderPermission> permissions = getValidPermissions(accountId, aliasName, parentRelPath);
        String accountToken = "," + accountId + ",";
        boolean canWrite = permissions.stream()
                .anyMatch(p -> p.getWriteAccountIds() != null && p.getWriteAccountIds().contains(accountToken));

        if (!canWrite) {
            throw new RuntimeException("Access Denied: Bạn không có quyền đổi tên đối tượng này!");
        }

        // 3. Thực hiện đổi tên
        Path newPhysical = oldPhysical.resolveSibling(newName);
        if (Files.exists(newPhysical)) {
            throw new RuntimeException("Tên mới đã bị trùng với một file/thư mục khác!");
        }

        Files.move(oldPhysical, newPhysical, StandardCopyOption.REPLACE_EXISTING);
    }

    private void saveHistoryAction(String acction, int accid) {
        Map<String, Object> data = new HashMap<>();
        data.put("Acction", acction);
        data.put("AccountID", accid);
        dynamicTableService.insert("AcctionFile", data, "");
        data.clear();
    }

}
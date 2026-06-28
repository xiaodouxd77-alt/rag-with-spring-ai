package org.benaya.ai.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件存储服务 - 基础设施服务
 * 职责：只负责文件的保存、读取、删除
 * 不关心数据库、向量库、文档等业务逻辑
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${app.upload-path:./uploads}")
    private String uploadPath;

    /**
     * 保存文件到磁盘
     *
     * @param file 上传的文件
     * @param kbCode 知识库代码，用于创建子目录
     * @return 存储文件信息（文件名、路径、大小）
     * @throws IOException IO异常
     */
    public StoredFile save(MultipartFile file, String kbCode) throws IOException {
        // 1. 生成唯一文件名（UUID + 原始文件名）
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        // 2. ✅ 使用 toAbsolutePath() + normalize() 处理路径
        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path dir = basePath.resolve(kbCode).normalize();

        // 3. 创建目录
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            log.info("创建目录: {}", dir.toAbsolutePath());
        }
        // 3. 构建目标文件路径
        Path targetPath = dir.resolve(uniqueFileName).normalize();

        // 4. 保存文件到磁盘
        file.transferTo(targetPath.toFile());
        log.info("文件保存成功: {}", targetPath.toAbsolutePath());

        // 5. 返回存储文件信息
        return new StoredFile(
                originalFilename != null ? originalFilename : "unknown",
                targetPath.toString(),
                file.getSize()
        );
    }

    /**
     * 根据文件路径读取文件
     *
     * @param filePath 文件路径
     * @return 文件资源
     * @throws IOException IO异常
     */
    public Resource load(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + filePath);
        }

        if (!Files.isReadable(path)) {
            throw new IOException("文件不可读: " + filePath);
        }

        Resource resource = new UrlResource(path.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("无法访问文件: " + filePath);
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @throws IOException IO异常
     */
    public void delete(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            log.warn("文件不存在，无需删除: {}", filePath);
            return;
        }

        boolean deleted = Files.deleteIfExists(path);
        if (deleted) {
            log.info("文件删除成功: {}", filePath);
        } else {
            throw new IOException("文件删除失败: " + filePath);
        }
    }

    /**
     * 存储文件信息值对象
     * 封装文件保存后的基本信息
     */
    public record StoredFile(
            String fileName,   // 原始文件名
            String filePath,   // 完整文件路径
            long fileSize      // 文件大小（字节）
    ) {}
}
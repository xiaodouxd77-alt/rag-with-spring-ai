package org.benaya.ai.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.benaya.ai.rag.exception.ResourceNotFoundException;
import org.benaya.ai.rag.model.Document;
import org.benaya.ai.rag.model.DocumentStatus;
import org.benaya.ai.rag.repository.DocumentJpaRepository;
import org.benaya.ai.rag.repository.VectorStoreRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentManagementService {

    private final FileStorageService fileStorageService;
    private final DocumentJpaRepository documentJpaRepository;
    private final VectorStoreRepository vectorStoreRepository;
    private final DocumentImportService documentImportService;
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 上传文件并导入到知识库
     * 完整链路：保存文件 → 创建记录 → 向量化 → 更新状态
     */
    public Document uploadAndImport(MultipartFile file, String kbCode) {
        // 1. 知识库 code → ID
        Long knowledgeBaseId = knowledgeBaseService.getKbId(kbCode);

        // 2. 保存文件到磁盘（先保存再建记录，文件存失败不创建 Document）
        FileStorageService.StoredFile storedFile;
        try {
            storedFile = fileStorageService.save(file, kbCode);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + file.getOriginalFilename(), e);
        }

        // 3. 创建 Document 记录
        Document doc = new Document();
        doc.setKnowledgeBaseId(knowledgeBaseId);
        doc.setFileName(storedFile.fileName());
        doc.setFilePath(storedFile.filePath());
        doc.setFileSize(storedFile.fileSize());
        doc.setStatus(DocumentStatus.PROCESSING);
        doc = documentJpaRepository.save(doc);

        Long documentId = doc.getId();

        try {
            // 4. 加载文件并导入向量库
            Resource fileResource = fileStorageService.load(storedFile.filePath());
            int chunks = documentImportService.importFromCsv(
                    fileResource, knowledgeBaseId, documentId);

            // 5. 更新为完成
            doc.setStatus(DocumentStatus.COMPLETED);
            doc.setChunkCount(chunks);
            log.info("文档导入完成: id={}, fileName={}, chunks={}",
                    documentId, storedFile.fileName(), chunks);
        } catch (Exception e) {
            log.error("文档导入失败: id={}, fileName={}", documentId, storedFile.fileName(), e);
            doc.setStatus(DocumentStatus.FAILED);
        } finally {
            doc = documentJpaRepository.save(doc);
        }

        return doc;
    }

    /**
     * 删除文档（向量 + 文件 + 数据库记录）
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        Document doc = documentJpaRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        // 1. 删除向量库中的向量
        vectorStoreRepository.deleteByDocumentId(documentId);

        // 2. 删除物理文件（失败不影响数据库删除）
        try {
            fileStorageService.delete(doc.getFilePath());
        } catch (IOException e) {
            log.warn("文件删除失败: path={}", doc.getFilePath(), e);
        }

        // 3. 删除数据库记录
        documentJpaRepository.delete(doc);
        log.info("文档已删除: id={}, fileName={}", documentId, doc.getFileName());
    }

    /**
     * 查询单个文档
     */
    public Document getDocument(Long documentId) {
        return documentJpaRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    /**
     * 分页查询文档列表
     */
    public Page<Document> listDocuments(Long knowledgeBaseId, Pageable pageable) {
        if (knowledgeBaseId != null) {
            return documentJpaRepository.findByKnowledgeBaseId(knowledgeBaseId, pageable);
        }
        return documentJpaRepository.findAll(pageable);
    }

    /**
     * 按 kbCode 分页查询文档列表
     */
    public Page<Document> listDocumentsByCode(String kbCode, Pageable pageable) {
        Long knowledgeBaseId = knowledgeBaseService.getKbId(kbCode);
        return documentJpaRepository.findByKnowledgeBaseId(knowledgeBaseId, pageable);
    }

    /**
     * 重新向量化：删旧向量 → 重新解析 → 重新导入
     */
    public Document reprocessDocument(Long documentId) {
        Document doc = getDocument(documentId);

        // 1. 删除旧向量
        vectorStoreRepository.deleteByDocumentId(documentId);

        // 2. 更新状态
        doc.setStatus(DocumentStatus.PROCESSING);
        doc = documentJpaRepository.save(doc);

        try {
            // 3. 重新解析并导入
            Resource fileResource = fileStorageService.load(doc.getFilePath());
            int chunks = documentImportService.importFromCsv(
                    fileResource, doc.getKnowledgeBaseId(), documentId);

            doc.setStatus(DocumentStatus.COMPLETED);
            doc.setChunkCount(chunks);
            log.info("文档重新导入完成: id={}, fileName={}, chunks={}",
                    documentId, doc.getFileName(), chunks);
        } catch (Exception e) {
            log.error("文档重新导入失败: id={}", documentId, e);
            doc.setStatus(DocumentStatus.FAILED);
        } finally {
            doc = documentJpaRepository.save(doc);
        }

        return doc;
    }
}

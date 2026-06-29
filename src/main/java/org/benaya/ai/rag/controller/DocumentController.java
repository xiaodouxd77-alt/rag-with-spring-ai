package org.benaya.ai.rag.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.benaya.ai.rag.dto.DocumentResponse;
import org.benaya.ai.rag.dto.UploadResponse;
import org.benaya.ai.rag.model.Document;
import org.benaya.ai.rag.service.DocumentManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.benaya.ai.rag.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv");

    private final DocumentManagementService documentManagementService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<UploadResponse> upload(@RequestParam MultipartFile file,
                                                 @RequestParam String kbCode) {
        if (file.isEmpty()) {
            throw new BadRequestException("文件内容为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !isAllowedExtension(filename)) {
            throw new BadRequestException("不支持的文件类型，仅支持 CSV");
        }

        Document doc = documentManagementService.uploadAndImport(file, kbCode);
        DocumentResponse docResp = DocumentResponse.from(doc);
        return ResponseEntity.ok(UploadResponse.from(docResp));
    }

    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> list(@RequestParam(required = false) String kbCode,
                                                       Pageable pageable) {
        Page<Document> page = (kbCode != null)
                ? documentManagementService.listDocumentsByCode(kbCode, pageable)
                : documentManagementService.listDocuments(null, pageable);
        return ResponseEntity.ok(page.map(DocumentResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(DocumentResponse.from(documentManagementService.getDocument(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentManagementService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<DocumentResponse> reprocess(@PathVariable Long id) {
        return ResponseEntity.ok(
                DocumentResponse.from(documentManagementService.reprocessDocument(id)));
    }

    private boolean isAllowedExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        return ALLOWED_EXTENSIONS.contains(filename.substring(dot + 1).toLowerCase());
    }
}

package org.benaya.ai.rag.dto;

public record UploadResponse(
        Long id,
        String fileName,
        DocumentResponse document
) {
    public static UploadResponse from(DocumentResponse doc) {
        return new UploadResponse(doc.id(), doc.fileName(), doc);
    }

    public static UploadResponse of(Long id, String fileName, DocumentResponse doc) {
        return new UploadResponse(id, fileName, doc);
    }
}

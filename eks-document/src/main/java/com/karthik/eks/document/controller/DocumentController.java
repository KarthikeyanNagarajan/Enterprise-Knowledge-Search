package com.karthik.eks.document.controller;

import com.karthik.eks.document.model.Document;
import com.karthik.eks.document.service.DocumentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<Document> getDocuments() {
        return documentService.getAllDocuments();
    }

    @PostMapping
    public Document addDocument(@RequestBody Document document) {
        return documentService.addDocument(document);
    }
}
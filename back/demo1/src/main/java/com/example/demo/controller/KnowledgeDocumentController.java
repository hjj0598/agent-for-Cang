package com.example.demo.controller;

import com.example.demo.dto.KnowledgeTextRequest;
import com.example.demo.dto.PageResult;
import com.example.demo.pojo.KnowledgeChunk;
import com.example.demo.pojo.KnowledgeDocument;
import com.example.demo.pojo.Result;
import com.example.demo.service.KnowledgeDocumentService;
import com.example.demo.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeDocumentController {

    private static final long MAX_UPLOAD_FILE_SIZE = 70L * 1024 * 1024;
    private static final int MAX_PDF_PAGE_COUNT = 100;
    private static final int MAX_KNOWLEDGE_CONTENT_LENGTH = 200_000;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<KnowledgeDocument>> list(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      @RequestParam(required = false) String keyword,
                                                      HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(knowledgeDocumentService.page(userId, page, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public Result<KnowledgeDocument> detail(@PathVariable Long id,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(knowledgeDocumentService.getById(id, userId));
    }

    @GetMapping("/{id}/chunks")
    public Result<List<KnowledgeChunk>> chunks(@PathVariable Long id,
                                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(knowledgeDocumentService.listChunks(id, userId));
    }

    @PostMapping("/text")
    public Result<Void> addText(@RequestBody @Valid KnowledgeTextRequest knowledgeTextRequest,
                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        KnowledgeDocument document = new KnowledgeDocument();
        document.setUserId(userId);
        document.setTitle(knowledgeTextRequest.getTitle());
        document.setContent(knowledgeTextRequest.getContent());
        document.setSource("手动添加");

        knowledgeDocumentService.add(document);
        operationLogService.record(userId, "新增知识库", "手动新增知识：" + document.getTitle());

        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody @Valid KnowledgeTextRequest knowledgeTextRequest,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setUserId(userId);
        document.setTitle(knowledgeTextRequest.getTitle());
        document.setContent(knowledgeTextRequest.getContent());

        knowledgeDocumentService.update(document);
        operationLogService.record(userId, "编辑知识库", "编辑知识：" + document.getTitle());

        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        KnowledgeDocument document = knowledgeDocumentService.getById(id, userId);
        knowledgeDocumentService.delete(id, userId);
        operationLogService.record(userId, "删除知识库", "删除知识：" + document.getTitle());
        return Result.success();
    }

    @PostMapping("/upload")
    public Result<Void> upload(@RequestParam("file") MultipartFile file,
                               HttpServletRequest request) throws Exception {
        Long userId = (Long) request.getAttribute("userId");

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        if (file.getSize() > MAX_UPLOAD_FILE_SIZE) {
            throw new RuntimeException("上传文件不能超过70MB");
        }

        String filename = validateFilename(file.getOriginalFilename());
        byte[] fileBytes = file.getBytes();

        String lowerName = filename.toLowerCase();
        String content;
        String source;

        if (lowerName.endsWith(".txt")) {
            content = readTxt(fileBytes);
            source = "TXT上传";
        } else if (lowerName.endsWith(".pdf")) {
            content = readPdf(fileBytes);
            source = "PDF上传";
        } else {
            throw new RuntimeException("当前只支持上传 txt 和 pdf 文件");
        }

        validateKnowledgeContent(content);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setUserId(userId);
        document.setTitle(filename);
        document.setContent(content);
        document.setSource(source);

        knowledgeDocumentService.add(document);
        operationLogService.record(userId, "上传知识库", "上传文件：" + filename);

        return Result.success();
    }

    private String validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new RuntimeException("文件名不能为空");
        }

        String cleanFilename = filename.trim();

        if (cleanFilename.contains("/") || cleanFilename.contains("\\") || cleanFilename.contains("..")) {
            throw new RuntimeException("文件名不合法，请不要包含路径或特殊字符");
        }

        if (cleanFilename.length() > 120) {
            throw new RuntimeException("文件名不能超过120个字符");
        }

        return cleanFilename;
    }

    private String readTxt(byte[] fileBytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            return decoder.decode(ByteBuffer.wrap(fileBytes)).toString();
        } catch (CharacterCodingException e) {
            throw new RuntimeException("TXT文件不是有效的UTF-8编码，请转换编码后再上传");
        }
    }

    private String readPdf(byte[] fileBytes) throws Exception {
        if (!isPdfFile(fileBytes)) {
            throw new RuntimeException("PDF文件格式不正确，请上传真实的PDF文件");
        }

        try (PDDocument pdfDocument = Loader.loadPDF(fileBytes)) {
            int pageCount = pdfDocument.getNumberOfPages();

            if (pageCount > MAX_PDF_PAGE_COUNT) {
                throw new RuntimeException("PDF页数不能超过100页");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(pdfDocument);
        }
    }

    private boolean isPdfFile(byte[] fileBytes) {
        return fileBytes.length >= 4
                && fileBytes[0] == '%'
                && fileBytes[1] == 'P'
                && fileBytes[2] == 'D'
                && fileBytes[3] == 'F';
    }

    private void validateKnowledgeContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("文件内容为空，无法导入知识库");
        }

        if (content.length() > MAX_KNOWLEDGE_CONTENT_LENGTH) {
            throw new RuntimeException("文件解析后的文本内容不能超过20万字，请拆分后再上传");
        }
    }
}

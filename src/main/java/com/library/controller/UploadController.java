package com.library.controller;

import com.library.common.Result;
import com.library.common.ResultCode;
import com.library.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传接口：头像、图书封面（登录用户可用）
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");

    /** 上传保存目录（相对工作目录），静态映射 /upload/** */
    @Value("${app.upload-dir:upload}")
    private String uploadDir;

    @Operation(summary = "上传图片，返回可访问 URL")
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅支持图片格式：" + ALLOWED_EXTENSIONS);
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(fileName).toFile());
            String url = "/upload/" + fileName;
            log.info("图片上传成功: {} ({} bytes)", url, file.getSize());
            return Result.ok(url);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new BizException(ResultCode.SERVER_ERROR, "上传失败，请稍后再试");
        }
    }
}

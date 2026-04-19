package com.bubbles.server.controller.user;


import com.bubbles.common.constant.MessageConstant;
import com.bubbles.common.result.Result;
import com.bubbles.common.utils.AliOssUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 通用接口
 */
@RestController
@Slf4j
@RequestMapping("/user/common")
@Tag(name = "通用接口",description = "")

public class CommonController {
    @Autowired
    private AliOssUtil aliOssUtil;
    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @Operation(summary = "文件上传",description = "")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传:{}",file);
        try {
            //通过UUID保证文件名不重复并动态地把原始文件名后缀截取
            String originalFilename = file.getOriginalFilename();
            //截取原始文件名的后缀dwadrg213.png
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            //UUID,构造新文件名称
            String objectName=UUID.randomUUID().toString()+extension;
            //文件的请求路径
            String filePath = aliOssUtil.upload(file.getBytes(),objectName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败: { }",e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}

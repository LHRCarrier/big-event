package com.bubbles.server.controller.writterAI;

import com.bubbles.common.result.Result;
import com.bubbles.pojo.dto.WriterRequestDTO;
import com.bubbles.pojo.dto.WriterResponseDTO;
import com.bubbles.server.service.WriterAIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI撰稿控制器
 * 提供AI撰稿相关的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/user/writer")
@RequiredArgsConstructor
@Tag(name = "AI撰稿", description = "AI撰稿相关接口")
public class WriterController {
    
    private final WriterAIService writerAIService;
    
    /**
     * AI撰稿接口
     * 
     * @param request 撰稿请求参数
     * @return 撰稿响应结果
     */
    @PostMapping("/write")
    @Operation(summary = "AI撰稿", description = "根据话题生成文章内容")
    public Result<WriterResponseDTO> writeArticle(@RequestBody WriterRequestDTO request) {
        log.info("收到AI撰稿请求，话题: {}", request.getTopic());
        
        // 调用AI服务
        WriterResponseDTO response = writerAIService.writeArticle(request);
        
        log.info("AI撰稿完成，文章ID: {}", response.getArticleId());
        return Result.success(response);
    }
    
    /**
     * 检查AI服务状态
     * 
     * @return 服务状态
     */
    @GetMapping("/status")
    @Operation(summary = "检查服务状态", description = "检查AI服务是否可用")
    public Result<String> checkStatus() {
        String status = writerAIService.getServiceStatus();
        return Result.success(status);
    }
    
    /**
     * 快速撰稿接口（简化版）
     * 只需要传入话题即可生成文章
     * 
     * @param topic 话题关键词
     * @return 撰稿响应结果
     */
    @GetMapping("/quick-write")
    @Operation(summary = "快速撰稿", description = "简化版撰稿接口，只需传入话题")
    public Result<WriterResponseDTO> quickWrite(@RequestParam String topic) {
        log.info("收到快速撰稿请求，话题: {}", topic);
        
        // 构建默认请求
        WriterRequestDTO request = WriterRequestDTO.builder()
                .topic(topic)
                .length(500)
                .style("neutral")
                .audience("general")
                .generateSummary(true)
                .build();
        
        // 调用AI服务
        WriterResponseDTO response = writerAIService.writeArticle(request);
        
        log.info("快速撰稿完成，文章ID: {}", response.getArticleId());
        return Result.success(response);
    }
}
package com.bubbles.server.controller.writterAI;

import com.bubbles.common.context.BaseContext;
import com.bubbles.common.result.Result;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.pojo.dto.WriteFromHotRequestDTO;
import com.bubbles.pojo.dto.WriterRequestDTO;
import com.bubbles.pojo.dto.WriterResponseDTO;
import com.bubbles.pojo.entity.AiProcessRecord;
import com.bubbles.pojo.entity.Article;
import com.bubbles.pojo.entity.ArticleCategory;
import com.bubbles.server.mapper.AiProcessRecordMapper;
import com.bubbles.server.mapper.ArticleCategoryMapper;
import com.bubbles.server.service.ArticleService;
import com.bubbles.server.service.WriterAIService;
import com.bubbles.server.service.impl.AutoPublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private final AutoPublishService autoPublishService;
    private final ArticleService articleService;
    private final ArticleCategoryMapper articleCategoryMapper;
    private final AiProcessRecordMapper aiProcessRecordMapper;
    
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
     * AI撰稿流式接口
     * 通过SSE实时推送生成内容
     */
    @PostMapping(value = "/write/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI流式撰稿", description = "通过SSE实时推送AI生成的文章内容")
    public Flux<String> streamArticle(@RequestBody WriterRequestDTO request) {
        log.info("收到AI流式撰稿请求，话题: {}", request.getTopic());
        return writerAIService.streamArticle(request);
    }

    /**
     * 基于热点数据撰稿接口
     * 与普通撰稿的区别：请求中携带完整的热点上下文（播放量、互动数据等），
     * AI 将基于这些信息生成更高质量的文章
     */
    @PostMapping("/write-from-hot")
    @Operation(summary = "热点撰稿", description = "基于热点数据生成文章并自动保存到文章管理（分类不存在时自动创建）")
    public Result<WriterResponseDTO> writeFromHot(@RequestBody WriteFromHotRequestDTO request) {
        log.info("收到热点撰稿请求，标题: {}, 热度评分: {}", request.getTitle(), request.getHotScore());

        WriterResponseDTO response = writerAIService.writeFromHot(request);
        log.info("热点撰稿AI生成完成, title={}", response.getTitle());

        // 查找或创建分类
        String categoryName = request.getPartition();
        Long categoryId = articleCategoryMapper.findIdByName(categoryName);
        if (categoryId == null && categoryName != null && !categoryName.isEmpty()) {
            Long createUser = BaseContext.getCurrentId();
            if (createUser == null) {
                createUser = 1L; // fallback to system user
                log.warn("[撰稿] BaseContext.currentId 为空，回退使用系统用户ID=1");
            }
            ArticleCategory category = ArticleCategory.builder()
                    .categoryName(categoryName)
                    .categoryAlias(categoryName)
                    .createUser(createUser)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            articleCategoryMapper.add(category);
            categoryId = articleCategoryMapper.findIdByName(categoryName);
            log.info("[撰稿] 自动创建分类: {} (ID: {})", categoryName, categoryId);
        }

        // 保存文章到数据库（title 截断到 28 字符以内，适配 DB varchar(30)）
        String title = response.getTitle();
        if (title != null && title.length() > 28) {
            title = title.substring(0, 28) + "…";
        }
        String coverImg = (request.getCoverUrl() != null && !request.getCoverUrl().isEmpty())
                ? request.getCoverUrl()
                : "https://bubbleovo.oss-cn-beijing.aliyuncs.com/default-cover.png";

        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle(title);
        articleDTO.setContent(response.getContent());
        articleDTO.setCoverImg(coverImg);
        articleDTO.setCategoryId(categoryId);
        articleDTO.setState("草稿");
        articleService.addArticle(articleDTO);
        log.info("[撰稿] 文章已保存: title={}, categoryId={}, coverImg={}", title, categoryId,
                coverImg.length() > 50 ? coverImg.substring(0, 50) + "..." : coverImg);

        // 记录处理状态
        if (request.getBvid() != null) {
            AiProcessRecord record = AiProcessRecord.builder()
                    .bvid(request.getBvid())
                    .articleId(null)
                    .processTime(LocalDateTime.now())
                    .status("generated")
                    .aiModel(response.getModelUsed())
                    .hotScore(request.getHotScore() != null ? BigDecimal.valueOf(request.getHotScore()) : null)
                    .build();
            aiProcessRecordMapper.insert(record);
        }

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

    /**
     * 自动发布接口
     * 手动触发：评分 → 筛选 → AI撰稿 → 创建草稿文章
     */
    @PostMapping("/auto-publish")
    @Operation(summary = "自动发布", description = "从热榜中筛选 Top N 热点，自动生成文章并保存为草稿")
    public Result<List<Article>> autoPublish(
            @RequestParam(defaultValue = "5") int topN,
            @RequestParam(defaultValue = "all") String partition,
            @RequestParam(defaultValue = "60") int minScore) {
        log.info("收到自动发布请求: topN={}, partition={}, minScore={}", topN, partition, minScore);

        List<Article> articles = autoPublishService.autoPublish(topN, partition, minScore);

        log.info("自动发布完成，共生成 {} 篇文章", articles.size());
        return Result.success(articles);
    }
}
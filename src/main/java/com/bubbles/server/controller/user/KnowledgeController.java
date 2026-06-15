package com.bubbles.server.controller.user;

import com.bubbles.common.result.PageResult;
import com.bubbles.common.result.Result;
import com.bubbles.pojo.dto.KnowledgeArticleDTO;
import com.bubbles.pojo.dto.KnowledgePageQueryDTO;
import com.bubbles.pojo.vo.KnowledgeArticleVO;
import com.bubbles.server.service.KnowledgeArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@Tag(name = "知识库管理", description = "知识库文章的增删改查接口")
@RequestMapping("/user/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeArticleService knowledgeArticleService;

    @PostMapping
    @Operation(summary = "新增知识库文章")
    public Result add(@Validated @RequestBody KnowledgeArticleDTO dto) {
        log.info("新增知识库文章: title={}", dto.getTitle());
        knowledgeArticleService.add(dto);
        return Result.success("知识库文章添加成功");
    }

    @GetMapping
    @Operation(summary = "分页查询知识库文章")
    public Result<PageResult> page(KnowledgePageQueryDTO query) {
        log.info("分页查询知识库: page={}, pageSize={}, category={}, keyword={}",
                query.getPage(), query.getPageSize(), query.getCategory(), query.getKeyword());
        PageResult pageResult = knowledgeArticleService.pageQuery(query);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识库文章详情")
    public Result<KnowledgeArticleVO> getById(@PathVariable Long id) {
        log.info("获取知识库文章详情: id={}", id);
        KnowledgeArticleVO vo = knowledgeArticleService.getById(id);
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改知识库文章")
    public Result update(@PathVariable Long id, @Validated @RequestBody KnowledgeArticleDTO dto) {
        log.info("修改知识库文章: id={}", id);
        dto.setId(id);
        knowledgeArticleService.update(dto);
        return Result.success("知识库文章修改成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库文章")
    public Result delete(@PathVariable Long id) {
        log.info("删除知识库文章: id={}", id);
        knowledgeArticleService.delete(id);
        return Result.success("知识库文章删除成功");
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/停用知识库文章")
    public Result toggleStatus(@PathVariable Long id) {
        log.info("切换知识库文章状态: id={}", id);
        knowledgeArticleService.toggleStatus(id);
        return Result.success("状态切换成功");
    }

    /** 供 Python 检索端调用的内部接口 */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的知识库文章（供AI检索用）")
    public Result<List<KnowledgeArticleVO>> listAllEnabled() {
        List<KnowledgeArticleVO> list = knowledgeArticleService.listAllEnabled();
        return Result.success(list);
    }

    @GetMapping("/enabled/{id}")
    @Operation(summary = "获取单篇启用的知识库文章全文")
    public Result<KnowledgeArticleVO> getEnabledById(@PathVariable Long id) {
        KnowledgeArticleVO vo = knowledgeArticleService.getEnabledById(id);
        return Result.success(vo);
    }
}

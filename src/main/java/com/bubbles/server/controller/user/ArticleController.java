package com.bubbles.server.controller.user;

import com.bubbles.common.result.PageResult;
import com.bubbles.common.result.Result;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.pojo.dto.ArticlePageQueryDTO;
import com.bubbles.pojo.entity.Article;
import com.bubbles.pojo.vo.ArticleVO;
import com.bubbles.server.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@Tag(name="文章管理相关接口",description = "包含新增，修改，及文章信息的增删改查")
@RequestMapping("/article")
public class ArticleController {
    @Autowired
    private ArticleService articleService;

    /**
     * 新增文章
     * @param articleDTO
     * @return
     */
    @PostMapping("/add")
    @Operation(summary = "新增文章",description = "该接口用于发布(新增)文章")
    public Result add(@RequestBody ArticleDTO articleDTO){
        log.info("新增文章请求，articleDTO: {}", articleDTO);
        articleService.addArticle(articleDTO);
        return Result.success("文章新增成功！");
    }

    /**
     * 文章列表分页查询
     * @param articlePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "文章列表(条件分页)",description = "该接口用于根据条件查询文章，带分页")
    public Result<PageResult> page(ArticlePageQueryDTO articlePageQueryDTO){
        log.info("文章列表条件分页请求:articlePageQueryDTO: {}", articlePageQueryDTO);
        PageResult pageResult = articleService.pageQuery(articlePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 获取文章详情
     * @param id
     * @return
     */
    @GetMapping("/detail")
    @Operation(summary = "获取文章详情",description = "该接口用于根据文章ID获取文章详细信息")
    public Result<Article> detail(Long id){
        log.info("获取文章详情请求,文章id:{}",id);
        Article article = articleService.detail(id);
        return  Result.success(article);
    }

    /**
     * 更新文章信息
     * @param articleDTO
     * @return
     */
    @PutMapping("/update")
    @Operation(summary = "更新文章",description = "该接口用于更新文章信息")
    public Result<String> update(ArticleDTO articleDTO){
        log.info("更新文章请求，articleDTO: {}", articleDTO);
        articleService.update(articleDTO);
        return Result.success("文章信息更新成功");
    }

    /**
     * 删除文章
     * @param id
     * @return
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除文章",description = "该接口用于根据id删除文章")
    public Result<String> delete(Long id){
        log.info("删除文章请求,文章id:{}",id);
        articleService.delete(id);
        return Result.success("文章已删除!");
    }
}

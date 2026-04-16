package com.bubbles.server.controller.user;

import com.bubbles.common.Result;
import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.ArticleDTO;
import com.bubbles.server.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@Tag(name="文章相关接口",description = "包含新增，修改，及文章信息的增删改查")
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
    @Operation(summary = "新增文章",description = "新增文章")
    public Result add(@RequestBody ArticleDTO articleDTO){
        log.info("新增文章请求，articleDTO: {}", articleDTO);
        articleService.addArticle(articleDTO);
        return Result.success("文章新增成功！");
    }
    @PostMapping("/category")
    @Operation(summary = "新增文章分类",description = "新增文章分类")
    public Result category(@RequestBody ArticleCategoryDTO articleCategoryDTO){
        log.info("新增文章分类请求:articleCategoryDTO: {}", articleCategoryDTO);
        articleService.addCategory(articleCategoryDTO);
        return  Result.success("新增文章分类操作成功！");
    }
    /**
     * 查询文章
     * @return
     */
    @GetMapping("/list")
    public Result<String> list(){
        return Result.success("<UNK>");
    }

    @PutMapping("/update")
    public Result<String> update(){
        return Result.success("<UNK>");
    }
}

package com.bubbles.server.controller.user;

import com.bubbles.common.result.PageResult;
import com.bubbles.common.result.Result;
import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.CategoryPageQueryDTO;
import com.bubbles.server.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@Tag(name="文章分类相关接口",description = "包含文章分类新增，修改，查询，删除接口")
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增文章分类
     * @param articleCategoryDTO
     * @return
     */
    @PostMapping("/add")
    @Operation(summary = "新增文章分类",description = "新增文章分类")
    public Result category(@RequestBody ArticleCategoryDTO articleCategoryDTO){
        log.info("新增文章分类请求:articleCategoryDTO: {}", articleCategoryDTO);
        categoryService.addCategory(articleCategoryDTO);
        return  Result.success("新增文章分类操作成功！");
    }

    /**
     * 分页查询文章分类
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询文章分类",description = "该接口用于获取当前已登录用户创建的所有文章分类")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询文章分类请求:categoryPageQueryDTO: {}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    @PutMapping("/update")
    public Result<String> update(){
        return Result.success("<UNK>");
    }
}

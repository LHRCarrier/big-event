package com.bubbles.server.controller.user;

import com.bubbles.common.result.PageResult;
import com.bubbles.common.result.Result;
import com.bubbles.pojo.dto.ArticleCategoryDTO;
import com.bubbles.pojo.dto.CategoryPageQueryDTO;
import com.bubbles.server.service.ArticleCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@Tag(name="文章分类相关接口",description = "包含文章分类新增，修改，查询，删除接口")
@RequestMapping("/user/category")
public class ArticleCategoryController {
    @Autowired
    private ArticleCategoryService articleCategoryService;

    /**
     * 新增文章分类
     * @param articleCategoryDTO
     * @return
     */
    @PostMapping("/add")
    @Operation(summary = "新增文章分类",description = "新增文章分类")
    public Result category(@Validated @RequestBody ArticleCategoryDTO articleCategoryDTO){
        log.info("新增文章分类请求:articleCategoryDTO: {}", articleCategoryDTO);
        articleCategoryService.addCategory(articleCategoryDTO);
        return  Result.success("新增文章分类操作成功！");
    }

    /**
     * 分页查询文章分类
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询文章分类",description = "该接口用于获取当前已登录用户创建的所有文章分类")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询文章分类请求:categoryPageQueryDTO: {}", categoryPageQueryDTO);
        PageResult pageResult = articleCategoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 修改文章分类
     * @return
     */
    @PutMapping("/update")
    @Operation(summary = "修改文章分类信息",description = "该接口用于更新文章分类")
    public Result update(@Validated @RequestBody ArticleCategoryDTO articleCategoryDTO){
        log.info("修改文章分类请求:{}",articleCategoryDTO);
        articleCategoryService.update(articleCategoryDTO);
        return Result.success("修改成功!");
    }

    /**
     * 删除文章分类
     * @param id
     * @return
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除文章分类",description = "该接口用于根据id删除文章分类")
    public Result delete(Long id){
        log.info("删除文章分类请求: id:{}",id);
        articleCategoryService.delete(id);
        return Result.success("删除文章分类成功!");
    }
}

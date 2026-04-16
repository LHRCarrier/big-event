package com.bubbles.server.controller.user;

import com.bubbles.common.Result;
import com.bubbles.pojo.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/article")
public class ArticleController {
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

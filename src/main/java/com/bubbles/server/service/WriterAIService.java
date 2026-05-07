package com.bubbles.server.service;

import com.bubbles.pojo.dto.WriterRequestDTO;
import com.bubbles.pojo.dto.WriterResponseDTO;

/**
 * AI撰稿服务接口
 * 定义与AI服务交互的方法
 */
public interface WriterAIService {
    
    /**
     * 调用AI撰写文章
     * 
     * @param request 撰稿请求参数
     * @return 撰稿响应结果
     */
    WriterResponseDTO writeArticle(WriterRequestDTO request);
    
    /**
     * 检查AI服务是否可用
     * 
     * @return true表示服务可用，false表示不可用
     */
    boolean isServiceAvailable();
    
    /**
     * 获取AI服务状态信息
     * 
     * @return 状态信息字符串
     */
    String getServiceStatus();
}
package com.bubbles.server.service;

import com.bubbles.pojo.dto.WriteFromHotRequestDTO;
import com.bubbles.pojo.dto.WriterRequestDTO;
import com.bubbles.pojo.dto.WriterResponseDTO;

/**
 * AI撰稿服务接口
 * 定义与AI服务交互的方法
 */
public interface WriterAIService {

    /**
     * 调用AI撰写文章
     */
    WriterResponseDTO writeArticle(WriterRequestDTO request);

    /**
     * 基于热点数据撰写文章（注入完整热点上下文）
     */
    WriterResponseDTO writeFromHot(WriteFromHotRequestDTO request);

    /**
     * 检查AI服务是否可用
     */
    boolean isServiceAvailable();

    /**
     * 获取AI服务状态信息
     */
    String getServiceStatus();
}
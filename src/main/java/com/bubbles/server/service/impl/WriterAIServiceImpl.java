package com.bubbles.server.service.impl;

import com.bubbles.pojo.dto.WriteFromHotRequestDTO;
import com.bubbles.pojo.dto.WriterRequestDTO;
import com.bubbles.pojo.dto.WriterResponseDTO;
import com.bubbles.server.service.WriterAIService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * AI撰稿服务实现类
 * 通过REST API调用Python AI服务
 */
@Slf4j
@Service
public class WriterAIServiceImpl implements WriterAIService {
    
    private final WebClient webClient;
    
    @Value("${ai.service.base-url:http://localhost:8001}")
    private String aiServiceBaseUrl;
    
    @Value("${ai.service.timeout:600000}")
    private int timeout;
    
    public WriterAIServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }
    
    @Override
    public WriterResponseDTO writeArticle(WriterRequestDTO request) {
        long start = System.currentTimeMillis();
        log.info("[调用链-1/4] Spring→Python 开始撰稿: topic={}, length={}, style={}, audience={}, refs={}",
                request.getTopic(), request.getLength(), request.getStyle(), request.getAudience(),
                request.getReferences() != null ? request.getReferences().size() : 0);

        try {
            String url = aiServiceBaseUrl + "/api/writer/write";
            log.info("[调用链-2/4] POST {} (timeout={}ms)", url, timeout);

            Mono<WriterResponseDTO> responseMono = webClient.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(WriterResponseDTO.class)
                    .timeout(Duration.ofMillis(timeout));

            WriterResponseDTO response = responseMono.block();

            long elapsed = System.currentTimeMillis() - start;
            if (response != null) {
                log.info("[调用链-3/4] Python返回成功: articleId={}, title={}, contentLen={}, model={}, elapsed={}ms",
                        response.getArticleId(), response.getTitle(),
                        response.getContent() != null ? response.getContent().length() : 0,
                        response.getModelUsed(), elapsed);
            } else {
                log.warn("[调用链-3/4] Python返回null, elapsed={}ms", elapsed);
            }
            return response;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[调用链-3/4] Python调用失败, elapsed={}ms, error={}", elapsed, e.getMessage());
            return generateMockResponse(request.getTopic());
        }
    }
    
    @Override
    public Flux<String> streamArticle(WriterRequestDTO request) {
        String url = aiServiceBaseUrl + "/api/writer/write/stream";
        log.info("[SSE流式] 开始流式撰稿: topic={}, length={}, style={}, useKnowledge={}",
                request.getTopic(), request.getLength(), request.getStyle(), request.getUseKnowledge());

        return webClient.post()
                .uri(url)
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::extractContentFromSSE)
                .timeout(Duration.ofMillis(timeout))
                .onErrorResume(e -> {
                    log.error("[SSE流式] 传输中断: {}", e.getMessage());
                    return Flux.just("\n\n[流式传输中断: " + e.getMessage() + "]");
                });
    }

    private String extractContentFromSSE(String sseData) {
        try {
            String json = sseData;
            if (json.startsWith("data: ")) {
                json = json.substring(6);
            }
            JsonNode node = new ObjectMapper().readTree(json);
            JsonNode content = node.get("content");
            return content != null ? content.asText() : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public WriterResponseDTO writeFromHot(WriteFromHotRequestDTO request) {
        long start = System.currentTimeMillis();
        log.info("[调用链-1/4] Spring→Python 热点撰稿: title={}, score={}, partition={}, descLen={}",
                request.getTitle(), request.getHotScore(), request.getPartition(),
                request.getDescription() != null ? request.getDescription().length() : 0);

        try {
            String url = aiServiceBaseUrl + "/api/writer/write-from-hot";
            log.info("[调用链-2/4] POST {} (timeout={}ms)", url, timeout);

            Mono<WriterResponseDTO> responseMono = webClient.post()
                    .uri(url)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(WriterResponseDTO.class)
                    .timeout(Duration.ofMillis(timeout));

            WriterResponseDTO response = responseMono.block();

            long elapsed = System.currentTimeMillis() - start;
            if (response != null) {
                log.info("[调用链-3/4] Python热点撰稿返回: articleId={}, title={}, contentLen={}, model={}, elapsed={}ms",
                        response.getArticleId(), response.getTitle(),
                        response.getContent() != null ? response.getContent().length() : 0,
                        response.getModelUsed(), elapsed);
            } else {
                log.warn("[调用链-3/4] Python热点撰稿返回null, elapsed={}ms", elapsed);
            }
            return response;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[调用链-3/4] Python热点撰稿失败, elapsed={}ms, error={}", elapsed, e.getMessage());
            WriterRequestDTO fallbackReq = WriterRequestDTO.builder()
                    .topic(request.getTitle())
                    .length(request.getLength() != null ? request.getLength() : 800)
                    .style(request.getStyle() != null ? request.getStyle() : "neutral")
                    .audience(request.getAudience() != null ? request.getAudience() : "general")
                    .generateSummary(request.getGenerateSummary() != null ? request.getGenerateSummary() : true)
                    .build();
            return writeArticle(fallbackReq);
        }
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            String url = aiServiceBaseUrl + "/health";
            
            Mono<String> responseMono = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5));
            
            String response = responseMono.block();
            return response != null && response.contains("running");
            
        } catch (Exception e) {
            log.warn("AI服务不可用: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getServiceStatus() {
        if (isServiceAvailable()) {
            return "AI服务运行正常";
        } else {
            return "AI服务不可用，使用Mock模式";
        }
    }
    
    /**
     * 生成Mock响应数据
     * 当AI服务不可用时使用
     * 
     * @param topic 话题
     * @return Mock响应
     */
    private WriterResponseDTO generateMockResponse(String topic) {
        log.info("AI服务不可用，返回Mock响应");
        
        String content = String.format("""
                # %s
                
                ## 引言
                
                随着时代的发展，%s已经成为人们关注的重要话题。本文将从多个角度深入探讨%s的相关内容，帮助读者全面了解这一领域的最新动态。
                
                ## 发展现状
                
                目前，%s领域正处于快速发展阶段。相关技术不断进步，应用场景日益广泛。越来越多的企业和机构开始重视%s的研究和应用，推动了整个行业的发展。
                
                ## 主要挑战
                
                尽管%s取得了显著进展，但仍然面临一些挑战。例如，技术瓶颈、人才短缺、市场竞争激烈等问题都需要得到妥善解决。
                
                ## 未来展望
                
                展望未来，%s有着广阔的发展前景。随着技术的不断突破和市场的逐步成熟，相信%s将在更多领域发挥重要作用，为社会发展做出更大贡献。
                
                ## 结语
                
                总之，%s是一个充满机遇和挑战的领域。我们需要持续关注其发展动态，积极探索创新应用，以适应不断变化的市场需求。
                
                ---
                
                *本文由AI撰稿系统自动生成（Mock模式）*
                """, topic, topic, topic, topic, topic, topic, topic, topic, topic);
        
        return WriterResponseDTO.builder()
                .articleId("mock-" + System.currentTimeMillis())
                .title(topic)
                .content(content)
                .summary(String.format("本文围绕「%s」话题展开，探讨了其发展现状、面临挑战及未来展望。", topic))
                .generatedAt(java.time.LocalDateTime.now())
                .modelUsed("mock")
                .build();
    }
}
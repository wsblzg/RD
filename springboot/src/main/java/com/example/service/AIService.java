package com.example.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * AI服务接口
 * 提供各种AI功能,包括图像识别、文本生成、内容推荐等
 */
public interface AIService {

    /**
     * 根据图片生成描述和标题
     * @param imageFile 图片文件
     * @return AI生成的描述信息 (JSON格式: {title: "", description: ""})
     */
    String generateImageDescription(MultipartFile imageFile);

    /**
     * 根据图片base64生成描述和标题
     * @param imageBase64 图片的base64编码
     * @return AI生成的描述信息 (JSON格式: {title: "", description: ""})
     */
    String generateImageDescriptionFromBase64(String imageBase64);

    /**
     * 生成文章摘要
     * @param content 文章内容
     * @param maxLength 摘要最大长度
     * @return 文章摘要
     */
    String generateArticleSummary(String content, int maxLength);

    /**
     * 根据用户行为推荐文章
     * @param userId 用户ID
     * @param limit 推荐数量
     * @return 推荐的文章ID列表 (JSON格式)
     */
    String recommendArticles(Long userId, int limit);

    /**
     * 智能客服问答
     * @param question 用户问题
     * @param context 上下文信息(可选)
     * @return AI回答
     */
    String chatbotAnswer(String question, String context);

    /**
     * 审核文本内容
     * @param content 待审核内容
     * @return 审核结果 (JSON格式: {isPass: true/false, reason: ""})
     */
    String moderateContent(String content);

    /**
     * 优化陶瓷 3D 生成提示词
     */
    String optimizeCeramicPrompt(String message, String style, String vessel);
}

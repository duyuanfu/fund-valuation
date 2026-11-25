package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_vision_config")
public class SysVisionConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 是否启用 AI 视觉识图功能 */
    private Boolean enabled;

    /** 兼容 OpenAI Vision 的 Base URL */
    private String apiBaseUrl;

    /** 大模型 API 密钥 */
    private String apiKey;

    /** 视觉模型名称，例如 glm-4v-flash, qwen-vl-plus */
    private String modelName;

    /** 单用户每日识图限制次数 */
    private Integer dailyUserLimit;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}

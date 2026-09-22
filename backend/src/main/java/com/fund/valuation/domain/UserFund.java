package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("user_fund")
public class UserFund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String fundCode;

    private Integer sortNo;
}

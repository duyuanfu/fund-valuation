package com.fund.valuation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("sys_calendar_holiday")
public class CalendarHoliday {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate holidayDate;

    private String description;

    private LocalDateTime createdAt;
}

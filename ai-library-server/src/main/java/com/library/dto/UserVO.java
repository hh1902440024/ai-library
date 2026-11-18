package com.library.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图（不返回密码）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String phone;
    private Integer role;
    private String email;
    private String avatar;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}

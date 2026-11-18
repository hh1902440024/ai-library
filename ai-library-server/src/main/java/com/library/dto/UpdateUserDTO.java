package com.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息请求
 */
@Data
public class UpdateUserDTO {

    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱长度不能超过 64 位")
    private String email;

    @Size(max = 255, message = "头像地址过长")
    private String avatar;
}

package com.library.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.library.common.ResultCode;
import com.library.dto.LoginDTO;
import com.library.dto.LoginVO;
import com.library.dto.PageResult;
import com.library.dto.RegisterDTO;
import com.library.dto.UpdateUserDTO;
import com.library.dto.UserVO;
import com.library.entity.User;
import com.library.exception.BizException;
import com.library.interceptor.UserContext;
import com.library.mapper.UserMapper;
import com.library.service.UserService;
import com.library.util.BcryptUtil;
import com.library.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /** 登录失败计数：账号 -> {失败次数, 锁定截止时间}（内存版，重启清零） */
    private static final int MAX_FAIL_COUNT = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    private record LoginAttempt(int failCount, LocalDateTime lockUntil) {
    }

    @Override
    public void register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BizException("两次输入的密码不一致");
        }
        // 代码层查重；并发场景由数据库唯一索引兜底（DuplicateKeyException 全局处理）
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new BizException("用户名已存在");
        }
        if (userMapper.findByPhone(dto.getPhone()) != null) {
            throw new BizException("手机号已注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setPassword(BcryptUtil.encode(dto.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("新用户注册成功: id={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        String account = dto.getUsername();

        // 登录锁定检查（防暴力破解：失败 5 次锁 10 分钟）
        LoginAttempt attempt = loginAttempts.get(account);
        if (attempt != null && attempt.lockUntil() != null && LocalDateTime.now().isBefore(attempt.lockUntil())) {
            throw new BizException(ResultCode.FORBIDDEN, "登录失败次数过多，账号已锁定，请 10 分钟后再试");
        }

        // 支持用户名或手机号登录
        User user = userMapper.findByUsername(account);
        if (user == null) {
            user = userMapper.findByPhone(account);
        }

        if (user == null || !BcryptUtil.matches(dto.getPassword(), user.getPassword())) {
            recordLoginFail(account);
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        loginAttempts.remove(account);
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, toVO(user));
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO updateProfile(UpdateUserDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        userMapper.updateProfile(userId, dto.getEmail(), dto.getAvatar());
        return getCurrentUser();
    }

    @Override
    public void updatePassword(String oldPassword, String newPassword) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        User user = userMapper.findById(userId);
        if (user == null || !BcryptUtil.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码错误");
        }
        userMapper.updatePassword(userId, BcryptUtil.encode(newPassword));
    }

    @Override
    public PageResult<UserVO> listUsers(String keyword, int page, int size) {
        PageHelper.startPage(page, size);
        List<User> users = userMapper.findByPage(keyword);
        PageInfo<User> pageInfo = new PageInfo<>(users);
        List<UserVO> vos = users.stream().map(this::toVO).toList();
        return PageResult.of(pageInfo.getTotal(), vos);
    }

    @Override
    public void updateStatus(Long operatorId, Long userId, Integer status) {
        if (operatorId.equals(userId)) {
            throw new BizException("不能禁用/启用自己的账号");
        }
        if (userMapper.findById(userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        userMapper.updateStatus(userId, status);
        log.info("管理员 {} 修改用户 {} 状态为 {}", operatorId, userId, status);
    }

    @Override
    public void updateRole(Long operatorId, Long userId, Integer role) {
        if (operatorId.equals(userId)) {
            throw new BizException("不能调整自己的角色");
        }
        if (userMapper.findById(userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        userMapper.updateRole(userId, role);
        log.info("管理员 {} 修改用户 {} 角色为 {}", operatorId, userId, role);
    }

    private void recordLoginFail(String account) {
        loginAttempts.compute(account, (k, old) -> {
            int count = old == null || old.lockUntil() != null ? 1 : old.failCount() + 1;
            if (count >= MAX_FAIL_COUNT) {
                return new LoginAttempt(count, LocalDateTime.now().plus(LOCK_DURATION));
            }
            return new LoginAttempt(count, null);
        });
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}

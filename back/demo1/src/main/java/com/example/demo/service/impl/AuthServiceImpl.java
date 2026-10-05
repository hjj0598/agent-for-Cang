package com.example.demo.service.impl;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.pojo.AppUser;
import com.example.demo.service.AuthService;
import com.example.demo.utils.JwtUtil;
import com.example.demo.utils.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {
     //设置默认角色是是用户
    private static final String DEFAULT_USER_ROLE = "USER";
    private static final String RESET_PASSWORD_KEY_PREFIX = "reset-password:";
     //设置验证码的时间只有1分钟
    private static final long RESET_CODE_EXPIRE_MINUTES = 1;
    //设置登录失败的key
    private static final String LOGIN_FAIL_KEY_PREFIX = "login-fail:";
    //登录被锁的key
    private static final String LOGIN_LOCK_KEY_PREFIX = "login-lock:";
    //一分钟内发送登录的次数
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    private static final long LOGIN_FAIL_WINDOW_MINUTES = 5;
    private static final long LOGIN_LOCK_MINUTES = 10;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void register(LoginRequest request) {
        AppUser exists = userMapper.findByUsername(request.getUsername());
        if (exists != null) {
            throw new RuntimeException("用户名已存在");
        }

        AppUser user = new AppUser();
        user.setUsername(request.getUsername());
        // 数据库里只保存 BCrypt 密文，不保存用户输入的明文密码
        user.setPasswordHash(PasswordUtil.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setRole(DEFAULT_USER_ROLE); //默认设置用户

        userMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        checkLoginLocked(request.getUsername());

        AppUser user = userMapper.findByUsername(request.getUsername());

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!checkPasswordAndUpgradeIfNecessary(request.getPassword(), user)) {
            recordLoginFailure(request.getUsername());
        }

        clearLoginFailure(request.getUsername());

        String role = resolveRole(user.getRole());
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), role);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRole(role);

        return response;
    }

    @Override
    public String generateResetCode(String username) {
        AppUser user = userMapper.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String redisKey = buildResetPasswordKey(username);
        Long expireSeconds = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

        if (expireSeconds != null && expireSeconds > 0) {
            throw new RuntimeException("验证码已发送，请" + expireSeconds + "秒后再试");
        }

        if (expireSeconds != null && expireSeconds == -1) {
            stringRedisTemplate.delete(redisKey);
        }

        String code = generateSixDigitCode();

        stringRedisTemplate.opsForValue().set(
                redisKey,
                code,
                RESET_CODE_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        return code;
    }

    @Override
    public void resetPassword(String username, String code, String newPassword) {
        AppUser user = userMapper.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String redisKey = buildResetPasswordKey(username);
        String realCode = stringRedisTemplate.opsForValue().get(redisKey);

        if (realCode == null) {
            throw new RuntimeException("验证码已过期，请重新获取");
        }

        if (!realCode.equals(code)) {
            throw new RuntimeException("验证码错误");
        }

        userMapper.updatePasswordHashByUsername(username, PasswordUtil.encode(newPassword));
        stringRedisTemplate.delete(redisKey);
        clearLoginFailure(username);
    }

    private boolean checkPasswordAndUpgradeIfNecessary(String rawPassword, AppUser user) {
        String passwordHash = user.getPasswordHash();

        if (PasswordUtil.isEncoded(passwordHash)) {
            return PasswordUtil.matches(rawPassword, passwordHash);
        }

        // 兼容旧账号：如果数据库里还是明文密码，允许本次登录，并立即升级为 BCrypt 密文
        if (passwordHash != null && passwordHash.equals(rawPassword)) {
            userMapper.updatePasswordHash(user.getId(), PasswordUtil.encode(rawPassword));
            return true;
        }

        return false;
    }

    private String generateSixDigitCode() {
        int number = new Random().nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    private String buildResetPasswordKey(String username) {
        return RESET_PASSWORD_KEY_PREFIX + username;
    }

    private void checkLoginLocked(String username) {
        String lockKey = buildLoginLockKey(username);
        Long lockSeconds = stringRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);

        if (lockSeconds != null && lockSeconds > 0) {
            throw new RuntimeException("密码错误次数过多，请" + lockSeconds + "秒后再试");
        }

        if (lockSeconds != null && lockSeconds == -1) {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void recordLoginFailure(String username) {
        String failKey = buildLoginFailKey(username);
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);

        if (failCount != null && failCount == 1) {
            stringRedisTemplate.expire(failKey, LOGIN_FAIL_WINDOW_MINUTES, TimeUnit.MINUTES);
        }

        if (failCount != null && failCount >= MAX_LOGIN_FAIL_COUNT) {
            stringRedisTemplate.opsForValue().set(
                    buildLoginLockKey(username),
                    "locked",
                    LOGIN_LOCK_MINUTES,
                    TimeUnit.MINUTES
            );
            stringRedisTemplate.delete(failKey);
            throw new RuntimeException("密码错误次数过多，账号已锁定10分钟");
        }

        long remainingCount = MAX_LOGIN_FAIL_COUNT - (failCount == null ? 0 : failCount);
        throw new RuntimeException("密码错误，还剩" + remainingCount + "次机会");
    }

    private void clearLoginFailure(String username) {
        stringRedisTemplate.delete(buildLoginFailKey(username));
        stringRedisTemplate.delete(buildLoginLockKey(username));
    }

    private String buildLoginFailKey(String username) {
        return LOGIN_FAIL_KEY_PREFIX + username;
    }

    private String buildLoginLockKey(String username) {
        return LOGIN_LOCK_KEY_PREFIX + username;
    }

    private String resolveRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return DEFAULT_USER_ROLE;
        }

        return role.trim();
    }
}

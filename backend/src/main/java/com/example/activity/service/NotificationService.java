package com.example.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.activity.entity.Notification;
import com.example.activity.exception.BusinessException;
import com.example.activity.mapper.NotificationMapper;
import com.example.activity.security.AuthenticatedUser;
import com.example.activity.vo.NotificationView;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public void create(
            Long userId,
            String type,
            String title,
            String content,
            String targetType,
            Long targetId
    ) {
        create(userId, type, title, content, targetType, targetId, null);
    }

    public void createDeduplicated(
            Long userId,
            String type,
            String title,
            String content,
            String targetType,
            Long targetId,
            String dedupKey
    ) {
        create(userId, type, title, content, targetType, targetId, dedupKey);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> list(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreatedAt)
                        .last("LIMIT 100"))
                .stream()
                .map(NotificationView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Authentication authentication) {
        Long userId = currentUserId(authentication);
        return notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getReadAt));
    }

    @Transactional
    public NotificationView markRead(Long id, Authentication authentication) {
        Long userId = currentUserId(authentication);
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !userId.equals(notification.getUserId())) {
            throw new BusinessException(404, "通知不存在");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
        return NotificationView.from(notification);
    }

    @Transactional
    public void markAllRead(Authentication authentication) {
        Long userId = currentUserId(authentication);
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .isNull(Notification::getReadAt)
                .set(Notification::getReadAt, LocalDateTime.now()));
    }

    private void create(
            Long userId,
            String type,
            String title,
            String content,
            String targetType,
            Long targetId,
            String dedupKey
    ) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setDedupKey(dedupKey);
        notification.setCreatedAt(LocalDateTime.now());
        try {
            notificationMapper.insert(notification);
        } catch (DuplicateKeyException exception) {
            if (dedupKey == null) {
                throw exception;
            }
        }
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new BusinessException(401, "请先登录");
        }
        return authenticatedUser.user().getId();
    }
}

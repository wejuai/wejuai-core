package com.wejuai.core.service;

import com.wejuai.core.repository.mongo.SystemMessageRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.entity.mongo.SystemMessage;
import com.wejuai.entity.mysql.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author ZM.Wang
 */
@Service
public class MessageService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserRepository userRepository;
    private final SystemMessageRepository systemMessageRepository;

    public MessageService(UserRepository userRepository, SystemMessageRepository systemMessageRepository) {
        this.userRepository = userRepository;
        this.systemMessageRepository = systemMessageRepository;
    }

    public void sendSystemMessage(User user, String content) {
        userRepository.save(user.addMsg());
        new Thread(() -> saveSystemMessage(user.getId(), content, 0)).start();
    }

    private void saveSystemMessage(String userId, String content, int count) {
        try {
            systemMessageRepository.save(new SystemMessage(content, userId, false));
        } catch (Exception e) {
            if (count > 4) {
                logger.error("多次创建用户系统消息错误", e);
                return;
            }
            saveSystemMessage(userId, content, ++count);
        }
    }
}


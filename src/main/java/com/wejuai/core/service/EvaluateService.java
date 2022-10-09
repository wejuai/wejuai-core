package com.wejuai.core.service;

import com.endofmaster.rest.exception.BadRequestException;
import com.wejuai.core.repository.mysql.ArticleRepository;
import com.wejuai.core.repository.mysql.EvaluateRepository;
import com.wejuai.core.repository.mysql.OrdersRepository;
import com.wejuai.core.repository.mysql.UserRepository;
import com.wejuai.dto.request.SaveEvaluateRequest;
import com.wejuai.dto.response.EvaluateInfo;
import com.wejuai.entity.mongo.AppType;
import com.wejuai.entity.mysql.Article;
import com.wejuai.entity.mysql.Evaluate;
import com.wejuai.entity.mysql.Orders;
import com.wejuai.entity.mysql.RewardDemand;
import com.wejuai.entity.mysql.RewardDemandStatus;
import com.wejuai.entity.mysql.RewardSubmission;
import com.wejuai.entity.mysql.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ZM.Wang
 */
@Service
public class EvaluateService {

    private final EvaluateRepository evaluateRepository;
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final ArticleRepository articleRepository;

    private final UserService userService;
    private final RewardDemandService rewardDemandService;

    public EvaluateService(EvaluateRepository evaluateRepository, UserRepository userRepository, OrdersRepository ordersRepository, ArticleRepository articleRepository, UserService userService, RewardDemandService rewardDemandService) {
        this.evaluateRepository = evaluateRepository;
        this.userRepository = userRepository;
        this.ordersRepository = ordersRepository;
        this.articleRepository = articleRepository;
        this.userService = userService;
        this.rewardDemandService = rewardDemandService;
    }

    /** 文章是购买者评价发布者 */
    @Transactional
    public void articleEvaluate(SaveEvaluateRequest request) {
        User user = userService.getUser(request.getUserId());
        Article article = articleRepository.findById(request.getAppId()).orElseThrow(() -> new BadRequestException("没有该文章: " + request.getAppId()));
        User recipient = article.getUser();
        if (user.equals(article.getUser())) {
            throw new BadRequestException("无法自己评价自己");
        }
        Orders orders = ordersRepository.findByUserAndAppTypeAndAppIdAndNullifyFalse(user, AppType.ARTICLE, request.getAppId());
        if (orders == null) {
            throw new BadRequestException("该文章你尚未兑换");
        }
        long count = evaluateRepository.countByOrdersAndEvaluator(orders, user);
        if (count > 0) {
            throw new BadRequestException("该付费文章已经评价过");
        }
        Evaluate evaluate = new Evaluate(AppType.ARTICLE, request.getScore(), user, request.getContent());
        evaluateRepository.save(evaluate.setOrders(orders));
        userRepository.save(recipient.sellEvaluate(request.getScore()));
    }

    /** 悬赏是发布者评价提交者 */
    @Transactional
    public void rewardDemandEvaluate(SaveEvaluateRequest request) {
        RewardSubmission rewardSubmission = rewardDemandService.getRewardSubmission(request.getAppId());
        RewardDemand rewardDemand = rewardSubmission.getRewardDemand();
        if (rewardDemand == null) {
            throw new BadRequestException("没有该悬赏");
        }
        User user = userService.getUser(request.getUserId());
        if (!rewardDemand.getUser().equals(user)) {
            throw new BadRequestException("该悬赏并非您发布，无权评价");
        }
        if (rewardDemand.getStatus() != RewardDemandStatus.END) {
            throw new BadRequestException("当前状态下无法评价");
        }
        User recipient = rewardSubmission.getUser();
        long count = evaluateRepository.countByRewardSubmissionAndEvaluator(rewardSubmission, user);
        if (count > 0) {
            throw new BadRequestException("该悬赏答案已经评价过");
        }
        Evaluate evaluate = new Evaluate(AppType.REWARD_DEMAND, request.getScore(), user, request.getContent())
                .setRewardSubmission(rewardSubmission);
        evaluateRepository.save(evaluate);
        userRepository.save(recipient.sellEvaluate(request.getScore()));
    }

    public Page<EvaluateInfo> getEvaluates(String appId, Pageable pageable) {
        return evaluateRepository.findByAppTypeAndOrders_AppId(AppType.ARTICLE, appId, pageable).map(EvaluateInfo::new);
    }

}

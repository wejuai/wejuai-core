package com.wejuai.core.web;

import com.wejuai.core.service.OrderService;
import com.wejuai.dto.request.SaveOrderAppealRequest;
import com.wejuai.dto.request.SaveWithdrawalRequest;
import com.wejuai.dto.response.*;
import com.wejuai.entity.mongo.trade.TradeStatus;
import com.wejuai.entity.mysql.ApplyStatus;
import com.wejuai.entity.mysql.ChannelQueryType;
import com.wejuai.entity.mysql.OrdersPageType;
import com.wejuai.entity.mysql.OrdersType;
import com.wejuai.entity.mysql.WithdrawalType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Date;

/**
 * @author ZM.Wang
 */
@Api(tags = "订单相关")
@RestController
@RequestMapping("/orders")
public class OrdersController {

    private final OrderService orderService;

    public OrdersController(OrderService orderService) {
        this.orderService = orderService;
    }

    @ApiOperation("订单列表接口")
    @GetMapping
    public Page<OrdersInfo> getOrders(@RequestParam(required = false, defaultValue = "") String userId,
                                      @RequestParam(required = false, defaultValue = "") OrdersType type,
                                      @RequestParam(required = false, defaultValue = "") Boolean income,
                                      @RequestParam(required = false, defaultValue = "true")
                                      @ApiParam("是否不展示0积分单,默认true") boolean notZero,
                                      @RequestParam(required = false, defaultValue = "0") int page,
                                      @RequestParam(required = false, defaultValue = "10") int size,
                                      @RequestParam(required = false, defaultValue = "") Long start,
                                      @RequestParam(required = false, defaultValue = "") Long end) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Date startDate = null;
        Date endDate = null;
        if (start != null) {
            startDate = new Date(start);
        }
        if (end != null) {
            endDate = new Date(end);
        }
        return orderService.getOrders(userId, type, income, notZero, startDate, endDate, pageable);
    }

    @ApiOperation("充值订单查询")
    @GetMapping("/charge")
    public Slice<ChargeListInfo> getCharges(@RequestParam(required = false, defaultValue = "") String userId,
                                            @RequestParam(required = false, defaultValue = "") TradeStatus status,
                                            @RequestParam(required = false, defaultValue = "") ChannelQueryType channelType,
                                            @RequestParam(required = false, defaultValue = "") Long start,
                                            @RequestParam(required = false, defaultValue = "") Long end,
                                            @RequestParam(required = false, defaultValue = "0") long page,
                                            @RequestParam(required = false, defaultValue = "10") long size) {
        Date startDate = null;
        Date endDate = null;
        if (start != null) {
            startDate = new Date(start);
        }
        if (end != null) {
            endDate = new Date(end);
        }
        return orderService.getCharges(userId, status, channelType, startDate, endDate, page, size);
    }

    @ApiOperation("提现申请")
    @PostMapping("/withdrawal")
    public void withdrawal(@RequestBody SaveWithdrawalRequest request) {
        orderService.withdrawalApply(request);
    }

    @ApiOperation("提现列表")
    @GetMapping("/withdrawal")
    public Slice<WithdrawalInfo> getWithdrawals(@RequestParam(required = false, defaultValue = "") String userId,
                                                @RequestParam(required = false, defaultValue = "") ApplyStatus status,
                                                @RequestParam(required = false, defaultValue = "") String id,
                                                @RequestParam(required = false, defaultValue = "") WithdrawalType channelType,
                                                @RequestParam(required = false, defaultValue = "") Long start,
                                                @RequestParam(required = false, defaultValue = "") Long end,
                                                @RequestParam(required = false, defaultValue = "") int page,
                                                @RequestParam(required = false, defaultValue = "") int size) {
        Date startDate = null;
        Date endDate = null;
        if (start != null) {
            startDate = new Date(start);
        }
        if (end != null) {
            endDate = new Date(end);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return orderService.getWithdrawals(id, userId, status, channelType, startDate, endDate, pageable);
    }

    @ApiOperation("订单申诉")
    @PostMapping("/orderAppeal")
    public void orderAppeal(@RequestBody SaveOrderAppealRequest request) {
        orderService.orderAppeal(request);
    }

    @ApiModelProperty("获取订单申诉列表")
    @GetMapping("/orderAppeal")
    public Page<OrderAppealInfo> getOrderAppeals(@RequestParam(required = false, defaultValue = "") String userId,
                                                 @RequestParam(required = false, defaultValue = "")
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                                 @RequestParam(required = false, defaultValue = "")
                                                 @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
                                                 @RequestParam(required = false, defaultValue = "") ApplyStatus status,
                                                 @RequestParam(required = false, defaultValue = "") OrdersPageType type,
                                                 @RequestParam(required = false, defaultValue = "0") int page,
                                                 @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return orderService.getOrderAppeals(userId, start, end, status, type, pageable);
    }

    @ApiOperation("用户积分信息")
    @GetMapping("/userIntegral/{userId}")
    public UserIntegralInfo sumUserWithdrawableIntegral(@PathVariable String userId) {
        return orderService.sumUserWithdrawableIntegral(userId);
    }

    @ApiOperation("已购列表")
    @GetMapping("/purchased")
    public Page<ArticleListInfo> getPurchasedList(@RequestParam(required = false, defaultValue = "") String userId,
                                                  @RequestParam(required = false, defaultValue = "") String titleStr,
                                                  @RequestParam(required = false, defaultValue = "0") int page,
                                                  @RequestParam(required = false, defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        return orderService.getPurchasedList(userId, titleStr, pageable);
    }
}

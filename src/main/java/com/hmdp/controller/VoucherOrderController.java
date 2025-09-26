package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource // 注入 voucherOrderService，为什么能找到这个IVoucherOrderService对象？
    //Spring会执行以下步骤：
    // 类型匹配: Spring查找容器中所有实现了IVoucherOrderService接口的Bean
    // 找到实现: 发现VoucherOrderServiceImpl实现了这个接口
    // 自动注入: 将VoucherOrderServiceImpl的实例注入到Controller中
    private IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}

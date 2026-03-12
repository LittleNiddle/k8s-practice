package com.beyond.order_system.ordering.service;

import com.beyond.order_system.common.service.SseAlarmService;
import com.beyond.order_system.member.domain.Member;
import com.beyond.order_system.member.repository.MemberRepository;
import com.beyond.order_system.ordering.domain.Ordering;
import com.beyond.order_system.ordering.domain.OrderDetail;
import com.beyond.order_system.ordering.dtos.OrderCreateDto;
import com.beyond.order_system.ordering.dtos.OrderListDto;
import com.beyond.order_system.ordering.repository.OrderingDetailRepository;
import com.beyond.order_system.ordering.repository.OrderingRepository;
import com.beyond.order_system.product.domain.Product;
import com.beyond.order_system.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final OrderingDetailRepository orderingDetailRepository;
    private final ProductRepository productRepository;
    private final SseAlarmService sseAlarmService;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, MemberRepository memberRepository, OrderingDetailRepository orderingDetailRepository, ProductRepository productRepository, SseAlarmService sseAlarmService, @Qualifier("stockInventory") RedisTemplate<String, String> redisTemplate) {
        this.orderingRepository = orderingRepository;
        this.memberRepository = memberRepository;
        this.orderingDetailRepository = orderingDetailRepository;
        this.productRepository = productRepository;
        this.sseAlarmService = sseAlarmService;
        this.redisTemplate = redisTemplate;
    }

    public Long create( List<OrderCreateDto> orderCreateDtoList){
//        토큰을 gateway에서 검사
        String email = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("member is not found"));
        Ordering ordering = Ordering.builder()
                .member(member)
                .build();
        orderingRepository.save(ordering);

        for (OrderCreateDto dto : orderCreateDtoList){
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(()->new EntityNotFoundException("entity is not found"));
            if(product.getStockQuantity() < dto.getProductCount()){
                throw new IllegalArgumentException("재고가 부족합니다");
            }
            product.updateStockQuantity(dto.getProductCount());
            OrderDetail orderDetail = OrderDetail.builder()
                    .ordering(ordering)
                    .product(product)
                    .quantity(dto.getProductCount())
                    .build();
            orderingDetailRepository.save(orderDetail);
        }

        return ordering.getId();
    }

    public List<OrderListDto> findAll() {
        return orderingRepository.findAll().stream().map(OrderListDto::fromEntity).toList();
    }

    public List<OrderListDto> myOrders(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new NoSuchElementException("해당 이메일이 없습니다."));
        return orderingRepository.findAllByMember(member).stream().map(OrderListDto::fromEntity).toList();
    }
}

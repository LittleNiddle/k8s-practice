package com.beyond.order_system.ordering.service;

import com.beyond.order_system.common.service.SseAlarmService;

import com.beyond.order_system.ordering.domain.Ordering;
import com.beyond.order_system.ordering.domain.OrderDetail;
import com.beyond.order_system.ordering.dtos.OrderCreateDto;
import com.beyond.order_system.ordering.dtos.OrderListDto;
import com.beyond.order_system.ordering.dtos.ProductDto;
import com.beyond.order_system.ordering.feignclients.ProductFeignClient;
import com.beyond.order_system.ordering.repository.OrderingDetailRepository;
import com.beyond.order_system.ordering.repository.OrderingRepository;

import jakarta.persistence.EntityNotFoundException;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class OrderingService {
    private final OrderingRepository orderingRepository;

    private final OrderingDetailRepository orderingDetailRepository;

    private final SseAlarmService sseAlarmService;
    private final RestTemplate restTemplate;
    private final ProductFeignClient productFeignClient;
    private final KafkaTemplate kafkaTemplate;

    @Autowired
    public OrderingService(OrderingRepository orderingRepository, OrderingDetailRepository orderingDetailRepository, SseAlarmService sseAlarmService, RestTemplate restTemplate, ProductFeignClient productFeignClient, KafkaTemplate kafkaTemplate) {
        this.orderingRepository = orderingRepository;
        this.orderingDetailRepository = orderingDetailRepository;
        this.sseAlarmService = sseAlarmService;
        this.restTemplate = restTemplate;
        this.productFeignClient = productFeignClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Long create( List<OrderCreateDto> orderCreateDtoList, String email){
        Ordering ordering = Ordering.builder()
                .memberEmail(email)
                .build();
        orderingRepository.save(ordering);

        for (OrderCreateDto dto : orderCreateDtoList){
//            1. 재고 조회(동기 요청 - http 요청)
//            http://localhost:8080/product-service : apigateway을 통한 호출
//            http://product-service : eureka에게 질의 후 product-service 직접 호출
            String endpoint = "http://product-service/product/detail/" + dto.getProductId();
            HttpHeaders headers = new HttpHeaders();
//            HttpEntity : header + body
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);
            ResponseEntity<ProductDto> responseEntity = restTemplate.exchange(endpoint, HttpMethod.GET, httpEntity, ProductDto.class);
            ProductDto product = responseEntity.getBody();

            if(product.getStockQuantity() < dto.getProductCount()){
                throw new IllegalArgumentException("재고가 부족합니다");
            }
//            2. 주문 발생
            OrderDetail orderDetail = OrderDetail.builder()
                    .ordering(ordering)
                    .productName(product.getName())
                    .productId(dto.getProductId())
                    .quantity(dto.getProductCount())
                    .build();
            orderingDetailRepository.save(orderDetail);
//            3. 재고 감소 요청(동기 - http요청/비동기-이벤트(메시지)기반 모두 가능)
            String endpoint2 = "http://product-service/product/updatestock";
            HttpHeaders headers2 = new HttpHeaders();
            headers2.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<OrderCreateDto> httpEntity2 = new HttpEntity<>(dto, headers2);
            restTemplate.exchange(endpoint2, HttpMethod.PUT, httpEntity2, Void.class);
        }

        return ordering.getId();
    }

    public Long createFeign( List<OrderCreateDto> orderCreateDtoList, String email){
        Ordering ordering = Ordering.builder()
                .memberEmail(email)
                .build();
        orderingRepository.save(ordering);

        for (OrderCreateDto dto : orderCreateDtoList){
            ProductDto product = productFeignClient.getProductById(dto.getProductId());
            if(product.getStockQuantity() < dto.getProductCount()){
                throw new IllegalArgumentException("재고가 부족합니다");
            }
            OrderDetail orderDetail = OrderDetail.builder()
                    .ordering(ordering)
                    .productName(product.getName())
                    .productId(dto.getProductId())
                    .quantity(dto.getProductCount())
                    .build();
            orderingDetailRepository.save(orderDetail);
            //        feign을 사용한 동기적 재고 감소 요청
            //        productFeignClient.updateStockQuantity(dto);
            //        kafka를 활용한 비동기적 재고 감소
            kafkaTemplate.send("stock-update-topic", dto);
        }

        return ordering.getId();
    }

    public List<OrderListDto> findAll() {
        return orderingRepository.findAll().stream().map(OrderListDto::fromEntity).toList();
    }

    public List<OrderListDto> myOrders(String email) {
        return orderingRepository.findAllByMemberEmail(email).stream().map(OrderListDto::fromEntity).toList();
    }
}

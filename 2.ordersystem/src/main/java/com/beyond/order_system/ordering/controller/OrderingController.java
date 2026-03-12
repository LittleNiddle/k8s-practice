package com.beyond.order_system.ordering.controller;

import com.beyond.order_system.ordering.dtos.OrderCreateDto;
import com.beyond.order_system.ordering.dtos.OrderListDto;
import com.beyond.order_system.ordering.service.OrderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ordering")
public class OrderingController {
    private final OrderingService orderingService;
    @Autowired
    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

//    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody List<OrderCreateDto> listReqDtoList){
        Long id = orderingService.create(listReqDtoList);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> findAll(){
        List<OrderListDto> dtoList = orderingService.findAll();
        return ResponseEntity.status(HttpStatus.OK).body(dtoList);
    }

    @GetMapping("/myorders")
    public ResponseEntity<?> myOrders(@AuthenticationPrincipal String principal){
        List<OrderListDto> dtoList = orderingService.myOrders(principal);
        return ResponseEntity.status(HttpStatus.OK).body(dtoList);
    }
}

package com.beyond.order_system.ordering.dtos;

import com.beyond.order_system.ordering.domain.OrderStatus;
import com.beyond.order_system.ordering.domain.Ordering;
import com.beyond.order_system.ordering.domain.OrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderListDto {
    private Long id;
    private String memberEmail;
    private OrderStatus orderStatus;
    @Builder.Default
    private List<OrderDetailDto> orderDetailDtos = new ArrayList<>();

    public static OrderListDto fromEntity(Ordering ordering){
        List<OrderDetailDto> orderDetailDtos = new ArrayList<>();
        for(OrderDetail orderDetail : ordering.getOrderDetailList()){
            orderDetailDtos.add(OrderDetailDto.fromEntity(orderDetail));
        }

        OrderListDto orderListDto = OrderListDto.builder()
                .id(ordering.getId())
                .memberEmail(ordering.getMember().getEmail())
                .orderStatus(ordering.getOrderStatus())
                .orderDetailDtos(orderDetailDtos)
                .build();

        return orderListDto;
    }
}

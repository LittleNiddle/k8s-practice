package com.beyond.order_system.ordering.dtos;

import com.beyond.order_system.ordering.domain.Ordering;
import com.beyond.order_system.ordering.domain.OrderDetail;
import com.beyond.order_system.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class OrderCreateDto {
    private Long productId;
    private int productCount;

    public OrderDetail toEntity(Ordering ordering, Product product){
        return OrderDetail.builder()
                .ordering(ordering)
                .product(product)
                .quantity(this.productCount)
                .build();
    }
}

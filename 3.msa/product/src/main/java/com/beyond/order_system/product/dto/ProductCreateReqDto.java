package com.beyond.order_system.product.dto;

import com.beyond.order_system.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductCreateReqDto {
    private String name;
    private int price;
    private String category;
    private int stockQuantity;
    private MultipartFile productImage;

    public Product toEntity(String email){
        return Product.builder()
                .name(this.name)
                .category(this.category)
                .memberEmail(email)
                .price(this.price)
                .stockQuantity(this.stockQuantity)
                .build();
    }
}

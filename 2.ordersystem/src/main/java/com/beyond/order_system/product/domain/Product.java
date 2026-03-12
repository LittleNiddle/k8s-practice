package com.beyond.order_system.product.domain;

import com.beyond.order_system.common.domain.BaseTimeEntity;
import com.beyond.order_system.member.domain.Member;
import com.beyond.order_system.ordering.domain.OrderDetail;
import com.beyond.order_system.product.dto.ProductUpdateReqDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter @ToString
@Builder
@Entity
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private Integer price;
    private String category;
    private Integer stockQuantity;
    private String image_path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id", foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT), nullable = false)
    private Member member;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDetail> orderDetailList = new ArrayList<>();

    public void updateProfileImageUrl(String url){
        this.image_path = url;
    }

    public void updateStockQuantity(int orderQuantity){
        this.stockQuantity = this.stockQuantity-orderQuantity;
    }

    public void updateProduct(ProductUpdateReqDto dto) {
        this.name = dto.getName();
        this.price = dto.getPrice();
        this.category = dto.getCategory();
        this.stockQuantity = dto.getStockQuantity();
    }
}

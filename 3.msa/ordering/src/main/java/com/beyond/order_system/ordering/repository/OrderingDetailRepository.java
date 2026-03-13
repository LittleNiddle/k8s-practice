package com.beyond.order_system.ordering.repository;

import com.beyond.order_system.ordering.domain.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderingDetailRepository extends JpaRepository<OrderDetail, Long> {
}

package com.equity.order.item;

import org.springframework.data.repository.CrudRepository;

public interface LimitOrderRepository extends CrudRepository<LimitOrder, Long> {
}
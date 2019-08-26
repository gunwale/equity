package com.equity.order.book;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.equity.order.book.OrderBook.Status;

public interface OrderBookRepository extends CrudRepository<OrderBook, Long> {

	public Optional<OrderBook> findByIdAndStatus(Long id, Status status);
}
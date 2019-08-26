package com.equity.order.book;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import com.equity.order.history.OrderHistory;
import com.equity.order.history.OrderHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

// using spring data rest for this demo only
@RepositoryRestController
@RequestMapping(OrderBookService.ENTITIES)
@Log4j2
public class OrderBookService {

	public final static String ENTITIES = "orderBooks";

	private final OrderBookRepository orderBookRepository;
	private final OrderHistoryRepository orderHistoryRepository;

	// both will init at somewhere else for specific usage
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final RestTemplate restTemplate;

	public OrderBookService(OrderBookRepository orderBookRepository,
			OrderHistoryRepository orderHistoryRepository, RestTemplate restTemplate) {
		this.orderBookRepository = orderBookRepository;
		this.orderHistoryRepository = orderHistoryRepository;
		this.restTemplate = restTemplate;
	}

	// another service to
	// overwrite close order book to check only can closed if there are orders?

	@PutMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@RequestMapping("/{orderBookId}/order")
	public ResponseEntity<?> addOrders(@PathVariable String orderBookId,
			@RequestBody Order order)
			throws NumberFormatException, JsonProcessingException {

		log.info("Adding orders {} to {}", order, orderBookId);

		// find only OPEN status order book
		// right now just throw not present can throw better error
		final OrderBook orderBook = orderBookRepository
				.findByIdAndStatus(Long.parseLong(orderBookId), OrderBook.Status.OPEN)
				.orElse(null);

		return addOrderHistory(orderBook, order);
	}

	@PutMapping(consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
	@RequestMapping("/{orderBookId}/execution")
	public ResponseEntity<?> addExecutions(@PathVariable String orderBookId,
			@RequestBody Order order)
			throws NumberFormatException, JsonProcessingException {

		log.info("Adding executions {} to {}", order, orderBookId);

		// find only CLOSED status order book
		// right now just throw not present can throw better error
		final OrderBook orderBook = orderBookRepository
				.findByIdAndStatus(Long.parseLong(orderBookId), OrderBook.Status.CLOSED)
				.orElse(null);

		return addOrderHistory(orderBook, order);
	}

	public ResponseEntity<?> addOrderHistory(OrderBook orderBook, Order order)
			throws JsonProcessingException {
		if (orderBook == null) {
			return ResponseEntity.badRequest().build();
		}

		// just to not throw at lambda
		// can use a throwing function at lambda
		final String orderBookString = objectMapper.writeValueAsString(orderBook);

		// can be rest api call, using repo for this demo only
		orderHistoryRepository.saveAll((List<OrderHistory>) order.itemList.stream()
				.map(orderItem -> OrderHistory.builder().orderBook(orderBookString)
						.orderItem(get(orderItem.location)).build())
				.collect(Collectors.toList()));

		// normally rest association returns no content
		return ResponseEntity.noContent().build();
	}

	// get full object of orders
	// can do some optimization
	public String get(String url) {
		log.info("Get data from {}", url);

		return restTemplate.getForObject(url, String.class);
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Order {
		private List<Item> itemList;

		@Data
		@Builder
		@NoArgsConstructor
		@AllArgsConstructor
		public static class Item {
			private String location;
		}

		public String toJson() throws JsonProcessingException {
			return objectMapper.writeValueAsString(this);
		}
	}

}

package com.equity.order.history;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.equity.order.book.OrderBook;
import com.equity.order.history.OrderHistoryService.OrderHistory.OrderItem;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

// order history is the immutable view for events
// it is an aggregate that can show for example which order item at which order book
@RepositoryRestController
@RequestMapping(OrderHistoryService.ENTITIES)
@Log4j2
public class OrderHistoryService {

	public final static String ENTITIES = "orderHistories";

	// will init at somewhere else for specific usage
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final OrderHistoryRepository orderHistoryRepository;

	public OrderHistoryService(OrderHistoryRepository orderHistoryRepository) {
		this.orderHistoryRepository = orderHistoryRepository;
	}

	// just to show a better data representation than default spring data rest
	// no pagination for now
	@GetMapping
	public ResponseEntity<List<OrderHistory>> findAll() {

		log.info("Getting order history");

		return ResponseEntity.ok(StreamSupport
				.stream(orderHistoryRepository.findAll().spliterator(), false)
				.map(orderHistory -> OrderHistory.builder()
						.orderBook(buildOrderBook(orderHistory.getOrderBook()))
						.orderItem(buildOrderItem(orderHistory.getOrderItem()))
						.entryDate(orderHistory.getCreatedDate()).build())
				.collect(Collectors.toList()));
	}

	// can also have like list all order item from order book
	// /orderHistories/orderBook/123

	public OrderBook buildOrderBook(String object) {
		try {
			return objectMapper.readValue(object, OrderBook.class);
		} catch (IOException e) {
			// do some logging
			throw new RuntimeException();
		}
	}

	// do some unit test
	public OrderItem buildOrderItem(String object) {
		DocumentContext documentContext = JsonPath.using(
				Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build())
				.parse(object);
		// can customize object mapper to default big decimal instead of double
		BigDecimal price = documentContext.read("$.price") != null
				? new BigDecimal((Double) documentContext.read("$.price"),
						MathContext.DECIMAL64)
				: null;

		return OrderItem.builder().location(documentContext.read("$._links.self.href"))
				.quantity(documentContext.read("$.quantity")).price(price)
				.createdDate(documentContext.read("$.createdDate")).build();
	}

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	public static class OrderHistory {
		private OrderBook orderBook;
		private OrderItem orderItem;
		private Date entryDate;

		@Data
		@Builder
		@AllArgsConstructor
		@NoArgsConstructor
		public static class OrderItem {
			private String location;
			private Integer quantity;
			private BigDecimal price;
			// will have proper date format
			private String createdDate;
		}

		public static List<OrderHistory> fromJsonList(String list)
				throws JsonParseException, JsonMappingException, IOException {
			return objectMapper.readValue(list, new TypeReference<List<OrderHistory>>() {
			});
		}
	}
}

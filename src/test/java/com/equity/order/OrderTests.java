package com.equity.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Arrays;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.equity.order.OrderBookTests.OrderBookRequest;
import com.equity.order.book.OrderBook;
import com.equity.order.book.OrderBookRepository;
import com.equity.order.book.OrderBookService.Order;
import com.equity.order.execution.Execution;
import com.equity.order.execution.ExecutionRepository;
import com.equity.order.history.OrderHistoryService.OrderHistory;
import com.equity.order.item.MarketOrder;
import com.equity.order.item.MarketOrderRepository;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OrderTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private OrderBookRepository orderBookRepository;

	@Autowired
	private MarketOrderRepository marketOrderRepository;

	@Autowired
	private ExecutionRepository executionRepository;

	@Autowired
	private RestTemplate restTemplate;

	private MockMvc mockMvc;
	private MockRestServiceServer marketOrderMockService;
	private MockRestServiceServer executionMockService;

	private static String orderUrl = "http://localhost/orderBooks/{0}";
	private static String addOrderUrl = "http://localhost/orderBooks/{0}/order";
	private static String addExecutionUrl = "http://localhost/orderBooks/{0}/execution";
	private static String marketOrderUri = "http://localhost/marketOrders/{0}";
	private static String executionUri = "http://localhost/executions/{0}";

	@Before
	public void setup() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		this.marketOrderMockService = MockRestServiceServer.bindTo(restTemplate).build();

		final String instrument = "/instruments/123";

		final OrderBook orderBook = orderBookRepository
				.save(OrderBook.builder().instrument(instrument).build());

		assertEquals(orderBook.getInstrument(), instrument);
		assertEquals(orderBook.getStatus(), OrderBook.Status.OPEN);

		orderUrl = MessageFormat.format(orderUrl, String.valueOf(orderBook.getId()));
		addOrderUrl = MessageFormat.format(addOrderUrl,
				String.valueOf(orderBook.getId()));
		addExecutionUrl = MessageFormat.format(addExecutionUrl,
				String.valueOf(orderBook.getId()));

		// create market order
		final Integer quantity = 10;

		final MarketOrder marketOrder = marketOrderRepository
				.save(MarketOrder.builder().quantity(quantity).build());

		assertEquals(marketOrder.getQuantity(), quantity);

		marketOrderUri = MessageFormat.format(marketOrderUri,
				String.valueOf(marketOrder.getId()));

		marketOrderMockService.expect(requestTo(marketOrderUri)).andRespond(
				withSuccess(mockMvc.perform(get(marketOrderUri)).andDo(print())
						.andExpect(status().isOk()).andReturn().getResponse()
						.getContentAsString(), MediaType.APPLICATION_JSON_UTF8));
	}

	@Test
	public void a_addMarketOrderToOrderBook() throws Exception {
		mockMvc.perform(
				put(addOrderUrl)
						.content(Order.builder()
								.itemList(Arrays.asList(Order.Item.builder()
										.location(marketOrderUri).build()))
								.build().toJson())
						.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
				.andDo(print()).andExpect(status().isNoContent());

		marketOrderMockService.verify();

		// quick extraction for demo only
		OrderHistory orderHistory = OrderHistory.fromJsonList(mockMvc
				.perform(get("/orderHistories")).andDo(print()).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString()).get(0);

		// check added market order
		assertNotNull(orderHistory);
		assertTrue(addOrderUrl
				.contains(String.valueOf(orderHistory.getOrderBook().getId())));
		assertEquals(orderHistory.getOrderItem().getLocation(), marketOrderUri);

	}

	@Test
	public void b_closeOrderBook() throws Exception {
		final String status = "CLOSED";

		mockMvc.perform(patch(orderUrl).accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
				.content(OrderBookRequest.builder().status(status).build().toJson()))
				.andDo(print())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk()).andExpect(jsonPath("$..status").value(status))
				.andExpect(jsonPath("$..createdDate").exists());

		// throw bad request when order book is closed
		mockMvc.perform(
				put(addOrderUrl)
						.content(Order.builder()
								.itemList(Arrays.asList(Order.Item.builder()
										.location(marketOrderUri).build()))
								.build().toJson())
						.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
				.andDo(print()).andExpect(status().isBadRequest());
	}

	@Test
	public void c_addExecutions() throws Exception {

		// not sure how it works maybe it should have validations like quantity left?
		Execution execution = executionRepository.save(
				Execution.builder().price(new BigDecimal(20.00)).quantity(10).build());

		this.executionMockService = MockRestServiceServer.bindTo(restTemplate).build();

		executionUri = MessageFormat.format(executionUri,
				String.valueOf(execution.getId()));

		executionMockService.expect(requestTo(executionUri))
				.andRespond(withSuccess(mockMvc.perform(get(executionUri)).andDo(print())
						.andExpect(status().isOk()).andReturn().getResponse()
						.getContentAsString(), MediaType.APPLICATION_JSON_UTF8));

		mockMvc.perform(
				put(addExecutionUrl)
						.content(Order.builder()
								.itemList(Arrays.asList(Order.Item.builder()
										.location(executionUri).build()))
								.build().toJson())
						.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
				.andDo(print()).andExpect(status().isNoContent());

		executionMockService.verify();

		// quick extraction for demo only
		OrderHistory orderHistory = OrderHistory
				.fromJsonList(mockMvc.perform(get("/orderHistories")).andDo(print())
						.andExpect(status().isOk()).andReturn().getResponse()
						.getContentAsString())
				.stream()
				.filter(myOrderHistory -> executionUri
						.equals(myOrderHistory.getOrderItem().getLocation()))
				.findFirst().orElse(null);

		// check added execution
		assertNotNull(orderHistory);
		assertTrue(addOrderUrl
				.contains(String.valueOf(orderHistory.getOrderBook().getId())));
		assertEquals(orderHistory.getOrderItem().getLocation(), executionUri);

	}
}

package com.equity.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import lombok.Builder;
import lombok.Data;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OrderBookTests {

	public static final String LOCALHOST = "http://localhost";
	private static ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	private static String orderBook;

	@Before
	public void setup() throws Exception {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void a_openOrderBook() throws Exception {
		final String status = "OPEN";

		final String response = mockMvc
				.perform(post("/orderBooks")
						.content(OrderBookRequest.builder().instrument("/instruments/123")
								.build().toJson())
						.accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
				.andDo(print())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$..status").value(status))
				.andExpect(jsonPath("$..createdDate").exists()).andReturn().getResponse()
				.getContentAsString();

		assertEquals(JsonPath.parse(response).read("$..createdDate").toString(),
				JsonPath.parse(response).read("$..updatedDate").toString());

		orderBook = JsonPath.parse(response).read("$._links.self.href").toString()
				.replaceAll(LOCALHOST, "");

		assertNotNull(orderBook);
	}

	@Test
	public void b_closeOrderBook() throws Exception {
		final String status = "CLOSED";

		final String response = mockMvc
				.perform(patch(orderBook).accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
						.content(OrderBookRequest.builder().status(status).build()
								.toJson()))
				.andDo(print())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isOk()).andExpect(jsonPath("$..status").value(status))
				.andExpect(jsonPath("$..createdDate").exists()).andReturn().getResponse()
				.getContentAsString();

		assertNotEquals(JsonPath.parse(response).read("$..createdDate").toString(),
				JsonPath.parse(response).read("$..updatedDate").toString());
	}

	@Test
	public void c_orderBookInvalidStatus() throws Exception {
		String status = "REOPEN";

		mockMvc.perform(
				patch(orderBook).contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
						.content(OrderBookRequest.builder().status(status).build()
								.toJson()))
				.andDo(print())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
				.andExpect(status().isBadRequest());
	}

	@Data
	@Builder
	@JsonInclude(Include.NON_NULL)
	static class OrderBookRequest {
		private String instrument;
		private String status;

		public String toJson() throws JsonProcessingException {
			return objectMapper.writeValueAsString(this);
		}
	}

}

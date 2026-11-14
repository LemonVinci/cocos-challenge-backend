package com.cocos.challenge.adapter.web;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountFlowTest {

	private static final String DAY = "2023-07-14T15:00:00-03:00";

	private static final EmbeddedPostgres postgres = startPostgres();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "postgres");
	}

	@AfterAll
	static void stopPostgres() throws IOException {
		postgres.close();
	}

	@BeforeEach
	void juanStartsWithCash() {
		jdbc.update("DELETE FROM positions");
		jdbc.update("DELETE FROM orders");
		jdbc.update("DELETE FROM marketdata");
		jdbc.update("DELETE FROM account_cash");
		jdbc.update("DELETE FROM instruments");
		jdbc.update("DELETE FROM users");
		jdbc.update("""
				INSERT INTO users (id, email, accountnumber) VALUES
				(4, 'juan@test.com', '10004')
				""");
		jdbc.update("""
				INSERT INTO instruments (id, ticker, name, type) VALUES
				(47, 'PAMP', 'Pampa Energía', 'ACCIONES')
				""");
		jdbc.update("""
				INSERT INTO marketdata (instrumentid, high, low, open, close, previousclose, date) VALUES
				(47, 160.00, 140.00, 150.00, 150.00, 100.00, '2023-07-14')
				""");
		jdbc.update("""
				INSERT INTO account_cash (user_id, settled_cash, reserved_cash)
				VALUES (4, 10000.00, 0.00)
				""");
	}

	@Test
	void theHappyPathRunsFromSearchThroughBuySellAndCancel() throws Exception {
		mockMvc.perform(get("/instruments")
						.param("name", "pampa")
						.param("datetime", DAY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].ticker").value("PAMP"))
				.andExpect(jsonPath("$[0].close").value(150.0))
				.andExpect(jsonPath("$[0].dailyReturn").value(50.0));

		mockMvc.perform(get("/portfolio").param("userId", "4").param("datetime", DAY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalValue").value(10000.0))
				.andExpect(jsonPath("$.availableCash").value(10000.0))
				.andExpect(jsonPath("$.positions", hasSize(0)));

		place("BUY", "MARKET", "\"quantity\": 10", null);

		mockMvc.perform(get("/portfolio").param("userId", "4").param("datetime", DAY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.settledCash").value(8500.0))
				.andExpect(jsonPath("$.availableCash").value(8500.0))
				.andExpect(jsonPath("$.totalValue").value(10000.0))
				.andExpect(jsonPath("$.positions", hasSize(1)))
				.andExpect(jsonPath("$.positions[0].ticker").value("PAMP"))
				.andExpect(jsonPath("$.positions[0].quantity").value(10))
				.andExpect(jsonPath("$.positions[0].marketValue").value(1500.0))
				.andExpect(jsonPath("$.positions[0].totalReturn", closeTo(0.0, 0.001)))
				.andExpect(jsonPath("$.positions[0].dailyReturn").value(50.0));

		int limitId = place("BUY", "LIMIT", "\"quantity\": 20", "90");

		mockMvc.perform(get("/portfolio").param("userId", "4").param("datetime", DAY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.settledCash").value(8500.0))
				.andExpect(jsonPath("$.availableCash").value(6700.0))
				.andExpect(jsonPath("$.totalValue").value(10000.0))
				.andExpect(jsonPath("$.positions", hasSize(1)));

		place("SELL", "MARKET", "\"quantity\": 4", null);

		mockMvc.perform(get("/portfolio").param("userId", "4").param("datetime", DAY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.settledCash").value(9100.0))
				.andExpect(jsonPath("$.availableCash").value(7300.0))
				.andExpect(jsonPath("$.positions[0].quantity").value(6))
				.andExpect(jsonPath("$.positions[0].marketValue").value(900.0))
				.andExpect(jsonPath("$.totalValue").value(10000.0));

		mockMvc.perform(post("/orders/" + limitId + "/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(limitId))
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(get("/portfolio").param("userId", "4").param("datetime", DAY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.settledCash").value(9100.0))
				.andExpect(jsonPath("$.availableCash").value(9100.0))
				.andExpect(jsonPath("$.positions[0].quantity").value(6))
				.andExpect(jsonPath("$.totalValue").value(10000.0));
	}

	private int place(String side, String type, String sizeField, String price) throws Exception {
		String priceField = price == null ? "" : ", \"price\": " + price;
		String body = """
				{
				  "userId": 4,
				  "instrumentId": 47,
				  "side": "%s",
				  "type": "%s",
				  %s%s,
				  "datetime": "%s"
				}
				""".formatted(side, type, sizeField, priceField, DAY);
		MvcResult result = mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("LIMIT".equals(type) ? "NEW" : "FILLED"))
				.andReturn();
		Number id = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		return id.intValue();
	}

	private static EmbeddedPostgres startPostgres() {
		try {
			return EmbeddedPostgres.builder().start();
		} catch (IOException exception) {
			throw new IllegalStateException("No se pudo levantar Postgres para el test", exception);
		}
	}
}

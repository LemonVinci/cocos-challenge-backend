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
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderEndpointTest {

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
	void juanHasCashAndAPricedShare() {
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
				(47, 'PAMP', 'Pampa Energía', 'ACCIONES'),
				(66, 'ARS', 'Pesos', 'MONEDA')
				""");
		jdbc.update("""
				INSERT INTO marketdata (instrumentid, high, low, open, close, previousclose, date) VALUES
				(47, 160.00, 140.00, 150.00, 100.00, 90.00, '2023-07-13'),
				(47, 160.00, 140.00, 150.00, 150.00, 100.00, '2023-07-14')
				""");
		jdbc.update("""
				INSERT INTO account_cash (user_id, settled_cash, reserved_cash)
				VALUES (4, 10000.00, 0.00)
				""");
	}

	@Test
	void aMarketBuyFillsAtTheCloseAndALaterBuySeesTheUpdatedCash() throws Exception {
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"quantity\": 10", null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.price").value(150.0))
				.andExpect(jsonPath("$.userId").value(4))
				.andExpect(jsonPath("$.instrumentId").value(47));

		assertThat(cash("settled_cash")).isEqualByComparingTo("8500.00");
		assertThat(positionQuantity()).isEqualTo(10);
		assertThat(orderCount("FILLED")).isEqualTo(1);

		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"quantity\": 100", null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		assertThat(cash("settled_cash")).isEqualByComparingTo("8500.00");
		assertThat(positionQuantity()).isEqualTo(10);
		assertThat(orderCount("REJECTED")).isEqualTo(1);
	}

	@Test
	void anAmountIsTruncatedToWholeShares() throws Exception {
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"amount\": 1000", null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.size").value(6));

		assertThat(cash("settled_cash")).isEqualByComparingTo("9100.00");

		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"amount\": 100", null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"))
				.andExpect(jsonPath("$.size").value(0));

		assertThat(cash("settled_cash")).isEqualByComparingTo("9100.00");
	}

	@Test
	void theMarketDayFollowsArgentinaAndASellNeedsShares() throws Exception {
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"quantity\": 1", "2023-07-14T02:30:00Z")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andExpect(jsonPath("$.price").value(100.0));

		assertThat(cash("settled_cash")).isEqualByComparingTo("9900.00");

		jdbc.update("DELETE FROM positions WHERE user_id = 4");
		jdbc.update("""
				INSERT INTO positions (user_id, instrument_id, quantity, cost_basis, reserved_quantity)
				VALUES (4, 47, 10, 1000, 0)
				""");
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("SELL", "MARKET", "\"quantity\": 11", null)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));

		assertThat(jdbc.queryForObject(
				"SELECT quantity FROM positions WHERE user_id = 4 AND instrument_id = 47 ORDER BY quantity DESC LIMIT 1",
				Integer.class)).isEqualTo(10);
	}

	@Test
	void aLimitBuyStaysNewAndOnlyANewOrderCanBeCancelled() throws Exception {
		MvcResult placed = mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "LIMIT", "\"quantity\": 20", null, "90")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("NEW"))
				.andExpect(jsonPath("$.price").value(90.0))
				.andReturn();
		Number id = com.jayway.jsonpath.JsonPath.read(placed.getResponse().getContentAsString(), "$.id");

		assertThat(cash("settled_cash")).isEqualByComparingTo("10000.00");
		assertThat(cash("reserved_cash")).isEqualByComparingTo("1800.00");
		assertThat(positionQuantity()).isNull();

		mockMvc.perform(post("/orders/" + id.intValue() + "/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		assertThat(cash("reserved_cash")).isEqualByComparingTo("0.00");
		assertThat(orderCount("CANCELLED")).isEqualTo(1);

		mockMvc.perform(post("/orders/" + id.intValue() + "/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
		assertThat(cash("reserved_cash")).isEqualByComparingTo("0.00");

		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"quantity\": 1", null)))
				.andExpect(status().isCreated());
		Integer filledId = jdbc.queryForObject(
				"SELECT id FROM orders WHERE status = 'FILLED'",
				Integer.class);
		mockMvc.perform(post("/orders/" + filledId + "/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("FILLED"));
		assertThat(cash("settled_cash")).isEqualByComparingTo("9850.00");
	}

	@Test
	void aMissingUserOrANonShareIsNotInserted() throws Exception {
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"quantity\": 1", null).replace("\"userId\": 4", "\"userId\": 99")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"quantity\": 1", null).replace("\"instrumentId\": 47", "\"instrumentId\": 999")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("BUY", "MARKET", "\"quantity\": 1", null).replace("\"instrumentId\": 47", "\"instrumentId\": 66")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/orders/" + 999 + "/cancel"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(order("CASH_IN", "MARKET", "\"quantity\": 1", null)))
				.andExpect(status().isBadRequest());

		assertThat(orderCount(null)).isZero();
	}

	@Test
	void theSameIdempotencyKeyReturnsTheStoredOrderAndDoesNotDebitAgain() throws Exception {
		String body = order("BUY", "MARKET", "\"quantity\": 10", null);
		MvcResult created = mockMvc.perform(post("/orders")
						.header("Idempotency-Key", "buy-pamp-10")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FILLED"))
				.andReturn();
		int id = ((Number) com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id")).intValue();

		mockMvc.perform(post("/orders")
						.header("Idempotency-Key", "buy-pamp-10")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.status").value("FILLED"));

		assertThat(cash("settled_cash")).isEqualByComparingTo("8500.00");
		assertThat(orderCount("FILLED")).isEqualTo(1);

		mockMvc.perform(get("/actuator/metrics/account.lock"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.measurements[?(@.statistic=='COUNT')].value").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.greaterThan(0.0))));
		mockMvc.perform(get("/actuator/metrics/account.transaction"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.measurements[?(@.statistic=='COUNT')].value").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.greaterThan(0.0))));
	}

	private BigDecimal cash(String column) {
		return jdbc.queryForObject("SELECT " + column + " FROM account_cash WHERE user_id = 4", BigDecimal.class);
	}

	private Integer positionQuantity() {
		java.util.List<Integer> rows = jdbc.queryForList(
				"SELECT quantity FROM positions WHERE user_id = 4 AND instrument_id = 47",
				Integer.class);
		return rows.isEmpty() ? null : rows.getFirst();
	}

	private Integer orderCount(String status) {
		if (status == null) {
			return jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class);
		}
		return jdbc.queryForObject("SELECT count(*) FROM orders WHERE status = ?", Integer.class, status);
	}

	private static String order(String side, String type, String sizeField, String datetime) {
		return order(side, type, sizeField, datetime, null);
	}

	private static String order(String side, String type, String sizeField, String datetime, String price) {
		String when = datetime == null ? "2023-07-14T15:00:00-03:00" : datetime;
		String priceField = price == null ? "" : ", \"price\": " + price;
		return """
				{
				  "userId": 4,
				  "instrumentId": 47,
				  "side": "%s",
				  "type": "%s",
				  %s%s,
				  "datetime": "%s"
				}
				""".formatted(side, type, sizeField, priceField, when);
	}

	private static EmbeddedPostgres startPostgres() {
		try {
			return EmbeddedPostgres.builder().start();
		} catch (IOException exception) {
			throw new IllegalStateException("No se pudo levantar Postgres para el test", exception);
		}
	}
}

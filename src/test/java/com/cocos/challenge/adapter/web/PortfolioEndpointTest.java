package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.RebuildProjection;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioEndpointTest {

	private static final EmbeddedPostgres postgres = startPostgres();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private RebuildProjection projection;

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
	void emilianosBook() {
		jdbc.update("DELETE FROM positions");
		jdbc.update("DELETE FROM orders");
		jdbc.update("DELETE FROM marketdata");
		jdbc.update("DELETE FROM account_cash");
		jdbc.update("DELETE FROM instruments");
		jdbc.update("DELETE FROM users");
		jdbc.update("""
				INSERT INTO users (id, email, accountnumber) VALUES
				(1, 'emiliano@test.com', '10001'),
				(4, 'juan@test.com', '10004')
				""");
		jdbc.update("""
				INSERT INTO instruments (id, ticker, name, type) VALUES
				(31, 'BMA', 'Banco Macro', 'ACCIONES'),
				(45, 'LOMA', 'Loma Negra', 'ACCIONES'),
				(47, 'PAMP', 'Pampa Energía', 'ACCIONES'),
				(54, 'METR', 'Metrogas', 'ACCIONES'),
				(66, 'ARS', 'Pesos', 'MONEDA')
				""");
		jdbc.update("""
				INSERT INTO orders (id, instrumentid, userid, size, price, type, side, status, datetime) VALUES
				(1, 66, 1, 1000000, 1.00, 'MARKET', 'CASH_IN', 'FILLED', '2023-07-12 16:11:20'),
				(2, 47, 1, 50, 930.00, 'MARKET', 'BUY', 'FILLED', '2023-07-12 16:31:20'),
				(3, 47, 1, 50, 920.00, 'LIMIT', 'BUY', 'CANCELLED', '2023-07-12 18:21:20'),
				(4, 47, 1, 10, 940.00, 'MARKET', 'SELL', 'FILLED', '2023-07-12 18:51:20'),
				(5, 45, 1, 50, 710.00, 'LIMIT', 'BUY', 'NEW', '2023-07-12 19:14:20'),
				(6, 47, 1, 100, 950.00, 'MARKET', 'SELL', 'REJECTED', '2023-07-12 20:11:20'),
				(7, 31, 1, 60, 1500.00, 'LIMIT', 'BUY', 'NEW', '2023-07-13 15:13:20'),
				(8, 66, 1, 100000, 1.00, 'MARKET', 'CASH_OUT', 'FILLED', '2023-07-13 16:31:20'),
				(9, 31, 1, 20, 1540.00, 'LIMIT', 'BUY', 'FILLED', '2023-07-13 16:51:20'),
				(10, 54, 1, 500, 250.00, 'MARKET', 'BUY', 'FILLED', '2023-07-13 18:11:20'),
				(11, 31, 1, 30, 1530.00, 'MARKET', 'SELL', 'FILLED', '2023-07-13 19:13:20')
				""");
		jdbc.update("""
				INSERT INTO marketdata (instrumentid, high, low, open, close, previousclose, date) VALUES
				(31, 1541.00, 1415.00, 1425.00, 1520.25, 1413.50, '2023-07-13'),
				(31, 1560.00, 1470.00, 1525.00, 1502.80, 1520.25, '2023-07-14'),
				(47, 924.60, 896.00, 900.00, 921.80, 893.65, '2023-07-13'),
				(47, 927.70, 902.05, 920.15, 925.85, 921.80, '2023-07-14'),
				(54, 232.00, 222.50, 232.00, 232.00, 229.00, '2023-07-13'),
				(54, 233.00, 225.25, 232.00, 229.50, 232.00, '2023-07-14')
				""");
		projection.rebuildIfEmpty();
		projection.rebuildIfEmpty();
	}

	@Test
	void portfolioUsesSettledCashPlusMarketValueAndLeavesReservedCashAvailable() throws Exception {
		mockMvc.perform(get("/portfolio")
						.param("userId", "1")
						.param("datetime", "2023-07-14T15:00:00-03:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalValue").value(889756.00))
				.andExpect(jsonPath("$.settledCash").value(753000.00))
				.andExpect(jsonPath("$.availableCash").value(627500.00))
				.andExpect(jsonPath("$.positions", hasSize(3)))
				.andExpect(jsonPath("$.positions[0].ticker").value("BMA"))
				.andExpect(jsonPath("$.positions[0].quantity").value(-10))
				.andExpect(jsonPath("$.positions[0].marketValue").value(-15028.00))
				.andExpect(jsonPath("$.positions[0].totalReturn", closeTo(1.78, 0.001)))
				.andExpect(jsonPath("$.positions[0].dailyReturn", closeTo(-1.15, 0.001)))
				.andExpect(jsonPath("$.positions[1].ticker").value("PAMP"))
				.andExpect(jsonPath("$.positions[1].quantity").value(40))
				.andExpect(jsonPath("$.positions[1].marketValue").value(37034.00))
				.andExpect(jsonPath("$.positions[1].totalReturn", closeTo(-0.45, 0.001)))
				.andExpect(jsonPath("$.positions[1].dailyReturn", closeTo(0.44, 0.001)))
				.andExpect(jsonPath("$.positions[2].ticker").value("METR"))
				.andExpect(jsonPath("$.positions[2].quantity").value(500))
				.andExpect(jsonPath("$.positions[2].marketValue").value(114750.00))
				.andExpect(jsonPath("$.positions[2].totalReturn", closeTo(-8.20, 0.001)))
				.andExpect(jsonPath("$.positions[2].dailyReturn", closeTo(-1.08, 0.001)));
	}

	@Test
	void anEarlierDatetimeUsesThatDaysClose() throws Exception {
		mockMvc.perform(get("/portfolio")
						.param("userId", "1")
						.param("datetime", "2023-07-14T02:30:00Z"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.positions[1].ticker").value("PAMP"))
				.andExpect(jsonPath("$.positions[1].marketValue").value(36872.00));
	}

	@Test
	void aUserWithoutOrdersHasAnEmptyPortfolioAndAnUnknownUserIsMissing() throws Exception {
		mockMvc.perform(get("/portfolio")
						.param("userId", "4")
						.param("datetime", "2023-07-14T10:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalValue").value(0))
				.andExpect(jsonPath("$.availableCash").value(0))
				.andExpect(jsonPath("$.positions", hasSize(0)));

		mockMvc.perform(get("/portfolio")
						.param("userId", "99")
						.param("datetime", "2023-07-14T10:00:00"))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/portfolio").param("userId", "1"))
				.andExpect(status().isBadRequest());
	}

	private static EmbeddedPostgres startPostgres() {
		try {
			return EmbeddedPostgres.builder().start();
		} catch (IOException exception) {
			throw new IllegalStateException("No se pudo levantar Postgres para el test", exception);
		}
	}
}

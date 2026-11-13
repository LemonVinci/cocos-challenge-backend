package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.MarketPrice;
import com.cocos.challenge.domain.MarketDay;
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
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InstrumentSearchTest {

	private static final EmbeddedPostgres postgres = startPostgres();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private MarketPrice marketPrice;

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
	void instruments() {
		jdbc.update("DELETE FROM marketdata");
		jdbc.update("DELETE FROM instruments");
		jdbc.update("""
				INSERT INTO instruments (id, ticker, name, type) VALUES
				(1, 'GGAL', 'Grupo Financiero Galicia', 'ACCIONES'),
				(2, 'YPFD', 'YPF', 'ACCIONES'),
				(66, 'ARS', 'Pesos', 'MONEDA')
				""");
		jdbc.update("""
				INSERT INTO marketdata (instrumentid, high, low, open, close, previousclose, date) VALUES
				(1, 105.00, 95.00, 100.00, 100.00, 90.00, '2023-07-13'),
				(1, 1.00, 1.00, 1.00, 1.00, 1.00, '2023-07-14'),
				(1, 115.00, 105.00, 110.00, 110.00, 100.00, '2023-07-14'),
				(2, 55.00, 45.00, 50.00, 50.00, 0.00, '2023-07-14')
				""");
	}

	@Test
	void tickerSearchReturnsTheCloseAndDailyReturnOfTheRequestedDay() throws Exception {
		mockMvc.perform(get("/instruments")
						.param("ticker", "gg")
						.param("datetime", "2023-07-14T15:00:00-03:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].ticker").value("GGAL"))
				.andExpect(jsonPath("$[0].name").value("Grupo Financiero Galicia"))
				.andExpect(jsonPath("$[0].type").value("ACCIONES"))
				.andExpect(jsonPath("$[0].close").value(110.00))
				.andExpect(jsonPath("$[0].dailyReturn").value(10.00));
	}

	@Test
	void nameAndTickerTogetherBothHaveToMatch() throws Exception {
		mockMvc.perform(get("/instruments")
						.param("ticker", "gg")
						.param("name", "ypf")
						.param("datetime", "2023-07-14T10:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(get("/instruments")
						.param("name", "financiero")
						.param("datetime", "2023-07-14T10:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].ticker").value("GGAL"));
	}

	@Test
	void anOffsetThatIsStillThePreviousDayInArgentinaUsesThatClose() throws Exception {
		mockMvc.perform(get("/instruments")
						.param("ticker", "GGAL")
						.param("datetime", "2023-07-14T02:30:00Z"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].close").value(100.00))
				.andExpect(jsonPath("$[0].dailyReturn").value(11.11));
	}

	@Test
	void aZeroPreviousCloseAndACurrencyHaveNoDailyReturn() throws Exception {
		mockMvc.perform(get("/instruments")
						.param("ticker", "YPF")
						.param("datetime", "2023-07-14T10:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].close").value(50.00))
				.andExpect(jsonPath("$[0].dailyReturn").value(nullValue()));

		mockMvc.perform(get("/instruments")
						.param("name", "peso")
						.param("datetime", "2023-07-14T10:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].ticker").value("ARS"))
				.andExpect(jsonPath("$[0].type").value("MONEDA"))
				.andExpect(jsonPath("$[0].close").value(nullValue()))
				.andExpect(jsonPath("$[0].dailyReturn").value(nullValue()));
	}

	@Test
	void aPercentSignIsALiteralAndAMissingDatetimeIsRejected() throws Exception {
		mockMvc.perform(get("/instruments")
						.param("ticker", "%")
						.param("datetime", "2023-07-14T10:00:00"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(get("/instruments").param("ticker", "GGAL"))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/instruments")
						.param("datetime", "ayer"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void closeReadsTheSameDayUsedByTheSearch() {
		MarketDay july14 = new MarketDay(LocalDate.of(2023, 7, 14));
		MarketDay july13 = new MarketDay(LocalDate.of(2023, 7, 13));

		assertThat(marketPrice.close(1, july14)).contains(new java.math.BigDecimal("110.00"));
		assertThat(marketPrice.close(1, july13)).contains(new java.math.BigDecimal("100.00"));
		assertThat(marketPrice.close(66, july14)).isEmpty();
	}

	private static EmbeddedPostgres startPostgres() {
		try {
			return EmbeddedPostgres.builder().start();
		} catch (IOException exception) {
			throw new IllegalStateException("No se pudo levantar Postgres para el test", exception);
		}
	}
}

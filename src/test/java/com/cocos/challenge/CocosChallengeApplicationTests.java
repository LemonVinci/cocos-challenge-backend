package com.cocos.challenge;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CocosChallengeApplicationTests {

	private static final EmbeddedPostgres postgres = startPostgres();

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

	@Test
	void flywayCreatesTheExistingSchemaAndTheAccountProjection() {
		List<String> tables = jdbc.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_name IN ('users', 'instruments', 'orders', 'marketdata', 'account_cash', 'positions')
				ORDER BY table_name
				""", String.class);
		assertThat(tables).containsExactly(
				"account_cash", "instruments", "marketdata", "orders", "positions", "users");

		List<String> indexes = jdbc.queryForList("""
				SELECT indexname
				FROM pg_indexes
				WHERE schemaname = 'public'
				  AND indexname IN ('orders_account_idx', 'marketdata_latest_idx')
				ORDER BY indexname
				""", String.class);
		assertThat(indexes).containsExactly("marketdata_latest_idx", "orders_account_idx");
	}

	private static EmbeddedPostgres startPostgres() {
		try {
			return EmbeddedPostgres.builder().start();
		} catch (IOException exception) {
			throw new IllegalStateException("No se pudo levantar Postgres para el test", exception);
		}
	}

}

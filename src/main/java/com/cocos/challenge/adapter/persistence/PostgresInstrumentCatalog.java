package com.cocos.challenge.adapter.persistence;

import com.cocos.challenge.application.InstrumentCatalog;
import com.cocos.challenge.application.InstrumentMatch;
import com.cocos.challenge.application.MarketPrice;
import com.cocos.challenge.domain.Instrument;
import com.cocos.challenge.domain.InstrumentType;
import com.cocos.challenge.domain.MarketDay;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgresInstrumentCatalog implements InstrumentCatalog, MarketPrice {

	private final JdbcTemplate jdbc;

	public PostgresInstrumentCatalog(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<InstrumentMatch> find(String ticker, String name, MarketDay day) {
		return jdbc.query("""
				SELECT i.id, i.ticker, i.name, i.type, m.close, m.previousclose
				FROM instruments i
				LEFT JOIN LATERAL (
				    SELECT close, previousclose
				    FROM marketdata
				    WHERE instrumentid = i.id
				      AND date = ?
				    ORDER BY id DESC
				    LIMIT 1
				) m ON true
				WHERE (CAST(? AS text) IS NULL OR i.ticker ILIKE ? ESCAPE '\\')
				  AND (CAST(? AS text) IS NULL OR i.name ILIKE ? ESCAPE '\\')
				ORDER BY i.ticker NULLS LAST, i.id
				""",
				(rs, row) -> InstrumentMatch.of(
						new Instrument(
								rs.getInt("id"),
								rs.getString("ticker"),
								rs.getString("name"),
								InstrumentType.parse(rs.getString("type"))),
						rs.getBigDecimal("close"),
						rs.getBigDecimal("previousclose")),
				day.date(),
				likePattern(ticker),
				likePattern(ticker),
				likePattern(name),
				likePattern(name));
	}

	@Override
	public Optional<BigDecimal> close(int instrumentId, MarketDay day) {
		List<BigDecimal> closes = jdbc.query("""
				SELECT close
				FROM marketdata
				WHERE instrumentid = ?
				  AND date = ?
				  AND close IS NOT NULL
				ORDER BY id DESC
				LIMIT 1
				""",
				(rs, row) -> rs.getBigDecimal("close"),
				instrumentId,
				day.date());
		return closes.stream().findFirst();
	}

	private static String likePattern(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String escaped = value.trim()
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
		return "%" + escaped + "%";
	}
}

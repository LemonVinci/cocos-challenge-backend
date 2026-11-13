package com.cocos.challenge.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioTest {

	private static final int BMA = 31;
	private static final int PAMP = 47;
	private static final int METR = 54;

	@Test
	void valuesALongAtTheCloseAndKeepsTheDailyReturnApart() {
		Account account = Account.of(1, new AccountCash(new BigDecimal("10000.00"), new BigDecimal("500.00")))
				.withPosition(Position.openLong(PAMP, 4, new BigDecimal("100"))
						.buyFilled(6, new BigDecimal("150"))
						.orElseThrow());

		Portfolio portfolio = Portfolio.value(account, Map.of(
				PAMP, new Quote(new BigDecimal("160.00"), new BigDecimal("100.00"))));

		assertThat(portfolio.totalValue()).contains(new BigDecimal("11600.00"));
		assertThat(portfolio.holdings()).singleElement().satisfies(holding -> {
			assertThat(holding.instrumentId()).isEqualTo(PAMP);
			assertThat(holding.quantity()).isEqualTo(10);
			assertThat(holding.marketValue()).contains(new BigDecimal("1600.00"));
			assertThat(holding.totalReturnPercent()).contains(new BigDecimal("23.08"));
			assertThat(holding.dailyReturnPercent()).contains(new BigDecimal("60.00"));
		});
	}

	@Test
	void valuesEmilianosBookOnTheFourteenth() {
		Account account = Account.of(1, new AccountCash(new BigDecimal("753000.00"), new BigDecimal("125500.00")))
				.withPosition(new Position(PAMP, 40, new BigDecimal("37200"), 0))
				.withPosition(new Position(METR, 500, new BigDecimal("125000"), 0))
				.withPosition(new Position(BMA, -10, new BigDecimal("15300"), 0));

		Portfolio portfolio = Portfolio.value(account, Map.of(
				PAMP, new Quote(new BigDecimal("925.85"), new BigDecimal("948.50")),
				METR, new Quote(new BigDecimal("229.50"), new BigDecimal("220.00")),
				BMA, new Quote(new BigDecimal("1502.80"), new BigDecimal("1520.25"))));

		assertThat(portfolio.totalValue()).contains(new BigDecimal("889756.00"));
		assertThat(portfolio.holdings()).extracting(Portfolio.Holding::instrumentId)
				.containsExactly(BMA, PAMP, METR);
		assertThat(holding(portfolio, BMA).marketValue()).contains(new BigDecimal("-15028.00"));
		assertThat(holding(portfolio, BMA).totalReturnPercent()).contains(new BigDecimal("1.78"));
		assertThat(holding(portfolio, BMA).dailyReturnPercent()).contains(new BigDecimal("-1.15"));
		assertThat(holding(portfolio, PAMP).marketValue()).contains(new BigDecimal("37034.00"));
		assertThat(holding(portfolio, PAMP).totalReturnPercent()).contains(new BigDecimal("-0.45"));
		assertThat(holding(portfolio, METR).marketValue()).contains(new BigDecimal("114750.00"));
		assertThat(holding(portfolio, METR).totalReturnPercent()).contains(new BigDecimal("-8.20"));
	}

	@Test
	void aPositionWithoutACloseLeavesTheTotalEmpty() {
		Account account = Account.of(1, AccountCash.empty())
				.withPosition(Position.openLong(PAMP, 10, new BigDecimal("100")));

		Portfolio portfolio = Portfolio.value(account, Map.of());

		assertThat(portfolio.totalValue()).isEmpty();
		assertThat(portfolio.holdings()).singleElement().satisfies(holding -> {
			assertThat(holding.marketValue()).isEmpty();
			assertThat(holding.totalReturnPercent()).isEmpty();
			assertThat(holding.dailyReturnPercent()).isEmpty();
		});
	}

	@Test
	void aZeroCostHasMarketValueAndNoReturn() {
		Account account = Account.of(1, new AccountCash(new BigDecimal("1000.00"), BigDecimal.ZERO))
				.withPosition(new Position(PAMP, 5, BigDecimal.ZERO, 0));

		Portfolio portfolio = Portfolio.value(account, Map.of(
				PAMP, new Quote(new BigDecimal("10.00"), BigDecimal.ZERO)));

		assertThat(portfolio.totalValue()).contains(new BigDecimal("1050.00"));
		assertThat(portfolio.holdings()).singleElement().satisfies(holding -> {
			assertThat(holding.marketValue()).contains(new BigDecimal("50.00"));
			assertThat(holding.totalReturnPercent()).isEmpty();
			assertThat(holding.dailyReturnPercent()).isEmpty();
		});
	}

	private static Portfolio.Holding holding(Portfolio portfolio, int instrumentId) {
		return portfolio.holdings().stream()
				.filter(holding -> holding.instrumentId() == instrumentId)
				.findFirst()
				.orElseThrow();
	}
}

package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.Portfolio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record AccountStatement(
		int userId,
		BigDecimal settledCash,
		BigDecimal availableCash,
		Optional<BigDecimal> totalValue,
		List<Line> lines) {

	public AccountStatement {
		Objects.requireNonNull(settledCash, "settledCash");
		Objects.requireNonNull(availableCash, "availableCash");
		Objects.requireNonNull(totalValue, "totalValue");
		lines = List.copyOf(lines);
	}

	public record Line(
			int instrumentId,
			String ticker,
			int quantity,
			Optional<BigDecimal> marketValue,
			Optional<BigDecimal> totalReturnPercent,
			Optional<BigDecimal> dailyReturnPercent) {

		public Line {
			Objects.requireNonNull(marketValue, "marketValue");
			Objects.requireNonNull(totalReturnPercent, "totalReturnPercent");
			Objects.requireNonNull(dailyReturnPercent, "dailyReturnPercent");
		}
	}

	public static AccountStatement of(Account account, Portfolio portfolio, Map<Integer, String> tickers) {
		List<Line> lines = portfolio.holdings().stream()
				.map(holding -> new Line(
						holding.instrumentId(),
						tickers.get(holding.instrumentId()),
						holding.quantity(),
						holding.marketValue(),
						holding.totalReturnPercent(),
						holding.dailyReturnPercent()))
				.toList();
		return new AccountStatement(
				account.userId(),
				account.cash().settledCash(),
				account.cash().available(),
				portfolio.totalValue(),
				lines);
	}
}

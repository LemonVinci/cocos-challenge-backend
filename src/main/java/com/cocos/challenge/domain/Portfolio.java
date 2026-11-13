package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;

public record Portfolio(Optional<BigDecimal> totalValue, List<Holding> holdings) {

	public Portfolio {
		Objects.requireNonNull(totalValue, "totalValue");
		holdings = List.copyOf(holdings);
	}

	public record Holding(
			int instrumentId,
			int quantity,
			Optional<BigDecimal> marketValue,
			Optional<BigDecimal> totalReturnPercent,
			Optional<BigDecimal> dailyReturnPercent) {

		public Holding {
			Objects.requireNonNull(marketValue, "marketValue");
			Objects.requireNonNull(totalReturnPercent, "totalReturnPercent");
			Objects.requireNonNull(dailyReturnPercent, "dailyReturnPercent");
		}
	}

	public static Portfolio value(Account account, Map<Integer, Quote> quotes) {
		Objects.requireNonNull(account, "account");
		Objects.requireNonNull(quotes, "quotes");
		List<Holding> holdings = account.positions().values().stream()
				.sorted(Comparator.comparingInt(Position::instrumentId))
				.map(position -> holding(position, quotes.get(position.instrumentId())))
				.toList();
		return new Portfolio(totalValue(account.cash().settledCash(), holdings), holdings);
	}

	private static Holding holding(Position position, Quote quote) {
		Optional<BigDecimal> dailyReturn = isNull(quote) ? Optional.empty() : quote.dailyReturnPercent();
		if (position.quantity() == 0) {
			return new Holding(
					position.instrumentId(),
					0,
					Optional.of(money(BigDecimal.ZERO)),
					Optional.empty(),
					dailyReturn);
		}
		if (isNull(quote) || isNull(quote.close())) {
			return new Holding(
					position.instrumentId(),
					position.quantity(),
					Optional.empty(),
					Optional.empty(),
					dailyReturn);
		}
		BigDecimal marketValue = money(quote.close().multiply(BigDecimal.valueOf(position.quantity())));
		return new Holding(
				position.instrumentId(),
				position.quantity(),
				Optional.of(marketValue),
				totalReturnPercent(position, marketValue),
				dailyReturn);
	}

	private static Optional<BigDecimal> totalReturnPercent(Position position, BigDecimal marketValue) {
		if (position.costBasis().signum() == 0) {
			return Optional.empty();
		}
		BigDecimal profit = position.quantity() > 0
				? marketValue.subtract(position.costBasis())
				: position.costBasis().add(marketValue);
		BigDecimal ratio = profit.divide(position.costBasis(), 8, RoundingMode.HALF_UP);
		return Optional.of(ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
	}

	private static Optional<BigDecimal> totalValue(BigDecimal settledCash, List<Holding> holdings) {
		BigDecimal total = settledCash;
		for (Holding holding : holdings) {
			if (holding.marketValue().isEmpty()) {
				return Optional.empty();
			}
			total = total.add(holding.marketValue().orElseThrow());
		}
		return Optional.of(money(total));
	}

	private static BigDecimal money(BigDecimal amount) {
		return amount.setScale(2, RoundingMode.HALF_UP);
	}
}

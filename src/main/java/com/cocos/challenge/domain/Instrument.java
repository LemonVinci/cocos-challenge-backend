package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record Instrument(int id, String ticker, String name, InstrumentType type) {

	public Instrument {
		Objects.requireNonNull(type, "type");
	}

	public boolean isTradable() {
		return type.isTradable();
	}

	public Optional<BigDecimal> dailyReturnPercent(Quote quote) {
		if (quote == null) {
			return Optional.empty();
		}
		return quote.dailyReturnPercent();
	}
}

package com.cocos.challenge.application;

import com.cocos.challenge.domain.Instrument;
import com.cocos.challenge.domain.Quote;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.isNull;

public record InstrumentMatch(Instrument instrument, Optional<Quote> quote) {

	public InstrumentMatch {
		Objects.requireNonNull(instrument, "instrument");
		Objects.requireNonNull(quote, "quote");
	}

	public static InstrumentMatch of(Instrument instrument, BigDecimal close, BigDecimal previousClose) {
		if (isNull(close) && isNull(previousClose)) {
			return new InstrumentMatch(instrument, Optional.empty());
		}
		return new InstrumentMatch(instrument, Optional.of(new Quote(close, previousClose)));
	}

	public Optional<BigDecimal> close() {
		return quote.map(Quote::close);
	}

	public Optional<BigDecimal> dailyReturnPercent() {
		return quote.flatMap(Quote::dailyReturnPercent);
	}
}

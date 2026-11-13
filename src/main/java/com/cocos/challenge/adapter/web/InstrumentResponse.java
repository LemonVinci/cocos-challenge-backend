package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.InstrumentMatch;
import com.cocos.challenge.domain.Instrument;

import java.math.BigDecimal;

public record InstrumentResponse(
		int id,
		String ticker,
		String name,
		String type,
		BigDecimal close,
		BigDecimal dailyReturn) {

	public static InstrumentResponse from(InstrumentMatch match) {
		Instrument instrument = match.instrument();
		return new InstrumentResponse(
				instrument.id(),
				instrument.ticker(),
				instrument.name(),
				instrument.type().name(),
				match.close().orElse(null),
				match.dailyReturnPercent().orElse(null));
	}
}

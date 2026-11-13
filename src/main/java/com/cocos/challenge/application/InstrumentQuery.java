package com.cocos.challenge.application;

import com.cocos.challenge.domain.MarketDay;

import java.util.Objects;

public record InstrumentQuery(String ticker, String name, MarketDay day) {

	public InstrumentQuery {
		Objects.requireNonNull(day, "day");
	}
}

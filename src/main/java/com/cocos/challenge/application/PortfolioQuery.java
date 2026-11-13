package com.cocos.challenge.application;

import com.cocos.challenge.domain.MarketDay;

import java.util.Objects;

public record PortfolioQuery(int userId, MarketDay day) {

	public PortfolioQuery {
		Objects.requireNonNull(day, "day");
	}
}

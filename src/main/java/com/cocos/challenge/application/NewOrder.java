package com.cocos.challenge.application;

import com.cocos.challenge.domain.MarketDay;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record NewOrder(
		int userId,
		int instrumentId,
		Side side,
		OrderType type,
		Integer quantity,
		BigDecimal amount,
		BigDecimal limitPrice,
		MarketDay day,
		LocalDateTime datetime,
		String idempotencyKey) {

	public NewOrder {
		Objects.requireNonNull(side, "side");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(day, "day");
		Objects.requireNonNull(datetime, "datetime");
	}
}

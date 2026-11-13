package com.cocos.challenge.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record OpenOrder(
		int instrumentId,
		Side side,
		OrderType type,
		OrderStatus status,
		int size,
		BigDecimal price) {

	public OpenOrder {
		Objects.requireNonNull(side, "side");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(price, "price");
	}

	public BigDecimal notional() {
		return price.multiply(BigDecimal.valueOf(size));
	}
}

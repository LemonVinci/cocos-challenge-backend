package com.cocos.challenge.application;

import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;

import java.math.BigDecimal;
import java.util.Objects;

public record BookOrder(
		int userId,
		int instrumentId,
		BookSide side,
		OrderType type,
		OrderStatus status,
		int size,
		BigDecimal price) {

	public BookOrder {
		Objects.requireNonNull(side, "side");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(price, "price");
	}

	public BigDecimal notional() {
		return price.multiply(BigDecimal.valueOf(size));
	}
}

package com.cocos.challenge.application;

import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record OrderDraft(
		int userId,
		int instrumentId,
		BookSide side,
		OrderType type,
		OrderStatus status,
		int size,
		BigDecimal price,
		LocalDateTime datetime) {

	public OrderDraft {
		Objects.requireNonNull(side, "side");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(datetime, "datetime");
	}
}

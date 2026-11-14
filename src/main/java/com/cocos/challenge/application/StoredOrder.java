package com.cocos.challenge.application;

import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.OpenOrder;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public record StoredOrder(
		int id,
		int userId,
		int instrumentId,
		BookSide side,
		OrderType type,
		OrderStatus status,
		int size,
		BigDecimal price,
		LocalDateTime datetime) {

	public Optional<OpenOrder> asTrade() {
		if (side.isCash()) {
			return Optional.empty();
		}
		BigDecimal orderPrice = price == null ? BigDecimal.ZERO : price;
		return Optional.of(new OpenOrder(instrumentId, Side.valueOf(side.name()), type, status, size, orderPrice));
	}

	public OrderReceipt receipt() {
		Side tradeSide = side.isCash() ? null : Side.valueOf(side.name());
		return new OrderReceipt(id, userId, instrumentId, tradeSide, type, status, size, price, datetime);
	}
}

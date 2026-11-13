package com.cocos.challenge.application;

import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderReceipt(
		int id,
		int userId,
		int instrumentId,
		Side side,
		OrderType type,
		OrderStatus status,
		int size,
		BigDecimal price,
		LocalDateTime datetime) {
}

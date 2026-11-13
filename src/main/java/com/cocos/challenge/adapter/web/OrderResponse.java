package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.OrderReceipt;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
		int id,
		int userId,
		int instrumentId,
		String side,
		String type,
		String status,
		int size,
		BigDecimal price,
		LocalDateTime datetime) {

	public static OrderResponse from(OrderReceipt receipt) {
		return new OrderResponse(
				receipt.id(),
				receipt.userId(),
				receipt.instrumentId(),
				receipt.side() == null ? null : receipt.side().name(),
				receipt.type().name(),
				receipt.status().name(),
				receipt.size(),
				receipt.price(),
				receipt.datetime());
	}
}

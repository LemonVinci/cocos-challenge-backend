package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.NewOrder;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Side;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import static java.util.Objects.isNull;

record OrderBody(
		Integer userId,
		Integer instrumentId,
		String side,
		String type,
		Integer quantity,
		BigDecimal amount,
		BigDecimal price,
		String datetime) {

	NewOrder toCommand() {
		if (isNull(userId) || isNull(instrumentId) || isNull(side) || isNull(type)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId, instrumentId, side and type are required");
		}
		Side parsedSide;
		try {
			parsedSide = Side.valueOf(side.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "side must be BUY or SELL");
		}
		OrderType parsedType;
		try {
			parsedType = OrderType.valueOf(type.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type must be MARKET or LIMIT");
		}
		LocalDateTime timestamp = RequestMarketDay.localTimestamp(datetime);
		return new NewOrder(
				userId,
				instrumentId,
				parsedSide,
				parsedType,
				quantity,
				amount,
				price,
				RequestMarketDay.parse(datetime),
				timestamp);
	}
}

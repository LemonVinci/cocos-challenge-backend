package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.Instrument;
import com.cocos.challenge.domain.MarketDay;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Side;

import java.math.BigDecimal;

public record PlaceOrderCommand(
		Instrument instrument,
		Side side,
		OrderType type,
		Integer quantity,
		BigDecimal amount,
		BigDecimal limitPrice,
		MarketDay day,
		Account account) {
}

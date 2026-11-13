package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.OrderStatus;

import java.math.BigDecimal;

public record PlacedOrder(OrderStatus status, int size, BigDecimal price, Account account) {

	public static PlacedOrder rejected(int size, BigDecimal price, Account account) {
		return new PlacedOrder(OrderStatus.REJECTED, size, price, account);
	}
}

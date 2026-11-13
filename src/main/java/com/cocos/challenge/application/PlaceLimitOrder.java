package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Position;
import com.cocos.challenge.domain.ShareCount;
import com.cocos.challenge.domain.Side;

import java.math.BigDecimal;
import java.util.Optional;

import static java.util.Objects.isNull;

public final class PlaceLimitOrder implements PlaceOrder {

	@Override
	public boolean supports(OrderType type) {
		return OrderType.LIMIT.equals(type);
	}

	@Override
	public PlacedOrder execute(PlaceOrderCommand command) {
		BigDecimal price = command.limitPrice();
		if (isNull(price) || price.signum() <= 0) {
			return PlacedOrder.rejected(0, price, command.account());
		}
		int size = ShareCount.from(command.quantity(), command.amount(), price);
		if (size < 1) {
			return PlacedOrder.rejected(size, price, command.account());
		}
		if (Side.BUY.equals(command.side())) {
			return buy(command, price, size);
		}
		return sell(command, price, size);
	}

	private PlacedOrder buy(PlaceOrderCommand command, BigDecimal price, int size) {
		Account account = command.account();
		BigDecimal notional = price.multiply(BigDecimal.valueOf(size));
		Optional<AccountCash> reserved = account.cash().reserve(notional);
		if (reserved.isEmpty()) {
			return PlacedOrder.rejected(size, price, account);
		}
		return new PlacedOrder(OrderStatus.NEW, size, price, account.withCash(reserved.get()));
	}

	private PlacedOrder sell(PlaceOrderCommand command, BigDecimal price, int size) {
		Account account = command.account();
		int instrumentId = command.instrument().id();
		if (account.availableShares(instrumentId) < size) {
			return PlacedOrder.rejected(size, price, account);
		}
		Position reserved = account.position(instrumentId).orElseThrow().reserve(size);
		return new PlacedOrder(OrderStatus.NEW, size, price, account.withPosition(reserved));
	}
}

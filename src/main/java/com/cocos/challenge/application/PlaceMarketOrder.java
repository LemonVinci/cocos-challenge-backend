package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Position;
import com.cocos.challenge.domain.ShareCount;
import com.cocos.challenge.domain.Side;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public final class PlaceMarketOrder implements PlaceOrder {

	private final MarketPrice prices;

	public PlaceMarketOrder(MarketPrice prices) {
		this.prices = prices;
	}

	@Override
	public boolean supports(OrderType type) {
		return OrderType.MARKET.equals(type);
	}

	@Override
	public PlacedOrder execute(PlaceOrderCommand command) {
		Optional<BigDecimal> close = prices.close(command.instrument().id(), command.day());
		if (close.isEmpty()) {
			return PlacedOrder.rejected(0, null, command.account());
		}
		BigDecimal price = close.get();
		int size = ShareCount.from(command.quantity(), command.amount(), price);
		if (size < 1) {
			return PlacedOrder.rejected(size, price, command.account());
		}
		BigDecimal notional = price.multiply(BigDecimal.valueOf(size));
		if (Side.BUY.equals(command.side())) {
			return buy(command, price, size, notional);
		}
		return sell(command, price, size, notional);
	}

	private PlacedOrder buy(PlaceOrderCommand command, BigDecimal price, int size, BigDecimal notional) {
		Account account = command.account();
		Optional<AccountCash> paid = account.cash().withdraw(notional);
		if (paid.isEmpty()) {
			return PlacedOrder.rejected(size, price, account);
		}
		int instrumentId = command.instrument().id();
		Position position = account.position(instrumentId)
				.map(current -> current.buyFilled(size, price).orElseThrow())
				.orElseGet(() -> Position.openLong(instrumentId, size, price));
		return new PlacedOrder(OrderStatus.FILLED, size, price, account.withCash(paid.get()).withPosition(position));
	}

	private PlacedOrder sell(PlaceOrderCommand command, BigDecimal price, int size, BigDecimal notional) {
		Account account = command.account();
		int instrumentId = command.instrument().id();
		if (account.availableShares(instrumentId) < size) {
			return PlacedOrder.rejected(size, price, account);
		}
		Position current = account.position(instrumentId).orElseThrow();
		Optional<Position> sold = current.sellFilled(size, price);
		Account credited = account.withCash(account.cash().deposit(notional));
		Account updated = sold
				.map(credited::withPosition)
				.orElseGet(() -> credited.withoutPosition(instrumentId));
		return new PlacedOrder(OrderStatus.FILLED, size, price, updated);
	}
}

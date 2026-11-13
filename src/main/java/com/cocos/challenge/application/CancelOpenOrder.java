package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.OpenOrder;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.Position;
import com.cocos.challenge.domain.Side;

public final class CancelOpenOrder implements CancelOrder {

	@Override
	public CancelOutcome execute(CancelOrderCommand command) {
		OpenOrder order = command.order();
		if (!OrderStatus.NEW.equals(order.status())) {
			return new CancelOutcome(order.status(), command.account());
		}
		Account account = command.account();
		if (Side.BUY.equals(order.side())) {
			account = account.withCash(account.cash().release(order.notional()));
		} else {
			Position position = account.position(order.instrumentId()).orElseThrow();
			account = account.withPosition(position.release(order.size()));
		}
		return new CancelOutcome(OrderStatus.CANCELLED, account);
	}
}

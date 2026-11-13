package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Position;

import java.math.BigDecimal;
import java.util.Optional;

public final class ApplyBookOrder {

	public Account apply(Account account, BookOrder order) {
		if (OrderStatus.REJECTED.equals(order.status()) || OrderStatus.CANCELLED.equals(order.status())) {
			return account;
		}
		if (order.side().isCash()) {
			return cash(account, order);
		}
		if (OrderStatus.FILLED.equals(order.status())) {
			return filled(account, order);
		}
		if (OrderStatus.NEW.equals(order.status()) && OrderType.LIMIT.equals(order.type())) {
			return openLimit(account, order);
		}
		throw new IllegalStateException("Unsupported recorded order: " + order.side() + " " + order.type() + " " + order.status());
	}

	private Account cash(Account account, BookOrder order) {
		if (!OrderStatus.FILLED.equals(order.status())) {
			throw new IllegalStateException("Cash movement is not filled");
		}
		if (BookSide.CASH_IN.equals(order.side())) {
			return account.withCash(account.cash().deposit(order.notional()));
		}
		AccountCash withdrawn = account.cash().withdraw(order.notional())
				.orElseThrow(() -> new IllegalStateException("FILLED cash-out exceeds available cash"));
		return account.withCash(withdrawn);
	}

	private Account filled(Account account, BookOrder order) {
		if (BookSide.BUY.equals(order.side())) {
			return buyFilled(account, order);
		}
		return sellFilled(account, order);
	}

	private Account buyFilled(Account account, BookOrder order) {
		AccountCash paid = account.cash().withdraw(order.notional())
				.orElseThrow(() -> new IllegalStateException("FILLED buy exceeds available cash"));
		Optional<Position> updated = account.position(order.instrumentId())
				.map(position -> position.buyFilled(order.size(), order.price()))
				.orElseGet(() -> Optional.of(Position.openLong(order.instrumentId(), order.size(), order.price())));
		Account paidAccount = account.withCash(paid);
		return updated
				.map(paidAccount::withPosition)
				.orElseGet(() -> paidAccount.withoutPosition(order.instrumentId()));
	}

	private Account sellFilled(Account account, BookOrder order) {
		Position current = account.position(order.instrumentId())
				.orElse(new Position(order.instrumentId(), 0, BigDecimal.ZERO, 0));
		Optional<Position> sold = current.sellFilled(order.size(), order.price());
		Account credited = account.withCash(account.cash().deposit(order.notional()));
		return sold
				.map(credited::withPosition)
				.orElseGet(() -> credited.withoutPosition(order.instrumentId()));
	}

	private Account openLimit(Account account, BookOrder order) {
		if (BookSide.BUY.equals(order.side())) {
			AccountCash reserved = account.cash().reserve(order.notional())
					.orElseThrow(() -> new IllegalStateException("NEW buy exceeds available cash"));
			return account.withCash(reserved);
		}
		Position position = account.position(order.instrumentId())
				.orElseThrow(() -> new IllegalStateException("NEW sell has no position"));
		if (position.availableQuantity() < order.size()) {
			throw new IllegalStateException("NEW sell exceeds available shares");
		}
		return account.withPosition(position.reserve(order.size()));
	}
}

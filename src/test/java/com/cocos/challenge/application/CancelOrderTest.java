package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.OpenOrder;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Position;
import com.cocos.challenge.domain.Side;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CancelOrderTest {

	private final CancelOrder cancelOrder = new CancelOpenOrder();

	@Test
	void onlyANewOrderCanBeCancelled() {
		Account reserved = Account.of(1, AccountCash.empty().deposit(new BigDecimal("10000")).reserve(new BigDecimal("1800")).orElseThrow());
		OpenOrder limit = new OpenOrder(47, Side.BUY, OrderType.LIMIT, OrderStatus.NEW, 20, new BigDecimal("90"));

		CancelOutcome cancelled = cancelOrder.execute(new CancelOrderCommand(limit, reserved));

		assertThat(cancelled.cancelled()).isTrue();
		assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(cancelled.account().cash().reservedCash()).isEqualByComparingTo("0");
		assertThat(cancelled.account().cash().available()).isEqualByComparingTo("10000");
	}

	@Test
	void aFilledOrderStaysFilledAndTheAccountDoesNotChange() {
		Account filled = Account.of(1, AccountCash.empty().deposit(new BigDecimal("8500")))
				.withPosition(Position.openLong(47, 10, new BigDecimal("150")));
		OpenOrder market = new OpenOrder(47, Side.BUY, OrderType.MARKET, OrderStatus.FILLED, 10, new BigDecimal("150"));

		CancelOutcome outcome = cancelOrder.execute(new CancelOrderCommand(market, filled));

		assertThat(outcome.cancelled()).isFalse();
		assertThat(outcome.status()).isEqualTo(OrderStatus.FILLED);
		assertThat(outcome.account()).isEqualTo(filled);
	}

	@Test
	void cancellingANewSellReleasesTheReservedShares() {
		Account reserved = Account.of(1, AccountCash.empty().deposit(new BigDecimal("10000")))
				.withPosition(Position.openLong(47, 10, new BigDecimal("100")).reserve(4));
		OpenOrder limit = new OpenOrder(47, Side.SELL, OrderType.LIMIT, OrderStatus.NEW, 4, new BigDecimal("110"));

		CancelOutcome cancelled = cancelOrder.execute(new CancelOrderCommand(limit, reserved));

		assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(cancelled.account().availableShares(47)).isEqualTo(10);
		assertThat(cancelled.account().position(47).orElseThrow().quantity()).isEqualTo(10);
	}
}

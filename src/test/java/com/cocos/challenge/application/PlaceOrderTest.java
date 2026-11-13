package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.Instrument;
import com.cocos.challenge.domain.InstrumentType;
import com.cocos.challenge.domain.MarketDay;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Position;
import com.cocos.challenge.domain.Side;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderTest {

	private static final Instrument PAMP = new Instrument(47, "PAMP", "Pampa Holding S.A.", InstrumentType.ACCIONES);
	private static final MarketDay JULY_14 = MarketDay.from(LocalDateTime.of(2023, 7, 14, 15, 0));

	private final SubmitOrder submitOrder = new SubmitOrder(List.of(
			new PlaceMarketOrder((instrumentId, day) -> Optional.of(new BigDecimal("150"))),
			new PlaceLimitOrder()));

	@Test
	void aMarketBuyFillsAtTheCloseAndUpdatesCashAndPosition() {
		PlacedOrder placed = submitOrder.execute(buy(OrderType.MARKET, 10, null, null, funded(new BigDecimal("10000"))));

		assertThat(placed.status()).isEqualTo(OrderStatus.FILLED);
		assertThat(placed.size()).isEqualTo(10);
		assertThat(placed.price()).isEqualByComparingTo("150");
		assertThat(placed.account().cash().settledCash()).isEqualByComparingTo("8500");
		assertThat(placed.account().cash().available()).isEqualByComparingTo("8500");
		Position position = placed.account().position(PAMP.id()).orElseThrow();
		assertThat(position.quantity()).isEqualTo(10);
		assertThat(position.costBasis()).isEqualByComparingTo("1500");
	}

	@Test
	void aLimitBuyStaysNewAndReservesTheNotional() {
		PlacedOrder placed = submitOrder.execute(buy(OrderType.LIMIT, 20, null, new BigDecimal("90"), funded(new BigDecimal("10000"))));

		assertThat(placed.status()).isEqualTo(OrderStatus.NEW);
		assertThat(placed.account().cash().settledCash()).isEqualByComparingTo("10000");
		assertThat(placed.account().cash().reservedCash()).isEqualByComparingTo("1800");
		assertThat(placed.account().cash().available()).isEqualByComparingTo("8200");
		assertThat(placed.account().position(PAMP.id())).isEmpty();
	}

	@Test
	void aBuyLargerThanAvailableCashIsRejected() {
		Account account = funded(new BigDecimal("1000"));

		PlacedOrder placed = submitOrder.execute(buy(OrderType.MARKET, 20, null, null, account));

		assertThat(placed.status()).isEqualTo(OrderStatus.REJECTED);
		assertThat(placed.account()).isEqualTo(account);
	}

	@Test
	void aSellLargerThanAvailableSharesIsRejected() {
		Account account = funded(new BigDecimal("10000")).withPosition(Position.openLong(PAMP.id(), 10, new BigDecimal("100")));

		PlacedOrder placed = submitOrder.execute(sell(11, account));

		assertThat(placed.status()).isEqualTo(OrderStatus.REJECTED);
		assertThat(placed.account().availableShares(PAMP.id())).isEqualTo(10);
	}

	@Test
	void anAmountIsTruncatedToWholeShares() {
		PlacedOrder placed = submitOrder.execute(buy(OrderType.MARKET, null, new BigDecimal("1000"), null, funded(new BigDecimal("10000"))));

		assertThat(placed.status()).isEqualTo(OrderStatus.FILLED);
		assertThat(placed.size()).isEqualTo(6);
		assertThat(placed.account().cash().settledCash()).isEqualByComparingTo("9100");
		assertThat(placed.account().position(PAMP.id()).orElseThrow().quantity()).isEqualTo(6);
	}

	@Test
	void anAmountThatBuysNoSharesIsRejected() {
		Account account = funded(new BigDecimal("10000"));

		PlacedOrder placed = submitOrder.execute(buy(OrderType.MARKET, null, new BigDecimal("100"), null, account));

		assertThat(placed.status()).isEqualTo(OrderStatus.REJECTED);
		assertThat(placed.size()).isZero();
		assertThat(placed.account()).isEqualTo(account);
	}

	private PlaceOrderCommand buy(OrderType type, Integer quantity, BigDecimal amount, BigDecimal limitPrice, Account account) {
		return new PlaceOrderCommand(PAMP, Side.BUY, type, quantity, amount, limitPrice, JULY_14, account);
	}

	private PlaceOrderCommand sell(int quantity, Account account) {
		return new PlaceOrderCommand(PAMP, Side.SELL, OrderType.MARKET, quantity, null, null, JULY_14, account);
	}

	private static Account funded(BigDecimal amount) {
		return Account.of(1, AccountCash.empty().deposit(amount));
	}
}

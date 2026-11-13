package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.BookSide;
import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Position;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyBookOrderTest {

	private static final int USER = 1;
	private static final int BMA = 31;
	private static final int LOMA = 45;
	private static final int PAMP = 47;
	private static final int METR = 54;
	private static final int ARS = 66;

	private final ApplyBookOrder apply = new ApplyBookOrder();

	@Test
	void replaysEmilianosBookIncludingTheShort() {
		Account account = Account.of(USER, AccountCash.empty());
		for (BookOrder order : emiliano()) {
			account = apply.apply(account, order);
		}

		assertThat(account.cash().settledCash()).isEqualByComparingTo("753000");
		assertThat(account.cash().reservedCash()).isEqualByComparingTo("125500");
		assertThat(account.cash().available()).isEqualByComparingTo("627500");
		assertThat(account.positions()).hasSize(3);
		assertPosition(account, PAMP, 40, "37200");
		assertPosition(account, METR, 500, "125000");
		assertPosition(account, BMA, -10, "15300");
		assertThat(account.position(LOMA)).isEmpty();
	}

	@Test
	void rejectedAndCancelledOrdersLeaveTheAccountUntouched() {
		Account account = Account.of(USER, new AccountCash(new BigDecimal("1000"), BigDecimal.ZERO))
				.withPosition(Position.openLong(PAMP, 10, new BigDecimal("100")));

		Account afterRejectedSell = apply.apply(account, order(PAMP, BookSide.SELL, OrderType.MARKET, OrderStatus.REJECTED, 100, "950"));
		Account afterCancelledBuy = apply.apply(account, order(PAMP, BookSide.BUY, OrderType.LIMIT, OrderStatus.CANCELLED, 50, "920"));

		assertThat(afterRejectedSell).isEqualTo(account);
		assertThat(afterCancelledBuy).isEqualTo(account);
	}

	private static void assertPosition(Account account, int instrumentId, int quantity, String cost) {
		Position position = account.position(instrumentId).orElseThrow();
		assertThat(position.quantity()).isEqualTo(quantity);
		assertThat(position.costBasis()).isEqualByComparingTo(cost);
		assertThat(position.reservedQuantity()).isZero();
	}

	private static List<BookOrder> emiliano() {
		return List.of(
				order(ARS, BookSide.CASH_IN, OrderType.MARKET, OrderStatus.FILLED, 1_000_000, "1"),
				order(PAMP, BookSide.BUY, OrderType.MARKET, OrderStatus.FILLED, 50, "930"),
				order(PAMP, BookSide.BUY, OrderType.LIMIT, OrderStatus.CANCELLED, 50, "920"),
				order(PAMP, BookSide.SELL, OrderType.MARKET, OrderStatus.FILLED, 10, "940"),
				order(LOMA, BookSide.BUY, OrderType.LIMIT, OrderStatus.NEW, 50, "710"),
				order(PAMP, BookSide.SELL, OrderType.MARKET, OrderStatus.REJECTED, 100, "950"),
				order(BMA, BookSide.BUY, OrderType.LIMIT, OrderStatus.NEW, 60, "1500"),
				order(ARS, BookSide.CASH_OUT, OrderType.MARKET, OrderStatus.FILLED, 100_000, "1"),
				order(BMA, BookSide.BUY, OrderType.LIMIT, OrderStatus.FILLED, 20, "1540"),
				order(METR, BookSide.BUY, OrderType.MARKET, OrderStatus.FILLED, 500, "250"),
				order(BMA, BookSide.SELL, OrderType.MARKET, OrderStatus.FILLED, 30, "1530"));
	}

	private static BookOrder order(
			int instrumentId,
			BookSide side,
			OrderType type,
			OrderStatus status,
			int size,
			String price) {
		return new BookOrder(USER, instrumentId, side, type, status, size, new BigDecimal(price));
	}
}

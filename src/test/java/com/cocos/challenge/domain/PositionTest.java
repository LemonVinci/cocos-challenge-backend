package com.cocos.challenge.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PositionTest {

	private static final int PAMP = 47;

	@Test
	void aSecondBuyIncreasesQuantityAndAddsItsCost() {
		Position position = Position.openLong(PAMP, 4, new BigDecimal("100"))
				.buyFilled(6, new BigDecimal("150"))
				.orElseThrow();

		assertThat(position.quantity()).isEqualTo(10);
		assertThat(position.costBasis()).isEqualByComparingTo("1300");
		assertThat(position.reservedQuantity()).isZero();
	}

	@Test
	void aPartialSaleKeepsTheCostOfTheSharesThatRemain() {
		Position position = holdingTenShares()
				.sellFilled(4, new BigDecimal("160"))
				.orElseThrow();

		assertThat(position.quantity()).isEqualTo(6);
		assertThat(position.costBasis()).isEqualByComparingTo("780");
	}

	@Test
	void sellingTheWholeQuantityRemovesThePosition() {
		assertThat(holdingTenShares().sellFilled(10, new BigDecimal("160"))).isEmpty();
	}

	@Test
	void aSellLimitReservesSharesWithoutChangingTheCost() {
		Position reserved = holdingTenShares().reserve(4);

		assertThat(reserved.quantity()).isEqualTo(10);
		assertThat(reserved.costBasis()).isEqualByComparingTo("1300");
		assertThat(reserved.reservedQuantity()).isEqualTo(4);
		assertThat(reserved.availableQuantity()).isEqualTo(6);

		Position released = reserved.release(4);
		assertThat(released.reservedQuantity()).isZero();
		assertThat(released.availableQuantity()).isEqualTo(10);
	}

	@Test
	void sellingMoreThanTheLongOpensAShortAtTheSalePrice() {
		Position position = shortTenShares();

		assertThat(position.quantity()).isEqualTo(-10);
		assertThat(position.costBasis()).isEqualByComparingTo("15300");
	}

	@Test
	void buyingAgainstAShortCoversItAndCanOpenANewLong() {
		Position stillShort = shortTenShares().buyFilled(4, new BigDecimal("1500")).orElseThrow();
		assertThat(stillShort.quantity()).isEqualTo(-6);
		assertThat(stillShort.costBasis()).isEqualByComparingTo("9180");

		assertThat(shortTenShares().buyFilled(10, new BigDecimal("1500"))).isEmpty();

		Position longAgain = shortTenShares().buyFilled(15, new BigDecimal("1500")).orElseThrow();
		assertThat(longAgain.quantity()).isEqualTo(5);
		assertThat(longAgain.costBasis()).isEqualByComparingTo("7500");
	}

	@Test
	void sellingMoreOfAShortAddsThatSaleToTheCost() {
		Position deeper = shortTenShares().sellFilled(2, new BigDecimal("1600")).orElseThrow();

		assertThat(deeper.quantity()).isEqualTo(-12);
		assertThat(deeper.costBasis()).isEqualByComparingTo("18500");
	}

	private static Position holdingTenShares() {
		return Position.openLong(PAMP, 4, new BigDecimal("100"))
				.buyFilled(6, new BigDecimal("150"))
				.orElseThrow();
	}

	private static Position shortTenShares() {
		return Position.openLong(PAMP, 20, new BigDecimal("1540"))
				.sellFilled(30, new BigDecimal("1530"))
				.orElseThrow();
	}
}

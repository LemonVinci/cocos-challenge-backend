package com.cocos.challenge.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountCashTest {

	@Test
	void cashInIncreasesSettledCash() {
		AccountCash cash = AccountCash.empty().deposit(new BigDecimal("10000"));

		assertThat(cash.settledCash()).isEqualByComparingTo("10000");
		assertThat(cash.reservedCash()).isEqualByComparingTo("0");
		assertThat(cash.available()).isEqualByComparingTo("10000");
	}

	@Test
	void cashOutDecreasesSettledCashWhenTheAmountIsAvailable() {
		AccountCash cash = funded().withdraw(new BigDecimal("700")).orElseThrow();

		assertThat(cash.settledCash()).isEqualByComparingTo("9300");
		assertThat(cash.reservedCash()).isEqualByComparingTo("0");
		assertThat(cash.available()).isEqualByComparingTo("9300");
	}

	@Test
	void cashOutAboveTheAvailableBalanceDoesNotMoveTheCash() {
		AccountCash funded = funded();

		assertThat(funded.withdraw(new BigDecimal("10001"))).isEmpty();
		assertThat(funded.settledCash()).isEqualByComparingTo("10000");
	}

	@Test
	void aBuyLimitReservesCashAndCancellingReleasesIt() {
		AccountCash reserved = funded().reserve(new BigDecimal("1800")).orElseThrow();

		assertThat(reserved.settledCash()).isEqualByComparingTo("10000");
		assertThat(reserved.reservedCash()).isEqualByComparingTo("1800");
		assertThat(reserved.available()).isEqualByComparingTo("8200");

		AccountCash released = reserved.release(new BigDecimal("1800"));
		assertThat(released.reservedCash()).isEqualByComparingTo("0");
		assertThat(released.available()).isEqualByComparingTo("10000");
	}

	@Test
	void aBuyLimitLargerThanAvailableCashIsNotReserved() {
		AccountCash funded = funded();

		assertThat(funded.reserve(new BigDecimal("10001"))).isEmpty();
		assertThat(funded.reservedCash()).isEqualByComparingTo("0");
	}

	private static AccountCash funded() {
		return AccountCash.empty().deposit(new BigDecimal("10000"));
	}
}

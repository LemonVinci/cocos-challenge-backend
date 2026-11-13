package com.cocos.challenge.application;

import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.CashFlow;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostCashTest {

	private final PostCash postCash = new PostCash(List.of(new DepositCash(), new WithdrawCash()));

	@Test
	void cashInAndCashOutGoThroughTheirOwnUseCase() {
		CashMovementResult deposited = postCash.execute(new CashMovementCommand(CashFlow.CASH_IN, new BigDecimal("10000"), AccountCash.empty()));
		CashMovementResult withdrawn = postCash.execute(new CashMovementCommand(CashFlow.CASH_OUT, new BigDecimal("700"), deposited.cash()));

		assertThat(deposited.accepted()).isTrue();
		assertThat(withdrawn.accepted()).isTrue();
		assertThat(withdrawn.cash().settledCash()).isEqualByComparingTo("9300");
		assertThat(withdrawn.cash().available()).isEqualByComparingTo("9300");
	}

	@Test
	void cashOutAboveTheAvailableBalanceIsNotAccepted() {
		AccountCash cash = AccountCash.empty().deposit(new BigDecimal("10000"));

		CashMovementResult result = postCash.execute(new CashMovementCommand(CashFlow.CASH_OUT, new BigDecimal("10001"), cash));

		assertThat(result.accepted()).isFalse();
		assertThat(result.cash()).isEqualTo(cash);
	}
}

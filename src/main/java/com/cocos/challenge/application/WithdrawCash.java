package com.cocos.challenge.application;

import com.cocos.challenge.domain.CashFlow;

public final class WithdrawCash implements PostCashMovement {

	@Override
	public boolean supports(CashFlow flow) {
		return CashFlow.CASH_OUT.equals(flow);
	}

	@Override
	public CashMovementResult execute(CashMovementCommand command) {
		return command.cash().withdraw(command.amount())
				.map(cash -> new CashMovementResult(true, cash))
				.orElseGet(() -> new CashMovementResult(false, command.cash()));
	}
}

package com.cocos.challenge.application;

import com.cocos.challenge.domain.CashFlow;

public final class DepositCash implements PostCashMovement {

	@Override
	public boolean supports(CashFlow flow) {
		return CashFlow.CASH_IN.equals(flow);
	}

	@Override
	public CashMovementResult execute(CashMovementCommand command) {
		return new CashMovementResult(true, command.cash().deposit(command.amount()));
	}
}

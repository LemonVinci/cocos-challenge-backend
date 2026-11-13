package com.cocos.challenge.application;

import com.cocos.challenge.domain.CashFlow;

public interface PostCashMovement extends UseCase<CashMovementCommand, CashMovementResult> {

	boolean supports(CashFlow flow);
}

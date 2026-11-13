package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.OrderStatus;

public record CancelOutcome(OrderStatus status, Account account) {

	public boolean cancelled() {
		return OrderStatus.CANCELLED.equals(status);
	}
}

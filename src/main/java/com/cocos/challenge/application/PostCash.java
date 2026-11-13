package com.cocos.challenge.application;

import java.util.List;

public final class PostCash implements UseCase<CashMovementCommand, CashMovementResult> {

	private final List<PostCashMovement> movements;

	public PostCash(List<PostCashMovement> movements) {
		this.movements = List.copyOf(movements);
	}

	@Override
	public CashMovementResult execute(CashMovementCommand command) {
		return movements.stream()
				.filter(movement -> movement.supports(command.flow()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported cash flow: " + command.flow()))
				.execute(command);
	}
}

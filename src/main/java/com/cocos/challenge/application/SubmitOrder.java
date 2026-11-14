package com.cocos.challenge.application;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class SubmitOrder implements UseCase<PlaceOrderCommand, PlacedOrder> {

	private final List<PlaceOrder> placements;

	public SubmitOrder(List<PlaceOrder> placements) {
		this.placements = List.copyOf(placements);
	}

	@Override
	public PlacedOrder execute(PlaceOrderCommand command) {
		return placements.stream()
				.filter(placement -> placement.supports(command.type()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unsupported order type: " + command.type()))
				.execute(command);
	}
}

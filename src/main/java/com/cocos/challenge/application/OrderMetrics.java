package com.cocos.challenge.application;

import com.cocos.challenge.domain.OrderStatus;
import com.cocos.challenge.domain.OrderType;
import com.cocos.challenge.domain.Side;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMetrics {

	private final MeterRegistry registry;

	public void accepted(Side side, OrderType type, OrderStatus status, boolean replayed) {
		registry.counter(
				"orders.accepted",
				"side", side.name(),
				"type", type.name(),
				"status", status.name(),
				"replayed", Boolean.toString(replayed)).increment();
	}

	public void cancelled(OrderStatus status, boolean released) {
		registry.counter(
				"orders.cancelled",
				"status", status.name(),
				"released", Boolean.toString(released)).increment();
	}
}

package com.cocos.challenge.application;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class AccountTiming {

	private final MeterRegistry registry;

	public AccountTiming(MeterRegistry registry) {
		this.registry = registry;
	}

	public void recordLock(long nanos) {
		registry.timer("account.lock").record(nanos, TimeUnit.NANOSECONDS);
	}

	public <T> T recordTransaction(String operation, Supplier<T> action) {
		long start = System.nanoTime();
		try {
			return action.get();
		} finally {
			registry.timer("account.transaction", "operation", operation)
					.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
		}
	}
}

package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RebuildProjection {

	private final OrderHistory orders;
	private final AccountProjectionStore accounts;
	private final ApplyBookOrder apply = new ApplyBookOrder();

	public RebuildProjection(OrderHistory orders, AccountProjectionStore accounts) {
		this.orders = orders;
		this.accounts = accounts;
	}

	@Transactional
	public void rebuildIfEmpty() {
		accounts.lockProjection();
		if (!accounts.isEmpty()) {
			return;
		}
		Map<Integer, Account> built = new LinkedHashMap<>();
		for (BookOrder order : orders.chronological()) {
			Account current = built.getOrDefault(order.userId(), Account.of(order.userId(), AccountCash.empty()));
			built.put(order.userId(), apply.apply(current, order));
		}
		built.values().forEach(accounts::save);
	}
}

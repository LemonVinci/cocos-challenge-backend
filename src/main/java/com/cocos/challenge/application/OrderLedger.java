package com.cocos.challenge.application;

import java.util.Optional;

public interface OrderLedger {

	int insert(OrderDraft draft);

	Optional<StoredOrder> find(int id);

	void markCancelled(int id);
}

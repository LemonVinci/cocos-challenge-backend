package com.cocos.challenge.application;

import java.util.Optional;

public interface OrderLedger {

	int insert(OrderDraft draft);

	Optional<StoredOrder> find(int id);

	Optional<StoredOrder> findByIdempotency(int userId, String idempotencyKey);

	void markCancelled(int id);
}

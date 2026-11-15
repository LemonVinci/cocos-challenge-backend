package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.AcceptOrder;
import com.cocos.challenge.application.AcceptedOrder;
import com.cocos.challenge.application.AccountTiming;
import com.cocos.challenge.application.CancelAccountOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

	private final AcceptOrder accept;
	private final CancelAccountOrder cancel;
	private final AccountTiming timing;

	@PostMapping
	public ResponseEntity<OrderResponse> place(
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
			@RequestBody OrderBody body) {
		return timing.recordTransaction("place", () -> accept.execute(body.toCommand(idempotencyKey)))
				.map(this::response)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@PostMapping("/{id}/cancel")
	public OrderResponse cancel(@PathVariable int id) {
		return timing.recordTransaction("cancel", () -> cancel.execute(id))
				.map(OrderResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private ResponseEntity<OrderResponse> response(AcceptedOrder accepted) {
		OrderResponse body = OrderResponse.from(accepted.receipt());
		if (accepted.replayed()) {
			return ResponseEntity.ok(body);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}
}

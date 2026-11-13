package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.AcceptOrder;
import com.cocos.challenge.application.CancelAccountOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/orders")
public class OrderController {

	private final AcceptOrder accept;
	private final CancelAccountOrder cancel;

	public OrderController(AcceptOrder accept, CancelAccountOrder cancel) {
		this.accept = accept;
		this.cancel = cancel;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> place(@RequestBody OrderBody body) {
		return accept.execute(body.toCommand())
				.map(receipt -> ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(receipt)))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	@PostMapping("/{id}/cancel")
	public OrderResponse cancel(@PathVariable int id) {
		return cancel.execute(id)
				.map(OrderResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}
}

package com.cocos.challenge.application;

public record AcceptedOrder(OrderReceipt receipt, boolean replayed) {
}

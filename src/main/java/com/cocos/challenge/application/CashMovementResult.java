package com.cocos.challenge.application;

import com.cocos.challenge.domain.AccountCash;

public record CashMovementResult(boolean accepted, AccountCash cash) {
}

package com.cocos.challenge.application;

import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.CashFlow;

import java.math.BigDecimal;

public record CashMovementCommand(CashFlow flow, BigDecimal amount, AccountCash cash) {
}

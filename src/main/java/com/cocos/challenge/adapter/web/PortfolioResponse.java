package com.cocos.challenge.adapter.web;

import com.cocos.challenge.application.AccountStatement;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
		int userId,
		BigDecimal totalValue,
		BigDecimal settledCash,
		BigDecimal availableCash,
		List<PositionResponse> positions) {

	public record PositionResponse(
			int instrumentId,
			String ticker,
			int quantity,
			BigDecimal marketValue,
			BigDecimal totalReturn,
			BigDecimal dailyReturn) {

		private static PositionResponse from(AccountStatement.Line line) {
			return new PositionResponse(
					line.instrumentId(),
					line.ticker(),
					line.quantity(),
					line.marketValue().orElse(null),
					line.totalReturnPercent().orElse(null),
					line.dailyReturnPercent().orElse(null));
		}
	}

	public static PortfolioResponse from(AccountStatement statement) {
		return new PortfolioResponse(
				statement.userId(),
				statement.totalValue().orElse(null),
				statement.settledCash(),
				statement.availableCash(),
				statement.lines().stream().map(PositionResponse::from).toList());
	}
}

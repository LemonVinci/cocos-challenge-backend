package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;
import com.cocos.challenge.domain.AccountCash;
import com.cocos.challenge.domain.Portfolio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
public class LoadPortfolio implements ViewPortfolio {

	private final AccountProjectionStore accounts;
	private final MarketBoard market;

	public LoadPortfolio(AccountProjectionStore accounts, MarketBoard market) {
		this.accounts = accounts;
		this.market = market;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<AccountStatement> execute(PortfolioQuery query) {
		if (!accounts.userExists(query.userId())) {
			return Optional.empty();
		}
		Account account = accounts.find(query.userId())
				.orElseGet(() -> Account.of(query.userId(), AccountCash.empty()));
		Set<Integer> instrumentIds = account.positions().keySet();
		Portfolio portfolio = Portfolio.value(account, market.quotes(instrumentIds, query.day()));
		return Optional.of(AccountStatement.of(account, portfolio, market.tickers(instrumentIds)));
	}
}

package com.cocos.challenge.application;

import com.cocos.challenge.domain.Account;

import java.util.Optional;

public interface AccountProjectionStore {

	void lockProjection();

	boolean isEmpty();

	Optional<Account> find(int userId);

	Account lockAccount(int userId);

	boolean userExists(int userId);

	void save(Account account);
}

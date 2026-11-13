package com.cocos.challenge.application;

public interface UseCase<C, R> {

	R execute(C command);
}

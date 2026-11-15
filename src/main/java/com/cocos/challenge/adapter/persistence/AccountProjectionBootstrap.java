package com.cocos.challenge.adapter.persistence;

import com.cocos.challenge.application.RebuildProjection;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountProjectionBootstrap implements ApplicationRunner {

	private final RebuildProjection projection;

	@Override
	public void run(ApplicationArguments args) {
		projection.rebuildIfEmpty();
	}
}

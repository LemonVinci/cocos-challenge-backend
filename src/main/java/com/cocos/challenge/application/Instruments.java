package com.cocos.challenge.application;

import com.cocos.challenge.domain.Instrument;

import java.util.Optional;

public interface Instruments {

	Optional<Instrument> findById(int id);
}

package io.ydanneg.service;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

@Service
@Slf4j
public class TimestampDaoService {

	public Completable save(List<Long> timestamps) {
		log.debug("saving: " + timestamps);
		return Completable.fromAction(() -> {
//			final int rndValue = new RandomDataGenerator().nextInt(0, 2);
			if (new Random().nextBoolean()) {
				log.trace("IO");
				throw new IOException();
			}
			//                    if (rndValue == 1) {
			//                        System.out.println("runtime");
			//                        throw new RuntimeException("something went wrong");
			//                    }
//			try {
//				log.trace("waiting...");
//				Thread.sleep(new RandomDataGenerator().nextLong(2000, 4000));
//				log.trace("waited!");
//			} catch (InterruptedException e) {
//				//
//			}

			log.debug("saved: " + timestamps);
		}).observeOn(Schedulers.computation());
	}
}

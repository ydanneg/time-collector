package io.ydanneg.service;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.springframework.stereotype.Service;

import io.reactivex.Completable;

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
			try {
				log.trace("waiting...");
				Thread.sleep(new RandomDataGenerator().nextLong(200, 2000));
				log.trace("waited!");
			} catch (InterruptedException e) {
				//
			}

			log.debug("saved: " + timestamps);
		});
	}

	public Completable saveOne(Long timestamp) throws IOException {
		return Completable.fromAction(() -> {
					log.debug("saving: " + timestamp);

					if (new RandomDataGenerator().nextInt(0, 2) == 0) {
						log.trace("IO");
						throw new IOException();
					}

					try {
						log.trace("waiting...");
						Thread.sleep(new RandomDataGenerator().nextLong(50, 400));
						log.trace("waited!");
					} catch (InterruptedException e) {
						//
					}
					log.debug("saved: " + timestamp);
				}
		);
	}
}

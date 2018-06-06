package io.ydanneg.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.ydanneg.model.Timestamp;
import io.ydanneg.model.TimestampRepository;

@Service
@Slf4j
public class TimestampDaoService {

	private final Random random = new Random();

	@Autowired
	private TimestampRepository repository;

	@Value("${io.ydanneg.timecollector.sim.ioexception:0}")
	private int simIOExceptionProbability;

	@Value("${io.ydanneg.timecollector.sim.delay:0}")
	private long simDelay;

	private static long sLastTs = 0;

	private boolean fileCreated;

	public Completable saveAll(List<Long> timestamps) {
		return Completable.fromAction(() -> doSaveAll(timestamps)
		);
	}

	public void doSaveAll(List<Long> timestamps) throws IOException {
		log.debug("saving: " + timestamps);

		// simulate IOException
		if (simIOExceptionProbability > 0 && random.nextInt(100) <= simIOExceptionProbability) {
			log.trace("throwing simulated IOException");
			throw new IOException();
		}

		// simulate delay
		if (simDelay > 0) {
			try {
				log.trace("simulated delay start");
				Thread.sleep(simDelay);
			} catch (InterruptedException e) {
				// ignored
			} finally {
				log.trace("simulated delay end");
			}
		}

		// assert persistence order
		if (timestamps.get(0) < sLastTs) {
			throw new RuntimeException("broken persistence order!!! OH NO!!!");
		}
		sLastTs = timestamps.get(timestamps.size() - 1);

		// persist into file
		File file = new File("output.txt");
		FileUtils.writeLines(file, "UTF-8", timestamps, fileCreated);
		// file is created now
		fileCreated = true;

		log.debug("saved: " + timestamps);
	}

	public Flowable<Timestamp> findAll() {
		return repository.findAll();
	}
}

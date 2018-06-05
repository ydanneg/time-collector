package io.ydanneg.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.reactivex.Completable;

@Service
@Slf4j
public class TimestampDaoService {

	private final Random random = new Random();

	@Value("${io.ydanneg.timecollector.sim.ioexception:0}")
	private int simIOExceptionProbability;

	@Value("${io.ydanneg.timecollector.sim.delay:0}")
	private long simDelay;

	private static long sLastTs = 0;
	public Completable saveAll(List<Long> timestamps) throws IOException {
		return Completable.fromAction(() -> {
					log.debug("saving: " + timestamps);

					// simulate IOException
					if (simIOExceptionProbability > 0 && random.nextInt(100) < simIOExceptionProbability) {
						log.trace("throwing simulated IOException");
						throw new IOException();
					}

					// simulate delay
					if (simDelay > 0) {
						try {
							log.trace("simulated delay start");
							Thread.sleep(simDelay);
							log.trace("simulated delay end");
						} catch (InterruptedException e) {
							//
						}
					}

					if (timestamps.get(0) < sLastTs) {
						throw new RuntimeException("error");
					}
					sLastTs = timestamps.get(timestamps.size() - 1);

					// persist into file
					File file = new File("output.txt");
					FileUtils.writeLines(file, "UTF-8", timestamps, true);

					log.debug("saved: " + timestamps);
				}
		);
	}
}

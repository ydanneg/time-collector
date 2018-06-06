package io.ydanneg.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * This class was used to simulate delay and error behaviors.
 * Not used anymore.
 */
@Service
@Slf4j
public class SimTimestampFileService {

	private final Random random = new Random();

	private long sLastTs;

	@Value("${io.ydanneg.timecollector.sim.ioexception:0}")
	private int simIOExceptionProbability;

	@Value("${io.ydanneg.timecollector.sim.delay:0}")
	private long simDelay;

	private boolean fileCreated;

	public void saveAll(List<Long> timestamps) throws IOException {
		log.debug("saving: " + timestamps);

		{ // simulate IOException
			if (simIOExceptionProbability > 0 && random.nextInt(100) <= simIOExceptionProbability) {
				log.trace("throwing simulated IOException");
				throw new IOException();
			}
		}

		{ // simulate delay
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
		}

		{ // assert persistence order
			if (timestamps.get(0) < sLastTs) {
				throw new RuntimeException("broken persistence order!!! OH NO!!!");
			}
			sLastTs = timestamps.get(timestamps.size() - 1);
		}

		// persist into file
		File file = new File("output.txt");
		FileUtils.writeLines(file, "UTF-8", timestamps, fileCreated);
		// file is created now
		fileCreated = true;

		log.debug("saved: " + timestamps);
	}
}

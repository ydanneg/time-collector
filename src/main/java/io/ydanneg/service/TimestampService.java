package io.ydanneg.service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import io.reactivex.Flowable;

@Service
public class TimestampService {

	public Flowable<Long> getTimestamps(long periodInMillis) {
		return Flowable.interval(periodInMillis, TimeUnit.MILLISECONDS)
				.map(ignored -> Instant.now().toEpochMilli());
	}
}

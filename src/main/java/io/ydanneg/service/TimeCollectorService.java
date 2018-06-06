package io.ydanneg.service;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.reactivex.BackpressureOverflowStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.ydanneg.model.Timestamp;
import io.ydanneg.model.TimestampRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeCollectorService {

	@Value("${io.ydanneg.timecollector.db.retry.delay:5000}")
	private Long retryDelayInMillis;

	@Value("${io.ydanneg.timecollector.timestamp.period:1000}")
	private Long timestampPeriodInMillis;

	@Value("${io.ydanneg.timecollector.capacity:5000}")
	private Long capacity;

	@Value("${io.ydanneg.timecollector.window:5}")
	private int windowSize;

	@Autowired
	private TimestampRepository repository;

	@Autowired
	private final TimestampService timestampService;
	@Autowired
	private final TimestampDaoService timestampDaoService;

	public void test() {
		System.out.println("--START--");
		final Long total = repository.findAll()
				.map(Timestamp::getTs)
				.doOnNext(System.out::println)
				.doOnError(System.out::println)
				.count()
				.blockingGet();

		System.out.println("Total: " + total);
		System.out.println("--END--");
	}

	public void startService() {
		final Flowable<Long> intervalPublisher = timestampService.getTimestamps(timestampPeriodInMillis);

		intervalPublisher
				.observeOn(Schedulers.newThread())
				.subscribe(aLong -> log.info(String.valueOf(aLong)));

		intervalPublisher
				.doOnNext(aLong -> log.debug("emitted: " + aLong))
				.onBackpressureBuffer(capacity, () -> log.warn("backpressure buffer overflow. dropping oldest"), BackpressureOverflowStrategy.DROP_OLDEST)
				.observeOn(Schedulers.newThread())
				.doOnNext(aLong -> log.debug("processing: " + aLong))
				.buffer(windowSize)
				.doOnNext(aLong -> log.debug("processing window: " + aLong))
				//                .observeOn(Schedulers.newThread())
				.map(longs -> longs.stream()
						.map(ts -> Timestamp.builder().ts(ts).build())
						.collect(Collectors.toList()))
				.flatMap(longs -> repository.saveAll(longs)
						.doOnNext(timestamp -> log.info("saved: " + timestamp))
						.doOnError(error -> log.error("failed to save into DB.", error))
						.observeOn(Schedulers.newThread())
						.retryWhen(throwableFlowable -> throwableFlowable.flatMap(throwable -> {
							//                            if (throwable instanceof IOException || throwable.getCause() instanceof IOException) {
							log.info("retry scheduled");
							// schedule retry
							return Flowable.timer(retryDelayInMillis, TimeUnit.MILLISECONDS);
							//                            }
							// propagate unexpected error
							//                            return Flowable.error(throwable);
						})), false, 1)
				.doOnError(throwable -> log.info("persistence failed", throwable))
				.blockingSubscribe();
	}
}

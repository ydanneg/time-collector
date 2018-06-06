package io.ydanneg.service;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

	@Value("${io.ydanneg.timecollector.retry:5000}")
	private Long retryDelayInMillis;

	@Value("${io.ydanneg.timecollector.period:1000}")
	private Long timestampPeriodInMillis;

	@Value("${io.ydanneg.timecollector.capacity:5000}")
	private Long capacity;

	@Value("${io.ydanneg.timecollector.window:5}")
	private int windowSize;

	private final TimestampRepository repository;
	private final TimestampService timestampService;

	public void print() {
		// find all records from DB
		final Long total = repository.findAll()
				// map into just a primitive ts
				.map(Timestamp::getTs)
				// and then into string
				.map(String::valueOf)
				// print into STDOUT
				.doOnNext(this::printAndLog)
				// print error into STDERR
				.doOnError(error -> printAndLogError("ERROR", error))
				// count total
				.count()
				// block until complete
				.blockingGet();

		System.out.println("Total: " + total);
	}

	public void collect() {
		// prepare a publisher that generates timestamps every 'timestampPeriodInMillis' period.
		// subscription is required to start emitting items
		final Flowable<Long> intervalPublisher = timestampService.getTimestamps(timestampPeriodInMillis);

		// first subscriber just prints generated timestamp immediately
		intervalPublisher
				// observing and printing in a dedicated thread
				.observeOn(Schedulers.newThread())
				// write ts into STDOUT and logger
				.subscribe(ts -> printAndLog(String.valueOf(ts)));

		// second subscriber to persist generated timestamps
		// it uses back-pressure with configurable buffer size '{capacity}'
		// splits emitted stream into bulks of '{window}' items
		// saves into MongoDB reactive repository
		// detects failure and schedules retry in '{retry}' period (infinitely :) )
		intervalPublisher
				// log emitted item
				.doOnNext(ts -> log.debug("emitted: " + ts))
				// use buffer back-pressure with size of '{capacity}'. Overflow of this buffer will drop OLDEST items.
				.onBackpressureBuffer(capacity, () -> log.warn("backpressure buffer overflow. dropping oldest"), BackpressureOverflowStrategy.DROP_OLDEST)
				// change to a new thread to process back-pressured items
				.observeOn(Schedulers.newThread())
				// log back-pressured item
				.doOnNext(ts -> log.debug("processing: " + ts))
				// split into bulk of '{window}' items to optimize persistence
				.buffer(windowSize)
				.doOnNext(tsList -> log.debug("processing window: " + tsList))
				// map raw timestamps into list of entities
				.map(tsList -> tsList.stream()
						// map into Entity
						.map(ts -> Timestamp.builder()
								.ts(ts)
								.build())
						// collect into list
						.collect(Collectors.toList()))
				// map stream into persistence
				.flatMap(entities -> repository.saveAll(entities)
						// log a save attempt
						.doOnSubscribe(subscription -> log.debug("saving..."))
						// log saved entity
						.doOnNext(savedEntity -> log.debug("saved: " + savedEntity))
						// print and log saving error
						.doOnError(error -> printAndLogError("Error saving into DB. retrying in " + retryDelayInMillis + " ms", error))
						// change thread (not necessary actually)
						.observeOn(Schedulers.newThread())
						// schedule retry for previous saveAll operation
						// flatMap concurrency is '1' to keep order (to not accept next items while waiting for this retry, back-pressure buffer will collect items)
						.retryWhen(throwableFlowable -> throwableFlowable.flatMap(throwable -> {
							log.debug("retry scheduled");
							// schedule retry on any repository error
							// TODO: handle only IO related errors
							return Flowable.timer(retryDelayInMillis, TimeUnit.MILLISECONDS)
									.doOnNext(throwable1 -> printAndLog("retrying save into DB..."));
						})), false, 1)
				// print and log unhandled error
				.doOnError(throwable -> printAndLogError("unexpected error", throwable))
				// block current thread until complete (actually never) or interrupted
				.blockingSubscribe();
	}

	private void printAndLog(String text) {
		System.out.println(text);
		log.info(text);
	}

	private void printAndLogError(String text, Throwable throwable) {
		System.err.println(text + "\ncause: " + throwable.getMessage());
		log.error(text, throwable);
	}
}

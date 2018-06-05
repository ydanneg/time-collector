package io.ydanneg.model;

import org.springframework.data.repository.reactive.RxJava2CrudRepository;

public interface TimestampRepository extends RxJava2CrudRepository<Timestamp, String> {
}

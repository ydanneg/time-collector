package io.ydanneg.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
public class Timestamp {

	@Id
	private String id;

	private Long ts;

	@Builder
	public Timestamp(Long ts) {
		this.ts = ts;
	}
}

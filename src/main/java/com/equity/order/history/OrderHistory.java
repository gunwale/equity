package com.equity.order.history;

import java.text.MessageFormat;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// this is the aggregate for orders in order book
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistory {

	@Id
	private Long id;

	@Lob
	@NotNull
	private String orderBook;
	@Lob
	@NotNull
	private String orderItem;

	@Enumerated(EnumType.STRING)
	private Status status;

	private Date createdDate;

	// it takes single entry of immutable order event which can be processed to
	// get the final state of an order book
	public enum Status {
		ORDER_CREATED, ORDER_CANCELED, EXECUTED
	}

	@PrePersist
	void init() {
		this.id = System.nanoTime(); // quick id generation
		this.createdDate = new Date();
		this.status = Status.ORDER_CREATED;
	}

	@PreUpdate
	void preUpdate() throws Exception {
		throw new Exception(MessageFormat.format("{0} is immutable",
				this.getClass().getSimpleName()));
	}

}

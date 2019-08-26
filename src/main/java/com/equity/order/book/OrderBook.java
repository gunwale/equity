package com.equity.order.book;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBook {

	@Id
	private Long id;
	@NotNull
	private String instrument;

	@Enumerated(EnumType.STRING)
	private Status status;

	private Date createdDate;
	private Date updatedDate;

	public enum Status {
		OPEN, CLOSED
	}

	@PrePersist
	void prePersist() {
		this.id = System.nanoTime(); // quick id generation
		this.status = Status.OPEN;
		this.createdDate = new Date();
		this.updatedDate = createdDate;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedDate = new Date();
	}
}
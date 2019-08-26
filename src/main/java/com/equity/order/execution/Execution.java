package com.equity.order.execution;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.Immutable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Immutable
@Table
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Execution {

	@Id
	private Long id;
	@NotNull
	private Integer quantity;
	@NotNull
	private BigDecimal price;

	private Date createdDate;

	@PrePersist
	void init() {
		this.id = System.nanoTime(); // quick id generation
		this.createdDate = new Date();
	}

	// by right should also be immutable?
	@PreUpdate
	void preUpdate() throws Exception {
		throw new Exception(MessageFormat.format("{0} is immutable",
				this.getClass().getSimpleName()));
	}
}
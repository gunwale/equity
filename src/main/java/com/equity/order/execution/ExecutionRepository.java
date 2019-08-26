package com.equity.order.execution;

import org.springframework.data.repository.CrudRepository;

public interface ExecutionRepository extends CrudRepository<Execution, Long> {
}
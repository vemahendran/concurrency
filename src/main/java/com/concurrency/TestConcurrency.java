package com.concurrency;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestConcurrency {

	public static final int MAX_TASKS = 10;
	public static final int SECONDS = 1;

	public static void main(String[] args) {

		List<MyTask> tasks = IntStream.range(0, MAX_TASKS)
									.mapToObj(i -> new MyTask(SECONDS))
									.collect(Collectors.toList());

		Concurrency concurrency = new ConcurrencyImpl();

		concurrency.useSequential(tasks);
		concurrency.useCompletableFutureWithExecutor(tasks);
		concurrency.useCompletableFuture(tasks);
		concurrency.useParallelStream(tasks);
	}
}

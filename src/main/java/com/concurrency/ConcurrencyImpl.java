package com.concurrency;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ConcurrencyImpl implements Concurrency {

	public void useSequential(List<MyTask> tasks) {
		System.out.println("Run on sequential manner. \nWait...");
		
		Instant startTime = Instant.now();
		List<Integer> result = tasks.stream()
									.map(MyTask::calculate)
									.collect(Collectors.toList());

		Instant endTime = Instant.now();
		printDuration(tasks, startTime, endTime);
	}
	

	public void useParallelStream(List<MyTask> tasks) {
		System.out.println("Run using a parallel stream. \nWait...");
		
		Instant startTime = Instant.now();
		List<Integer> result = tasks.parallelStream()
									.map(MyTask::calculate)
									.collect(Collectors.toList());

		Instant endTime = Instant.now();
		printDuration(tasks, startTime, endTime);
	}

	public void useCompletableFuture(List<MyTask> tasks) {
		System.out.println("Run using CompletableFutures. \nWait...");
		
		Instant startTime = Instant.now();
		List<CompletableFuture<Integer>> futures = tasks.stream()
														.map(tmpTask -> {
																return CompletableFuture.supplyAsync(() -> {
																	return tmpTask.calculate();
																});
														})
														.collect(Collectors.toList());

		List<Integer> result = futures.stream()
										.map(CompletableFuture::join)
										.collect(Collectors.toList());

		Instant endTime = Instant.now();
		printDuration(tasks, startTime, endTime);
	}

	public void useCompletableFutureWithExecutor(List<MyTask> tasks) {
		System.out.println("Run CompletableFutures with a custom Executor. \nWait...");
		
		final int NUM_OF_THREADS = 1000;
		
		Instant startTime = Instant.now();
		
		ExecutorService executor = Executors.newFixedThreadPool(Math.min(tasks.size(), NUM_OF_THREADS));
		
		List<CompletableFuture<Integer>> futures = tasks.stream()
						.map(tmpTask -> {
							return CompletableFuture.supplyAsync(() -> {
								return tmpTask.calculate();
							}, executor);
						})
						.collect(Collectors.toList());

		List<Integer> result = futures.stream()
									.map(CompletableFuture::join)
									.collect(Collectors.toList());
		
		Instant endTime = Instant.now();
		printDuration(tasks, startTime, endTime);
		
		executor.shutdown();
	}
	
	private void printDuration(List<MyTask> tasks, Instant start, Instant end) {
		Duration timeElapsed = Duration.between(start, end);
		System.out.printf("Processed %d tasks in %d milliseconds\n", tasks.size(), timeElapsed.toMillis());
	}

}

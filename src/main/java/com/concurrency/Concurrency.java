package com.concurrency;

import java.util.List;

public interface Concurrency {
	public void useSequential(List<MyTask> tasks);

	public void useParallelStream(List<MyTask> tasks);

	public void useCompletableFuture(List<MyTask> tasks);

	public void useCompletableFutureWithExecutor(List<MyTask> tasks);
}

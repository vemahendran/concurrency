# concurrency

### A quick check on performance of CompletableFuture and Parallel Stream

In my recent project, I had to improve the performance of existing java application. The application was written on plain java. It doesn't use any frameworks (e.g. Spring). The app was primarily written for infra purposes like handling Kubernetes workloads. Most of the time, the app is making hundreds of API calls to [Kubernetes API](https://github.com/kubernetes-client/java/) server and process the json payloads. Some of the api calls and the processing tasks can be run independently.

Once I got to know that I could run some tasks independently, the next thing which came to my mind was threads. As usual, I googled, and read the articles related to thread pools. Of course, I didn't miss to check [Baeldung](https://www.baeldung.com/java-concurrency) also. Then finally I decided to use `CompletableFuture` which gives you a lot of customized options to run your workloads in asynchronous way. I also wanted to give a quick check on `Collection.parallelStream`. Here is what I found in terms of performance.

Here is a sample task. I have `MyTask` class which has a `calculate` method and it just runs `TimeUnit.SECONDS.sleep(seconds)` statement and taking seconds as an input.

```
import java.util.concurrent.TimeUnit;

public class MyTask {
	private final int seconds;

	public MyTask(int seconds) {
		this.seconds = seconds;
	}

	public int calculate() {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
		return seconds;
	}
}
```

Here is the interface called `Concurrency` which has four methods. We have a class 'ConcurrencyImpl' that has implementation for all these four methods. We will see the implementation one by one.

```
public interface Concurrency {
	public void useSequential(List<MyTask> tasks);

	public void useParallelStream(List<MyTask> tasks);

	public void useCompletableFuture(List<MyTask> tasks);

	public void useCompletableFutureWithExecutor(List<MyTask> tasks);
}
```

Before diving into the implementations, we will see the main method which tests the implementations.

```
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
```

Instead of creating a `for loop` and assigning each task in a list, we can combine everything in a single statement. Thanks to Java Streams.

```
List<MyTask> tasks = IntStream.range(0, MAX_TASKS)
            .mapToObj(i -> new MyTask(SECONDS))
            .collect(Collectors.toList());
```

Here is the implementation for the method `useSequential(tasks)`. Again we are using stream to run each task.

```
public void useSequential(List<MyTask> tasks) {
	System.out.println("Run on sequential manner. \nWait...");

	Instant startTime = Instant.now();
	List<Integer> result = tasks.stream()
				.map(MyTask::calculate)
				.collect(Collectors.toList());

	Instant endTime = Instant.now();
	printDuration(tasks, startTime, endTime);
}
```

If we have ten tasks and each task requires one second to run, then the method will take ten seconds to complete the execution. That's what you can see in the console output.

```
Run on sequential manner.
Wait...
Processed 10 tasks in 10046 milliseconds
```

The next one is the implementation for the method `useCompletableFuture`.  It uses `CompletableFuture.supplyAsync()` method to run the tasks. Each task will run on a separate thread. The method creates ten threads for ten tasks. This is the maximum number of threads created by default. Suppose if we increase the number of tasks to fifty, we will only get ten threads created at that time.

```
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
```

The console output.

```
Run using CompletableFutures.
Wait...
Processed 10 tasks in 1005 milliseconds
```

In the above implementation, we don't have a control on creating the number of threads. So, we will use the `Executors` to set the number for threads. The next method `useCompletableFutureWithExecutor` additionally has the `Executors.newFixedThreadPool` which helps us to decide the number of thread. We have `Math.min` which prevents us from creating number of threads more than the number of tasks.


```
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
```

We can see that `useCompletableFutureWithExecutor` prints approximately the same execution time as seen in the method `useCompletableFuture`.

```
Run CompletableFutures with a custom Executor.
Wait...
Processed 10 tasks in 1010 milliseconds
```

Let's check what happen if we just leverage streams' parallel feature.

```
public void useParallelStream(List<MyTask> tasks) {
	System.out.println("Run using a parallel stream. \nWait...");

	Instant startTime = Instant.now();
	List<Integer> result = tasks.parallelStream()
				.map(MyTask::calculate)
				.collect(Collectors.toList());

	Instant endTime = Instant.now();
	printDuration(tasks, startTime, endTime);
}
```

The console output for the method `useParallelStream`.

```
Run using a parallel stream.
Wait...
Processed 10 tasks in 1006 milliseconds
```

In a nutshell, we don't see the much difference on the performance for small number of tasks. All three methods takes one second to complete ten tasks.

Once I increase the count to hundred, the parallel stream performs better.

```
Run using CompletableFutures.
Wait...
Processed 100 tasks in 10033 milliseconds

Run CompletableFutures with a custom Executor.
Wait...
Processed 100 tasks in 10038 milliseconds

Run using a parallel stream.
Wait...
Processed 100 tasks in 9033 milliseconds
```
What if we increase the count to thousand. We will ignore `useCompletableFuture` method. Now, we will only check the difference between `useCompletableFutureWithExecutor` and `useParallelStream`. Here is the output.

```
Run CompletableFutures with a custom Executor.
Wait...
Processed 1000 tasks in 100301 milliseconds

Run using a parallel stream.
Wait...
Processed 1000 tasks in 93246 milliseconds
```
We can still see the parallel stream performs better. Parallel stream creates only fifteen threads approximately at the time.

Let's check by increasing the `NUM_OF_THREADS` count to thousand on `useCompletableFutureWithExecutor` for thousand tasks.

```
Run CompletableFutures with a custom Executor.
Wait...
Processed 1000 tasks in 1102 milliseconds

Run using a parallel stream.
Wait...
Processed 1000 tasks in 93246 milliseconds
```

Phew! It merely takes one second to complete thousand tasks. It's obvious that `CompletableFutures` gives more power to developer for handling threads. Both of the implementations internally use `ForkJoinPool`. In terms of easy of use, parallel stream looks very handy for running tasks which are less than hundred.

All the sample code can be referred in my [github](https://github.com/vemahendran/concurrency/tree/master/src/main/java/com/concurrency).

I hope you've found this post useful. In the next post, we will see more about `CompletableFutures`, `Executors`, and Google's [Guava](https://github.com/google/guava) concurrent features. Thanks!

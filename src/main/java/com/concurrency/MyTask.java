package com.concurrency;

import java.util.concurrent.TimeUnit;

public class MyTask {
	private final int seconds;

	public MyTask(int seconds) {
		this.seconds = seconds;
	}

	public int calculate() {

//		System.out.println(Thread.currentThread().getName());
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
		return seconds;
	}
}

package com.spicymango.fanfictionreader.services;

/**
 * Created by Michael Chen on 03/01/2016.
 */
public class StoryNotFoundException extends Exception {

	/**
	 * Constructs a new {@code StoryNotFoundException} that includes the current stack trace.
	 */
	public StoryNotFoundException() {
	}

	/**
	 * Constructs a new {@code StoryNotFoundException} with the current stack trace and the
	 * specified detail message.
	 *
	 * @param detailMessage the detail message for this exception.
	 */
	public StoryNotFoundException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * Constructs a new {@code StoryNotFoundException} with the current stack trace, the specified
	 * detail message and the specified cause.
	 *
	 * @param detailMessage the detail message for this exception.
	 * @param throwable
	 */
	public StoryNotFoundException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	/**
	 * Constructs a new {@code StoryNotFoundException} with the current stack trace and the
	 * specified cause.
	 *
	 * @param throwable the cause of this exception.
	 */
	public StoryNotFoundException(Throwable throwable) {
		super(throwable);
	}
}

package com.samysadi.acs.core.tracing;

/**
 * 
 * @since 1.0
 */
public interface IncrementableProbe<T> extends ModifiableProbe<T> {
	public void increment();
}

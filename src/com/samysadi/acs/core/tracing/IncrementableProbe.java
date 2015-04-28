package com.samysadi.acs.core.tracing;

/**
 * 
 * @author Samy Sadi <samy.sadi.contact@gmail.com>
 * @author Belabbas Yagoubi <byagoubi@gmail.com>
 * @since 1.0
 */
public interface IncrementableProbe<T> extends ModifiableProbe<T> {
	public void increment();
}

package io.daniellavoie.jpareactivewrapper.sample;

public class SaveEntityResponse<T> {
	private final String requestId;
	private final T returnValue;
	private final RuntimeException error;

	public SaveEntityResponse(final String requestId, final T returnValue, final RuntimeException error) {
		this.requestId = requestId;
		this.returnValue = returnValue;
		this.error = error;
	}

	public String getRequestId() {
		return requestId;
	}

	public T getReturnValue() {
		return returnValue;
	}

	public RuntimeException getError() {
		return error;
	}
}

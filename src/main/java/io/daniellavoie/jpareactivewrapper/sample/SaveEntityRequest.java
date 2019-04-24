package io.daniellavoie.jpareactivewrapper.sample;

public class SaveEntityRequest<T> {
	private final String requestId;
	private final T entity;

	public SaveEntityRequest(final String requestId, final T entity) {
		this.requestId = requestId;
		this.entity = entity;
	}

	public String getRequestId() {
		return requestId;
	}

	public T getEntity() {
		return entity;
	}
}

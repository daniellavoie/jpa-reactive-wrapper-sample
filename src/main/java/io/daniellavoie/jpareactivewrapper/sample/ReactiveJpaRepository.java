package io.daniellavoie.jpareactivewrapper.sample;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class ReactiveJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {
	private DirectProcessor<SaveEntityRequest<T>> requestProcessor = DirectProcessor.create();
	private Flux<SaveEntityRequest<T>> requestFlux = requestProcessor.onBackpressureBuffer();

	DirectProcessor<SaveEntityResponse<T>> responseProcessor = DirectProcessor.create();
	Flux<SaveEntityResponse<T>> responseFlux = responseProcessor.onBackpressureBuffer(100,
			BufferOverflowStrategy.ERROR);

	public ReactiveJpaRepository(Class<T> domainClass, EntityManager em) {
		super(domainClass, em);

		requestFlux.buffer(Duration.ofSeconds(1)).map(this::processRequests)

				.flatMapIterable(responses -> responses)

				.doOnNext(response -> responseProcessor.onNext(response))

				.subscribeOn(Schedulers.newParallel("jpa-reactive-wrapper", 4))

				.subscribe();
	}

	public T batchSave(T entity) {
		SaveEntityRequest<T> request = new SaveEntityRequest<T>(UUID.randomUUID().toString(), entity);

		SaveEntityResponse<T> response = responseFlux
				.filter(responseNotification -> request.getRequestId().equals(responseNotification.getRequestId()))

				.doOnSubscribe(subcriber -> requestProcessor.onNext(request))

				.blockFirst();

		if (response.getError() != null) {
			throw response.getError();
		}

		return response.getReturnValue();
	}

	public List<SaveEntityResponse<T>> processRequests(List<SaveEntityRequest<T>> requests) {
		try {
			List<T> returnValues = this
					.save(requests.stream().map(SaveEntityRequest::getEntity).collect(Collectors.toList()));

			AtomicInteger index = new AtomicInteger();

			return returnValues.stream()
					.map(returnValue -> new SaveEntityResponse<T>(requests.get(index.getAndIncrement()).getRequestId(),
							returnValue, null))
					.collect(Collectors.toList());
		} catch (Exception ex) {
			return requests.stream().map(this::processRequest).collect(Collectors.toList());
		}
	}

	public SaveEntityResponse<T> processRequest(SaveEntityRequest<T> request) {
		try {
			return new SaveEntityResponse<>(request.getRequestId(), save(request.getEntity()), null);
		} catch (RuntimeException ex) {
			return new SaveEntityResponse<T>(request.getRequestId(), null, ex);
		}
	}

}

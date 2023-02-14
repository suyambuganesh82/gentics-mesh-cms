package com.gentics.mesh.core.data.page.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mesh.parameter.PagingParameters;

/**
 * Streaming page implementation.
 * 
 * @param <T>
 */
public class DynamicStreamPageImpl<T> extends AbstractDynamicPage<T> {

	public DynamicStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo) {
		this(stream, pagingInfo, null);
	}

	/**
	 * Creates a new page with a filter applied to the stream
	 *
	 * @param stream
	 *            a stream of elements to be paged
	 * @param pagingInfo
	 *            paging info the user requested
	 * @param filter
	 *            the filter to be applied to the stream
	 */
	public DynamicStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo, Predicate<? super T> filter) {
		super(pagingInfo);
		init(filter != null ? stream.filter(filter) : stream);
	}

	private void init(Stream<? extends T> stream) {
		AtomicLong pageCounter = new AtomicLong();
		stream = stream
			.map(item -> {
				totalCounter.incrementAndGet();
				return item;
			});

		// Apply paging - skip to lower bounds
		if (lowerBound != null) {
			stream = stream.skip(lowerBound);
		}

		visibleItems = stream.map(item -> {
			// Only add elements to the list if those elements are part of selected the page
			long elementsInPage = pageCounter.get();
			if (lowerBound == null || elementsInPage < perPage) {
				elementsOfPage.add(item);
				pageCounter.incrementAndGet();
			} else {
				pageFull.set(true);
				hasNextPage.set(true);
			}
			return item;
		}).iterator();
	}

}

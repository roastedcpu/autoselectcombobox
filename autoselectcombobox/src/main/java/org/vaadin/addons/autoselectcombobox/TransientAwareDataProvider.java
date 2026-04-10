package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.data.provider.AbstractDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Wraps an existing {@link DataProvider} and merges its results with items
 * from a {@link TransientItemStore}. Transient items appear alongside
 * persisted items in queries and counts.
 * <p>
 * When the transient store changes, this provider automatically refreshes.
 *
 * @param <T> the item type
 * @param <F> the filter type (typically String for combo boxes)
 */
public class TransientAwareDataProvider<T, F> extends AbstractDataProvider<T, F> {

    private final DataProvider<T, F> delegate;
    private final TransientItemStore<T> transientStore;
    private final TransientItemFilter<T, F> transientFilter;
    private Runnable storeListenerRemover;

    /**
     * @param delegate       the underlying (persisted) data provider
     * @param transientStore the shared transient item store
     * @param transientFilter filter logic to apply to transient items
     *                        (matching the delegate's filtering behavior)
     */
    public TransientAwareDataProvider(DataProvider<T, F> delegate,
                                      TransientItemStore<T> transientStore,
                                      TransientItemFilter<T, F> transientFilter) {
        this.delegate = delegate;
        this.transientStore = transientStore;
        this.transientFilter = transientFilter;

        storeListenerRemover = transientStore.addChangeListener(this::refreshAll);
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public int size(Query<T, F> query) {
        int delegateSize = delegate.size(query);
        long transientSize = getFilteredTransientItems(query.getFilter().orElse(null)).size();
        return delegateSize + (int) transientSize;
    }

    @Override
    public Stream<T> fetch(Query<T, F> query) {
        Stream<T> delegateStream = delegate.fetch(query);
        List<T> transientItems = getFilteredTransientItems(query.getFilter().orElse(null));

        if (transientItems.isEmpty()) {
            return delegateStream;
        }

        // Prepend transient items so they appear at the top
        List<T> combined = new ArrayList<>(transientItems);
        delegateStream.forEach(combined::add);
        return combined.stream();
    }

    private List<T> getFilteredTransientItems(F filter) {
        List<T> items = transientStore.getItems();
        if (items.isEmpty()) {
            return items;
        }
        if (filter == null) {
            return items;
        }
        List<T> filtered = new ArrayList<>();
        for (T item : items) {
            if (transientFilter.test(item, filter)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /** Detach the store change listener. Call when this provider is no longer needed. */
    public void detach() {
        if (storeListenerRemover != null) {
            storeListenerRemover.run();
            storeListenerRemover = null;
        }
    }

    /**
     * Filter predicate for transient items. Mirrors whatever filtering
     * logic the delegate data provider uses.
     *
     * @param <T> item type
     * @param <F> filter type
     */
    @FunctionalInterface
    public interface TransientItemFilter<T, F> {
        boolean test(T item, F filter);
    }
}

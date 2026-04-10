package org.vaadin.addons.autoselectcombobox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Holds items that have been created via custom value entry but not yet
 * persisted. Shared across multiple combo boxes so that a value created
 * in one place is immediately available in others.
 * <p>
 * Typical lifecycle:
 * <ol>
 *   <li>User creates "AAA" in Requisition #1 → added to this store</li>
 *   <li>Requisition #2 sees "AAA" in its dropdown (same store)</li>
 *   <li>User saves the episode → {@link #commitAll(Consumer)} persists items, clears store</li>
 *   <li>User cancels → {@link #discardAll()} clears without persisting</li>
 * </ol>
 * <p>
 * Thread-safe: multiple UI components may read concurrently.
 *
 * @param <T> the item type
 */
public class TransientItemStore<T> {

    private final CopyOnWriteArrayList<T> items = new CopyOnWriteArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();

    /** Add a transient item and notify listeners. */
    public void add(T item) {
        if (item == null) {
            return;
        }
        items.addIfAbsent(item);
        fireChange();
    }

    /** Remove a transient item and notify listeners. */
    public void remove(T item) {
        if (items.remove(item)) {
            fireChange();
        }
    }

    /** Returns an unmodifiable snapshot of current transient items. */
    public List<T> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public boolean contains(T item) {
        return items.contains(item);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    /**
     * Persist all transient items via the given consumer, then clear the store.
     *
     * @param persistAction called once with the full list of transient items
     */
    public void commitAll(Consumer<List<T>> persistAction) {
        if (!items.isEmpty()) {
            persistAction.accept(getItems());
        }
        items.clear();
        fireChange();
    }

    /** Discard all transient items without persisting. */
    public void discardAll() {
        if (!items.isEmpty()) {
            items.clear();
            fireChange();
        }
    }

    /**
     * Register a listener that fires whenever the store contents change.
     * Used by data providers to refresh their views.
     *
     * @return a runnable that removes the listener when called
     */
    public Runnable addChangeListener(Runnable listener) {
        changeListeners.add(listener);
        return () -> changeListeners.remove(listener);
    }

    private void fireChange() {
        changeListeners.forEach(Runnable::run);
    }
}

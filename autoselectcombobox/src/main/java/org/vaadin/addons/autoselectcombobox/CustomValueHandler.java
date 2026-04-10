package org.vaadin.addons.autoselectcombobox;

import java.util.function.Consumer;

/**
 * Handler invoked when the user submits a value that does not exist in the
 * combo box's data source.
 * <p>
 * Implementations decide how to handle the new value — for example, by opening
 * a creation dialog. When the new item is ready, call {@code onSubmit}. To
 * cancel and revert the combo box to its previous value, call {@code onCancel}.
 * <p>
 * If neither callback is invoked, the component remains in a pending state
 * with the typed text visible but no model value set.
 *
 * @param <T> the item type of the combo box
 */
@FunctionalInterface
public interface CustomValueHandler<T> {

    /**
     * Handle a custom (non-existing) value.
     *
     * @param customText the text the user typed
     * @param onSubmit   call with the newly created item to accept it
     * @param onCancel   call to discard the custom value and revert
     */
    void handle(String customText, Consumer<T> onSubmit, Runnable onCancel);
}

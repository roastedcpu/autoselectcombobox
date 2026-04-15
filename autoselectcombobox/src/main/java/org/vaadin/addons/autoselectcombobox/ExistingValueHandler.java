package org.vaadin.addons.autoselectcombobox;

import java.util.function.Consumer;

/**
 * Handler invoked when the user presses Enter (or another configured trigger)
 * on a value that is already selected and unchanged — typically to edit it.
 * <p>
 * The component does not prescribe what "edit" means. Implementations decide
 * whether to open a dialog, navigate, or ignore the action.
 *
 * @param <T> the item type of the combo box
 */
@FunctionalInterface
public interface ExistingValueHandler<T> {

    /**
     * Handle Enter on an existing selected value.
     *
     * @param currentItem the currently selected item
     * @param onUpdate    call with the updated item to replace the current value
     * @param onCancel    call to keep the current value unchanged
     */
    void handle(T currentItem, Consumer<T> onUpdate, Runnable onCancel);
}

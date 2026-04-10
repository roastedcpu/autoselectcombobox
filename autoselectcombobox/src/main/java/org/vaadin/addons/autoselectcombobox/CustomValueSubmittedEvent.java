package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.ComponentEvent;

/**
 * Fired when a custom value has been accepted, either directly (no handler)
 * or via a {@link CustomValueHandler}'s submit callback.
 *
 * @param <T> the item type of the combo box
 */
public class CustomValueSubmittedEvent<T> extends ComponentEvent<AutoSelectComboBox<T>> {

    private final String customText;
    private final T item;

    public CustomValueSubmittedEvent(AutoSelectComboBox<T> source, String customText, T item) {
        super(source, false);
        this.customText = customText;
        this.item = item;
    }

    /** The original text the user typed. */
    public String getCustomText() {
        return customText;
    }

    /** The item produced by the handler, or {@code null} if no handler was set. */
    public T getItem() {
        return item;
    }
}

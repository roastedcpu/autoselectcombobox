package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.ComponentEvent;

/**
 * Fired when a custom value action was cancelled — either by the user closing
 * the handler dialog or by calling the cancel callback.
 * The combo box value has already been reverted when this event fires.
 *
 * @param <T> the item type of the combo box
 */
public class CustomValueCancelledEvent<T> extends ComponentEvent<AutoSelectComboBox<T>> {

    private final String customText;

    public CustomValueCancelledEvent(AutoSelectComboBox<T> source, String customText) {
        super(source, false);
        this.customText = customText;
    }

    /** The text the user had typed before the action was cancelled. */
    public String getCustomText() {
        return customText;
    }
}

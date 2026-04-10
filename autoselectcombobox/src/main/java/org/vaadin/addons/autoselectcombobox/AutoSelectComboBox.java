package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A ComboBox that auto-selects the only matching item on blur/Enter,
 * with optional support for accepting values not present in the data source.
 * <p>
 * <b>Custom value flow:</b>
 * <ol>
 *   <li>User types text not matching any item and triggers (Enter/Tab/Blur)</li>
 *   <li>{@link CustomValueValidator} runs — if it fails, error is shown, flow stops</li>
 *   <li>If a {@link CustomValueHandler} is set, it is called with submit/cancel callbacks</li>
 *   <li>If no handler is set, a {@link CustomValueSubmittedEvent} fires directly</li>
 *   <li>On cancel, the value reverts and a {@link CustomValueCancelledEvent} fires</li>
 * </ol>
 *
 * @param <T> the item type
 */
@Tag("vcf-auto-select-combo-box")
@JsModule("./vcf-auto-select-combobox.js")
public class AutoSelectComboBox<T> extends ComboBox<T> {

    private boolean initializing;
    private boolean customValuesAllowed;
    private Set<CustomValueTrigger> customValueTriggers = EnumSet.of(CustomValueTrigger.ENTER);
    private CustomValueHandler<T> customValueHandler;
    private CustomValueValidator customValueValidator = CustomValueValidator.acceptAll();

    private TransientItemStore<T> transientStore;
    private T previousValue;
    private String pendingCustomText;
    private AtomicBoolean pendingHandlerResponse;

    public AutoSelectComboBox() {
        super();
        init();
    }

    public AutoSelectComboBox(int pageSize) {
        super(pageSize);
        init();
    }

    public AutoSelectComboBox(String label, Collection<T> items) {
        super(label, items);
        init();
    }

    @SafeVarargs
    public AutoSelectComboBox(String label, T... items) {
        super(label, items);
        init();
    }

    public AutoSelectComboBox(String label) {
        super(label);
        init();
    }

    private void init() {
        initializing = true;
        addCustomValueSetListener(e -> onCustomValueSet(e.getDetail()));
        initializing = false;
        addValueChangeListener(e -> previousValue = e.getOldValue());
    }

    private void onCustomValueSet(String customText) {
        if (!customValuesAllowed) {
            setValue(null);
            getElement().executeJs("this.inputElement.value = $0;", customText);
            return;
        }

        CustomValueValidator.Result result = customValueValidator.validate(customText);
        if (!result.valid()) {
            setValue(null);
            getElement().executeJs("this.inputElement.value = $0;", customText);
            setErrorMessage(result.errorMessage());
            setInvalid(true);
            return;
        }

        pendingCustomText = customText;
        pendingHandlerResponse = new AtomicBoolean(false);

        if (customValueHandler != null) {
            AtomicBoolean guard = pendingHandlerResponse;
            customValueHandler.handle(customText,
                    item -> {
                        if (guard.compareAndSet(false, true)) {
                            onHandlerSubmit(item);
                        }
                    },
                    () -> {
                        if (guard.compareAndSet(false, true)) {
                            onHandlerCancel();
                        }
                    });
        } else {
            fireSubmittedEvent(customText, null);
        }
    }

    private void onHandlerSubmit(T newItem) {
        String text = pendingCustomText;
        pendingCustomText = null;
        if (transientStore != null && newItem != null) {
            transientStore.add(newItem);
        }
        setValue(newItem);
        fireSubmittedEvent(text, newItem);
    }

    private void onHandlerCancel() {
        String text = pendingCustomText;
        pendingCustomText = null;
        revertToPreviousValue();
        fireCancelledEvent(text);
    }

    private void revertToPreviousValue() {
        setValue(previousValue);
    }

    @SuppressWarnings("unchecked")
    private void fireSubmittedEvent(String customText, T item) {
        ComponentUtil.fireEvent(this, new CustomValueSubmittedEvent<>(this, customText, item));
    }

    @SuppressWarnings("unchecked")
    private void fireCancelledEvent(String customText) {
        ComponentUtil.fireEvent(this, new CustomValueCancelledEvent<>(this, customText));
    }

    // --- Configuration ---

    /**
     * Enables or disables acceptance of values not present in the data source.
     * When disabled (default), typed text that doesn't match an item is rejected
     * and the field is marked invalid.
     */
    public void setCustomValuesAllowed(boolean allowed) {
        this.customValuesAllowed = allowed;
        syncTriggersToClient();
    }

    public boolean isCustomValuesAllowed() {
        return customValuesAllowed;
    }

    /**
     * Sets which user actions trigger custom value handling.
     * Defaults to {@link CustomValueTrigger#ENTER} only.
     */
    public void setCustomValueTriggers(Set<CustomValueTrigger> triggers) {
        this.customValueTriggers = triggers != null
                ? EnumSet.copyOf(triggers)
                : EnumSet.noneOf(CustomValueTrigger.class);
        syncTriggersToClient();
    }

    public Set<CustomValueTrigger> getCustomValueTriggers() {
        return EnumSet.copyOf(customValueTriggers);
    }

    /**
     * Sets the handler invoked when the user submits a custom value.
     * If {@code null}, custom values fire {@link CustomValueSubmittedEvent} directly.
     */
    public void setCustomValueHandler(CustomValueHandler<T> handler) {
        this.customValueHandler = handler;
    }

    /**
     * Sets a validator for custom values. Runs before the handler.
     * Defaults to accepting all values.
     */
    public void setCustomValueValidator(CustomValueValidator validator) {
        this.customValueValidator = validator != null ? validator : CustomValueValidator.acceptAll();
    }

    /**
     * Sets a shared transient item store. When a custom value is submitted via
     * the handler, the new item is automatically added to this store. Other
     * combo boxes sharing the same store (via {@link TransientAwareDataProvider})
     * will see the item immediately.
     * <p>
     * Set to {@code null} to disable transient item tracking.
     */
    public void setTransientItemStore(TransientItemStore<T> store) {
        this.transientStore = store;
    }

    public TransientItemStore<T> getTransientItemStore() {
        return transientStore;
    }

    // --- Events ---

    @SuppressWarnings("unchecked")
    public Registration addCustomValueSubmittedListener(
            ComponentEventListener<CustomValueSubmittedEvent<T>> listener) {
        return ComponentUtil.addListener(this,
                (Class<CustomValueSubmittedEvent<T>>) (Class<?>) CustomValueSubmittedEvent.class,
                listener);
    }

    @SuppressWarnings("unchecked")
    public Registration addCustomValueCancelledListener(
            ComponentEventListener<CustomValueCancelledEvent<T>> listener) {
        return ComponentUtil.addListener(this,
                (Class<CustomValueCancelledEvent<T>>) (Class<?>) CustomValueCancelledEvent.class,
                listener);
    }

    // --- Overrides ---

    /**
     * Sets the external validation state. Combined with internal validation
     * on the client: {@code invalid = _invalidInternal || invalidExternal}.
     */
    @Override
    public void setInvalid(boolean invalid) {
        getElement().setProperty("invalidExternal", invalid);
    }

    @Override
    public boolean isInvalid() {
        return getElement().getProperty("invalid", false);
    }

    /**
     * Custom value entry is managed internally by this component.
     * Calling with {@code true} from application code is not supported —
     * use {@link #setCustomValuesAllowed(boolean)} instead.
     *
     * @throws UnsupportedOperationException if called with {@code true} from outside init
     */
    @Override
    public void setAllowCustomValue(boolean allowCustomValue) {
        if (initializing) {
            super.setAllowCustomValue(allowCustomValue);
            return;
        }
        if (allowCustomValue) {
            throw new UnsupportedOperationException(
                    "AutoSelectComboBox manages custom values internally. "
                            + "Use setCustomValuesAllowed(true) instead.");
        }
    }

    // --- Internal ---

    private void syncTriggersToClient() {
        if (customValuesAllowed) {
            String json = customValueTriggers.stream()
                    .map(t -> "\"" + t.name() + "\"")
                    .collect(Collectors.joining(",", "[", "]"));
            getElement().setProperty("_customValueTriggers", json);
        } else {
            getElement().setProperty("_customValueTriggers", "[]");
        }
    }
}

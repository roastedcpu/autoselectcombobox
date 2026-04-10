package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Full-featured showcase combining all AutoSelectComboBox capabilities
 * into a realistic form scenario. Demonstrates replacing an
 * EntitySearchComboBox + "+" button pattern with a single component.
 *
 * <p>Features demonstrated:
 * <ul>
 *   <li>Auto-select on single match</li>
 *   <li>Custom value entry with creation dialog</li>
 *   <li>Custom value validation (before dialog opens)</li>
 *   <li>Configurable triggers (Enter, Tab, Blur)</li>
 *   <li>Binder integration with server-side validation</li>
 *   <li>Transient store — items shared across two "tabs"</li>
 *   <li>Cancel/ESC revert with event notification</li>
 *   <li>Commit/discard lifecycle</li>
 * </ul>
 */
@PageTitle("ARA Specific")
@Menu(order = 7)
@Route("ara-specific")
public class EntitySearchReplacementView extends AbstractDemo {

    private int nextId = 300;
    private TransientItemStore<Identification> transientStore;
    private List<Identification> persisted;

    @Override
    protected void initView() {
        transientStore = new TransientItemStore<>();
        persisted = new ArrayList<>();
        // Seed persisted data
        persisted.add(new Identification(1, "PT123456789", "ADSE", LocalDate.of(2027, 12, 31), true));
        persisted.add(new Identification(2, "SNS987654321", "SNS", LocalDate.of(2026, 6, 30), true));

        addCard("Realistic form with all features",
                fullFormDemo());

        addCard("Lifecycle — commit or discard transient items",
                lifecycleDemo());
    }

    private VerticalLayout fullFormDemo() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        layout.add(new H4("Two form sections sharing a transient store"));
        layout.add(new Span("Create an identification in Section A — it immediately appears in Section B's dropdown. "
                + "Type a number not in the list and press Enter to open the creation dialog. "
                + "Press ESC or Cancel to revert. The '+' button is still available as a shortcut."));
        layout.add(new Hr());

        // Backend DataProvider wrapping persisted data
        DataProvider<Identification, String> backendProvider = DataProvider.fromFilteringCallbacks(
                query -> {
                    String filter = query.getFilter().orElse("");
                    return persisted.stream()
                            .filter(id -> filter.isEmpty() || id.getNumber().toLowerCase().contains(filter.toLowerCase())
                                    || id.getEntity().toLowerCase().contains(filter.toLowerCase()))
                            .skip(query.getOffset())
                            .limit(query.getLimit());
                },
                query -> {
                    String filter = query.getFilter().orElse("");
                    return (int) persisted.stream()
                            .filter(id -> filter.isEmpty() || id.getNumber().toLowerCase().contains(filter.toLowerCase())
                                    || id.getEntity().toLowerCase().contains(filter.toLowerCase()))
                            .count();
                });

        // --- Section A ---
        layout.add(new H4("Section A"));
        layout.add(createFormSection("A", backendProvider));

        layout.add(new Hr());

        // --- Section B ---
        layout.add(new H4("Section B"));
        layout.add(createFormSection("B", backendProvider));

        // --- Transient store status ---
        layout.add(new Hr());
        Span storeStatus = new Span(storeLabel());
        transientStore.addChangeListener(() -> storeStatus.setText(storeLabel()));
        layout.add(storeStatus);

        return layout;
    }

    private VerticalLayout createFormSection(String label, DataProvider<Identification, String> backendProvider) {
        TransientAwareDataProvider<Identification, String> provider = new TransientAwareDataProvider<>(
                backendProvider, transientStore,
                (id, filter) -> id.getNumber().toLowerCase().contains(filter.toLowerCase())
                        || id.getEntity().toLowerCase().contains(filter.toLowerCase()));

        AutoSelectComboBox<Identification> combo = new AutoSelectComboBox<>("Identification (" + label + ")");
        combo.setHelperText("Type to filter. Enter a new number to create. Auto-selects on single match.");
        combo.setItems(provider);
        combo.setItemLabelGenerator(id -> id.getNumber() + " — " + id.getEntity());
        combo.setClearButtonVisible(true);
        combo.setWidth("350px");

        // --- All features enabled ---
        combo.setCustomValuesAllowed(true);
        combo.setCustomValueTriggers(EnumSet.of(CustomValueTrigger.ENTER, CustomValueTrigger.TAB));
        combo.setTransientItemStore(transientStore);

        // Validation: at least 3 characters before opening the dialog
        combo.setCustomValueValidator(text -> {
            if (text == null || text.trim().length() < 3) {
                return CustomValueValidator.Result.error("Number must be at least 3 characters");
            }
            return CustomValueValidator.Result.ok();
        });

        // Handler: opens creation dialog
        combo.setCustomValueHandler((text, onSubmit, onCancel) -> {
            IdentificationDialog dialog = new IdentificationDialog(text, onSubmit, onCancel);
            dialog.open();
        });

        // --- "+" button (alternative entry point) ---
        Button addButton = new Button(VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addButton.getElement().setAttribute("title", "Create new identification");
        addButton.addClickListener(e -> {
            IdentificationDialog dialog = new IdentificationDialog("", id -> {
                transientStore.add(id);
                combo.setValue(id);
            }, () -> {});
            dialog.open();
        });

        HorizontalLayout row = new HorizontalLayout(combo, addButton);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);

        // --- Event log ---
        Span valueSpan = new Span("Value: none");
        combo.addValueChangeListener(e -> {
            Identification v = e.getValue();
            valueSpan.setText(v != null ? "Value: " + v : "Value: none");
        });

        Span eventSpan = new Span("");
        combo.addCustomValueSubmittedListener(e ->
                eventSpan.setText("Created: " + e.getItem()));
        combo.addCustomValueCancelledListener(e ->
                eventSpan.setText("Cancelled: '" + e.getCustomText() + "' — reverted"));

        VerticalLayout section = new VerticalLayout(row, valueSpan, eventSpan);
        section.setPadding(false);
        section.setSpacing(false);
        return section;
    }

    private VerticalLayout lifecycleDemo() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);

        layout.add(new Span("Transient items live in memory until committed (persisted) or discarded. "
                + "This simulates the episode save / cancel flow."));

        Span contents = new Span(storeLabel());
        transientStore.addChangeListener(() -> contents.setText(storeLabel()));

        Button commit = new Button("Save episode (commit)", e -> {
            transientStore.commitAll(items -> {
                persisted.addAll(items);
                Notification.show("Persisted " + items.size() + " identification(s)");
            });
        });
        commit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button discard = new Button("Cancel episode (discard)", e -> {
            transientStore.discardAll();
            Notification.show("Transient items discarded");
        });
        discard.addThemeVariants(ButtonVariant.LUMO_ERROR);

        layout.add(contents, new HorizontalLayout(commit, discard));
        return layout;
    }

    private String storeLabel() {
        if (transientStore.isEmpty()) {
            return "Transient store: empty";
        }
        StringBuilder sb = new StringBuilder("Transient store: ");
        transientStore.getItems().forEach(id -> sb.append(id.getNumber()).append(", "));
        return sb.substring(0, sb.length() - 2);
    }

    // --- Demo model ---

    private class Identification {
        private final int id;
        private String number;
        private String entity;
        private LocalDate validity;
        private boolean active;

        Identification(int id, String number, String entity, LocalDate validity, boolean active) {
            this.id = id;
            this.number = number;
            this.entity = entity;
            this.validity = validity;
            this.active = active;
        }

        public int getId() { return id; }
        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }
        public String getEntity() { return entity; }
        public void setEntity(String entity) { this.entity = entity; }
        public LocalDate getValidity() { return validity; }
        public void setValidity(LocalDate validity) { this.validity = validity; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        @Override
        public String toString() {
            return number + " — " + entity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Identification)) return false;
            return id == ((Identification) o).id;
        }

        @Override
        public int hashCode() { return id; }
    }

    // --- Creation dialog ---

    private class IdentificationDialog extends Dialog {

        IdentificationDialog(String initialNumber, Consumer<Identification> onSubmit, Runnable onCancel) {
            setHeaderTitle("New Identification");
            setCloseOnEsc(true);
            setCloseOnOutsideClick(false);
            setWidth("380px");

            Identification model = new Identification(nextId++, initialNumber, "", null, true);

            TextField number = new TextField("Number");
            number.setWidthFull();
            number.setAutofocus(true);

            TextField entity = new TextField("Entity");
            entity.setWidthFull();

            DatePicker validity = new DatePicker("Validity");
            validity.setWidthFull();

            Checkbox active = new Checkbox("Active");
            active.setValue(true);

            Binder<Identification> binder = new Binder<>();
            binder.forField(number)
                    .asRequired("Number is required")
                    .bind(Identification::getNumber, Identification::setNumber);
            binder.forField(entity)
                    .asRequired("Entity is required")
                    .bind(Identification::getEntity, Identification::setEntity);
            binder.forField(validity)
                    .bind(Identification::getValidity, Identification::setValidity);
            binder.forField(active)
                    .bind(Identification::isActive, Identification::setActive);
            binder.setBean(model);

            FormLayout form = new FormLayout(number, entity, validity, active);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
            add(form);

            Button save = new Button("Save", VaadinIcon.CHECK.create(), e -> {
                if (binder.validate().isOk()) {
                    onSubmit.accept(model);
                    close();
                }
            });
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            Button cancel = new Button("Cancel", e -> {
                onCancel.run();
                close();
            });

            getFooter().add(cancel, save);

            addDialogCloseActionListener(e -> {
                onCancel.run();
                close();
            });
        }
    }
}

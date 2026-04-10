package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.EnumSet;

@PageTitle("Transient Store")
@Menu(order = 4)
@Route("transient-store")
public class TransientStoreView extends AbstractDemo {

    private int nextId = 200;
    private final TransientItemStore<Person> sharedStore = new TransientItemStore<>();

    @Override
    protected void initView() {
        addCard("Shared transient store across multiple combo boxes",
                sharedStoreDemo());

        addCard("Commit / Discard transient items",
                commitDiscardDemo());
    }

    /**
     * Two combo boxes share the same TransientItemStore. Creating a person
     * in one makes it immediately available in the other.
     */
    private VerticalLayout sharedStoreDemo() {
        PersonService service = new PersonService(3);

        DataProvider<Person, String> backendProvider = DataProvider.fromFilteringCallbacks(
                query -> service.fetch(query.getOffset(), query.getLimit(), query.getFilter().orElse(null)).stream(),
                query -> service.count(query.getFilter().orElse(null)));

        TransientAwareDataProvider<Person, String> provider1 = new TransientAwareDataProvider<>(
                backendProvider, sharedStore,
                (person, filter) -> person.toString().toLowerCase().contains(filter.toLowerCase()));

        TransientAwareDataProvider<Person, String> provider2 = new TransientAwareDataProvider<>(
                backendProvider, sharedStore,
                (person, filter) -> person.toString().toLowerCase().contains(filter.toLowerCase()));

        // --- Combo 1: "Requisition #1" ---
        AutoSelectComboBox<Person> combo1 = new AutoSelectComboBox<>("Requisition #1 — Identification");
        combo1.setHelperText("Type a new name and press Enter to create it. It appears in Req #2 immediately.");
        combo1.setItems(provider1);
        combo1.setItemLabelGenerator(Person::toString);
        combo1.setClearButtonVisible(true);
        combo1.setCustomValuesAllowed(true);
        combo1.setTransientItemStore(sharedStore);
        combo1.setCustomValueHandler((text, onSubmit, onCancel) -> {
            String[] parts = text.trim().split("\\s+", 2);
            Person person = new Person(nextId++, parts[0],
                    parts.length > 1 ? parts[1] : "", 0, null, "");
            onSubmit.accept(person);
        });

        Span status1 = new Span("Value: none");
        combo1.addValueChangeListener(e ->
                status1.setText("Value: " + (e.getValue() != null ? e.getValue() : "none")));

        // --- Combo 2: "Requisition #2" ---
        AutoSelectComboBox<Person> combo2 = new AutoSelectComboBox<>("Requisition #2 — Identification");
        combo2.setHelperText("Shares the same transient store. Items created in Req #1 appear here.");
        combo2.setItems(provider2);
        combo2.setItemLabelGenerator(Person::toString);
        combo2.setClearButtonVisible(true);
        combo2.setCustomValuesAllowed(true);
        combo2.setTransientItemStore(sharedStore);
        combo2.setCustomValueHandler((text, onSubmit, onCancel) -> {
            String[] parts = text.trim().split("\\s+", 2);
            Person person = new Person(nextId++, parts[0],
                    parts.length > 1 ? parts[1] : "", 0, null, "");
            onSubmit.accept(person);
        });

        Span status2 = new Span("Value: none");
        combo2.addValueChangeListener(e ->
                status2.setText("Value: " + (e.getValue() != null ? e.getValue() : "none")));

        Span storeStatus = new Span("Transient items: 0");
        sharedStore.addChangeListener(() ->
                storeStatus.setText("Transient items: " + sharedStore.size()));

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);

        layout.add(new H4("Simulates two requisition tabs sharing a transient store"));
        layout.add(combo1, status1);
        layout.add(new Hr());
        layout.add(combo2, status2);
        layout.add(new Hr());
        layout.add(storeStatus);

        return layout;
    }

    /**
     * Demonstrates commit (persist) and discard (clear) of transient items.
     */
    private VerticalLayout commitDiscardDemo() {
        Span storeContents = new Span("Store: empty");

        Runnable updateLabel = () -> {
            if (sharedStore.isEmpty()) {
                storeContents.setText("Store: empty");
            } else {
                StringBuilder sb = new StringBuilder("Store: ");
                sharedStore.getItems().forEach(p -> sb.append(p.toString()).append(", "));
                storeContents.setText(sb.toString());
            }
        };

        sharedStore.addChangeListener(updateLabel::run);

        Button commitButton = new Button("Commit (simulate save)", e -> {
            sharedStore.commitAll(items ->
                    Notification.show("Persisted " + items.size() + " item(s): " + items));
            updateLabel.run();
        });
        commitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button discardButton = new Button("Discard all", e -> {
            sharedStore.discardAll();
            Notification.show("All transient items discarded");
            updateLabel.run();
        });
        discardButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout buttons = new HorizontalLayout(commitButton, discardButton);

        VerticalLayout layout = new VerticalLayout(storeContents, buttons);
        layout.setPadding(false);
        return layout;
    }
}

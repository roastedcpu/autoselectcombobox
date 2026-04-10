package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Full showcase of every AutoSelectComboBox feature in a single page:
 * <ul>
 *   <li>Auto-select on single match</li>
 *   <li>Custom value entry with creation dialog (form)</li>
 *   <li>Custom value validation (before dialog opens)</li>
 *   <li>Configurable triggers (Enter + Tab)</li>
 *   <li>Binder integration</li>
 *   <li>Transient store — shared across two combo boxes</li>
 *   <li>Cancel/ESC revert with event notification</li>
 *   <li>"+" button as alternative entry point</li>
 * </ul>
 */
@PageTitle("Full Showcase")
@Menu(order = 6)
@Route("full-showcase")
public class FullShowcaseView extends AbstractDemo {

    private int nextId;
    private TransientItemStore<Person> transientStore;
    private List<Person> persisted;

    @Override
    protected void initView() {
        nextId = 100;
        transientStore = new TransientItemStore<>();
        persisted = new ArrayList<>();
        persisted.add(new Person(1, "Aaron", "Allen", 28, null, "555-0101"));
        persisted.add(new Person(2, "Benjamin", "Brick", 35, null, "555-0102"));
        persisted.add(new Person(3, "Catherine", "Cole", 42, null, "555-0103"));

        addCard("All features combined", allFeaturesDemo());
        addCard("Transient store status", storeStatusDemo());
    }

    private VerticalLayout allFeaturesDemo() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        layout.add(new H4("Two combo boxes sharing a transient store"));
        layout.add(new Span("Type a name not in the list and press Enter or Tab — a creation dialog opens. "
                + "Press ESC or Cancel to revert. Items created in Combo A appear instantly in Combo B. "
                + "The '+' button opens the same dialog without typing first."));
        layout.add(new Hr());

        DataProvider<Person, String> backendProvider = DataProvider.fromFilteringCallbacks(
                query -> {
                    String filter = query.getFilter().orElse("");
                    return persisted.stream()
                            .filter(p -> filter.isEmpty() || p.toString().toLowerCase().contains(filter.toLowerCase()))
                            .skip(query.getOffset())
                            .limit(query.getLimit());
                },
                query -> {
                    String filter = query.getFilter().orElse("");
                    return (int) persisted.stream()
                            .filter(p -> filter.isEmpty() || p.toString().toLowerCase().contains(filter.toLowerCase()))
                            .count();
                });

        layout.add(new H4("Combo A"));
        layout.add(createComboSection("A", backendProvider));
        layout.add(new Hr());
        layout.add(new H4("Combo B"));
        layout.add(createComboSection("B", backendProvider));

        return layout;
    }

    private VerticalLayout createComboSection(String label, DataProvider<Person, String> backendProvider) {
        TransientAwareDataProvider<Person, String> provider = new TransientAwareDataProvider<>(
                backendProvider, transientStore,
                (person, filter) -> person.toString().toLowerCase().contains(filter.toLowerCase()));

        AutoSelectComboBox<Person> combo = new AutoSelectComboBox<>("Person (" + label + ")");
        combo.setHelperText("Auto-selects on single match. Enter/Tab triggers custom value flow.");
        combo.setItems(provider);
        combo.setItemLabelGenerator(Person::toString);
        combo.setClearButtonVisible(true);
        combo.setWidth("350px");

        // Enable all features
        combo.setCustomValuesAllowed(true);
        combo.setCustomValueTriggers(EnumSet.of(CustomValueTrigger.ENTER, CustomValueTrigger.TAB));
        combo.setTransientItemStore(transientStore);

        // Validation: at least 2 characters
        combo.setCustomValueValidator(text -> {
            if (text == null || text.trim().length() < 2) {
                return CustomValueValidator.Result.error("Name must be at least 2 characters");
            }
            return CustomValueValidator.Result.ok();
        });

        // Handler: opens creation form
        combo.setCustomValueHandler((text, onSubmit, onCancel) ->
                openPersonDialog(text, onSubmit, onCancel));

        // "+" button
        Button addButton = new Button(VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        addButton.getElement().setAttribute("title", "Create new person");
        addButton.addClickListener(e ->
                openPersonDialog("", person -> combo.setValue(person), () -> {}));

        HorizontalLayout row = new HorizontalLayout(combo, addButton);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);

        // Status
        Span valueSpan = new Span("Value: none");
        combo.addValueChangeListener(e -> {
            Person v = e.getValue();
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

    private VerticalLayout storeStatusDemo() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);

        Span contents = new Span(storeLabel());
        transientStore.addChangeListener(() -> contents.setText(storeLabel()));

        Button commit = new Button("Commit (simulate save)", e -> {
            transientStore.commitAll(items -> {
                persisted.addAll(items);
                Notification.show("Persisted " + items.size() + " person(s)");
            });
            contents.setText(storeLabel());
        });
        commit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button discard = new Button("Discard all", e -> {
            transientStore.discardAll();
            Notification.show("Transient items discarded");
            contents.setText(storeLabel());
        });
        discard.addThemeVariants(ButtonVariant.LUMO_ERROR);

        layout.add(contents, new HorizontalLayout(commit, discard));
        return layout;
    }

    private void openPersonDialog(String prefillText, Consumer<Person> onSubmit, Runnable onCancel) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create new person");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("380px");

        String[] parts = prefillText.trim().split("\\s+", 2);
        Person model = new Person(nextId++, parts[0], parts.length > 1 ? parts[1] : "", 0, null, "");

        TextField firstName = new TextField("First name");
        firstName.setWidthFull();
        firstName.setAutofocus(true);

        TextField lastName = new TextField("Last name");
        lastName.setWidthFull();

        IntegerField age = new IntegerField("Age");
        age.setWidthFull();

        TextField phone = new TextField("Phone");
        phone.setWidthFull();

        Binder<Person> binder = new Binder<>();
        binder.forField(firstName).asRequired("First name is required")
                .bind(Person::getFirstName, Person::setFirstName);
        binder.forField(lastName).asRequired("Last name is required")
                .bind(Person::getLastName, Person::setLastName);
        binder.forField(age).bind(Person::getAge, Person::setAge);
        binder.forField(phone).bind(Person::getPhoneNumber, Person::setPhoneNumber);
        binder.setBean(model);

        FormLayout form = new FormLayout(firstName, lastName, age, phone);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        dialog.add(form);

        Button save = new Button("Create", VaadinIcon.CHECK.create(), e -> {
            if (binder.validate().isOk()) {
                onSubmit.accept(model);
                dialog.close();
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> {
            onCancel.run();
            dialog.close();
        });

        dialog.getFooter().add(cancel, save);
        dialog.addDialogCloseActionListener(e -> {
            onCancel.run();
            dialog.close();
        });

        dialog.open();
    }

    private String storeLabel() {
        if (transientStore.isEmpty()) {
            return "Transient store: empty";
        }
        StringBuilder sb = new StringBuilder("Transient store: ");
        transientStore.getItems().forEach(p -> sb.append(p).append(", "));
        return sb.substring(0, sb.length() - 2);
    }
}

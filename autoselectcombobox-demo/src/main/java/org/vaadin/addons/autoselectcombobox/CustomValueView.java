package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.EnumSet;
import java.util.function.Consumer;

@PageTitle("Custom Values")
@Menu(order = 3)
@Route("custom-values")
public class CustomValueView extends AbstractDemo {

    private int nextId = 100;

    @Override
    protected void initView() {
        addCard("Custom value — accept directly (no handler)",
                directAccept());

        addCard("Custom value — creation dialog",
                withCreationDialog());

        addCard("Custom value — with validation",
                withValidation());

        addCard("Custom value — trigger on Tab + Blur",
                withTabAndBlurTriggers());
    }

    /**
     * Simplest case: custom values are accepted as-is.
     * The submitted event carries the typed text with no item.
     */
    private VerticalLayout directAccept() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("Type anything");
        combo.setHelperText("Type a value not in the list and press Enter. It fires a submitted event.");
        combo.setItems("Alpha", "Beta", "Gamma");
        combo.setClearButtonVisible(true);
        combo.setCustomValuesAllowed(true);

        Span log = new Span("Events: none");
        combo.addCustomValueSubmittedListener(e ->
                log.setText("Submitted: '" + e.getCustomText() + "' (item: " + e.getItem() + ")"));
        combo.addCustomValueCancelledListener(e ->
                log.setText("Cancelled: '" + e.getCustomText() + "'"));

        VerticalLayout layout = new VerticalLayout(combo, log);
        layout.setPadding(false);
        return layout;
    }

    /**
     * Custom value triggers a creation dialog. On submit, the new Person
     * is set as the combo box value. On cancel, the value reverts.
     */
    private VerticalLayout withCreationDialog() {
        PersonService service = new PersonService(5);

        AutoSelectComboBox<Person> combo = new AutoSelectComboBox<>("People (with creation dialog)");
        combo.setHelperText("Type a name not in the list. A dialog opens to complete the creation.");
        combo.setItems(service.fetchAll());
        combo.setItemLabelGenerator(Person::toString);
        combo.setClearButtonVisible(true);
        combo.setCustomValuesAllowed(true);

        combo.setCustomValueHandler((text, onSubmit, onCancel) -> {
            PersonCreationDialog dialog = new PersonCreationDialog(text, person -> {
                service.addPerson();
                onSubmit.accept(person);
            }, onCancel);
            dialog.open();
        });

        Span status = new Span("Value: none");
        combo.addValueChangeListener(e ->
                status.setText("Value: " + (e.getValue() != null ? e.getValue() : "none")));

        Span events = new Span("Events: none");
        combo.addCustomValueSubmittedListener(e ->
                events.setText("Submitted: '" + e.getCustomText() + "' → " + e.getItem()));
        combo.addCustomValueCancelledListener(e ->
                events.setText("Cancelled: '" + e.getCustomText() + "' (reverted)"));

        VerticalLayout layout = new VerticalLayout(combo, status, events);
        layout.setPadding(false);
        return layout;
    }

    /**
     * Custom value with validation: text must be at least 3 characters.
     */
    private VerticalLayout withValidation() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("With custom value validation");
        combo.setHelperText("Custom values must be at least 3 characters. Try typing 'ab' and pressing Enter.");
        combo.setItems("Alpha", "Beta", "Gamma");
        combo.setClearButtonVisible(true);
        combo.setCustomValuesAllowed(true);
        combo.setCustomValueValidator(text -> {
            if (text == null || text.trim().length() < 3) {
                return CustomValueValidator.Result.error("Must be at least 3 characters");
            }
            return CustomValueValidator.Result.ok();
        });

        Span log = new Span("Events: none");
        combo.addCustomValueSubmittedListener(e ->
                log.setText("Accepted: '" + e.getCustomText() + "'"));

        VerticalLayout layout = new VerticalLayout(combo, log);
        layout.setPadding(false);
        return layout;
    }

    /**
     * Custom value triggered by Tab and Blur (not just Enter).
     */
    private VerticalLayout withTabAndBlurTriggers() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("Tab + Blur triggers");
        combo.setHelperText("Type a custom value, then Tab or click outside. All three triggers are active.");
        combo.setItems("Red", "Green", "Blue");
        combo.setClearButtonVisible(true);
        combo.setCustomValuesAllowed(true);
        combo.setCustomValueTriggers(EnumSet.of(
                CustomValueTrigger.ENTER,
                CustomValueTrigger.TAB,
                CustomValueTrigger.BLUR));

        Span log = new Span("Events: none");
        combo.addCustomValueSubmittedListener(e ->
                log.setText("Submitted via trigger: '" + e.getCustomText() + "'"));

        TextField focusTarget = new TextField("Tab target");
        focusTarget.setHelperText("Tab here from the combo box above to test Tab trigger.");

        VerticalLayout layout = new VerticalLayout(combo, focusTarget, log);
        layout.setPadding(false);
        return layout;
    }

    /**
     * Simple inline dialog for creating a Person from a typed name.
     */
    private class PersonCreationDialog extends Dialog {

        PersonCreationDialog(String initialText, Consumer<Person> onSubmit, Runnable onCancel) {
            setHeaderTitle("Create new person");
            setCloseOnEsc(true);
            setCloseOnOutsideClick(false);

            TextField firstName = new TextField("First name");
            TextField lastName = new TextField("Last name");

            // Pre-fill from typed text
            String[] parts = initialText.trim().split("\\s+", 2);
            firstName.setValue(parts[0]);
            if (parts.length > 1) {
                lastName.setValue(parts[1]);
            }

            VerticalLayout content = new VerticalLayout(firstName, lastName);
            content.setPadding(false);
            add(content);

            Button save = new Button("Create", e -> {
                Person person = new Person(nextId++,
                        firstName.getValue(), lastName.getValue(),
                        0, null, "");
                onSubmit.accept(person);
                close();
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

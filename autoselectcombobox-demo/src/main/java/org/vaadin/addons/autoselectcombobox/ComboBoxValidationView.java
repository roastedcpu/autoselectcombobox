package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Auto-Select Basics")
@Menu(order = 1)
@Route("")
@Uses(Icon.class)
public class ComboBoxValidationView extends AbstractDemo {

    private PersonService personService;

    @Override
    protected void initView() {
        personService = new PersonService(1);

        DataProvider<Person, String> dataProvider = DataProvider.fromFilteringCallbacks(
                query -> personService.fetch(query.getOffset(), query.getLimit(), query.getFilter().orElse(null)).stream(),
                query -> personService.count(query.getFilter().orElse(null)));

        addCard("Standard ComboBox (comparison)",
                regularComboBox(dataProvider));

        addCard("Auto-select with DataProvider",
                autoSelectWithDataProvider(dataProvider));

        addCard("Auto-select with fixed items",
                autoSelectWithFixedItems());

        addCard("Read-only",
                readOnlyComboBox());

        addCard("Autofocus with single item",
                autoFocusComboBox());

        addCard("Binder with validation",
                binderValidation());

        add(new Anchor("#", "Focus target for testing"));
    }

    private ComboBox<Person> regularComboBox(DataProvider<Person, String> dataProvider) {
        ComboBox<Person> combo = new ComboBox<>("Regular ComboBox");
        combo.setHelperText("Standard ComboBox for comparison. Clears input on blur if no match.");
        combo.setItems(dataProvider);
        combo.setItemLabelGenerator(Person::toString);
        combo.setClearButtonVisible(true);
        return combo;
    }

    private AutoSelectComboBox<Person> autoSelectWithDataProvider(DataProvider<Person, String> dataProvider) {
        AutoSelectComboBox<Person> combo = new AutoSelectComboBox<>("AutoSelectComboBox with DataProvider");
        combo.setHelperText("Type a partial name. If exactly one match remains, it auto-selects on blur/Enter.");
        combo.setItems(dataProvider);
        combo.setItemLabelGenerator(Person::toString);
        combo.setClearButtonVisible(true);

        Span status = new Span("Value: none");
        combo.addValueChangeListener(e ->
                status.setText("Value: " + (e.getValue() != null ? e.getValue() : "none")));

        VerticalLayout layout = new VerticalLayout(combo, status);
        layout.setPadding(false);
        return combo;
    }

    private AutoSelectComboBox<String> autoSelectWithFixedItems() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("AutoSelectComboBox with fixed items");
        combo.setHelperText("Fixed list: Bar, Foo, Baz, Quizzle, Quux. Type 'Q' to see auto-select.");
        combo.setItems("Bar", "Foo", "Baz", "Quizzle", "Quux");
        combo.setClearButtonVisible(true);

        Span status = new Span("Value: none");
        combo.addValueChangeListener(e ->
                status.setText("Value: " + (e.getValue() != null ? e.getValue() : "none")));

        VerticalLayout layout = new VerticalLayout(combo, status);
        layout.setPadding(false);
        return combo;
    }

    private AutoSelectComboBox<String> readOnlyComboBox() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("Read-only");
        combo.setHelperText("Pre-set to 'Bar', not editable.");
        combo.setItems("Foo", "Bar", "Baz");
        combo.setValue("Bar");
        combo.setReadOnly(true);
        combo.setClearButtonVisible(true);
        return combo;
    }

    private AutoSelectComboBox<String> autoFocusComboBox() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("Autofocus with 1 item");
        combo.setHelperText("Autofocus on page load with a single pre-selected item.");
        combo.setAutofocus(true);
        combo.setItems("Foo");
        combo.setValue("Foo");
        return combo;
    }

    private AutoSelectComboBox<String> binderValidation() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("With Binder validation");
        combo.setHelperText("Required field. 'Foo' is rejected by a custom validator.");
        combo.setItems("Bar", "Foo", "Baz");
        combo.setValue("Bar");

        Binder<TestBean> binder = new Binder<>();
        binder.forField(combo)
                .asRequired("Selection is required")
                .withValidator((value, ctx) ->
                        "Foo".equals(value)
                                ? ValidationResult.error("'Foo' is not allowed")
                                : ValidationResult.ok())
                .bind(TestBean::getName, TestBean::setName);

        binder.setBean(new TestBean());
        return combo;
    }
}

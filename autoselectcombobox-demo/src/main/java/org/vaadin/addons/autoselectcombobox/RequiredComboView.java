package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBox.ComboBoxI18n;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Required Field")
@Menu(order = 2)
@Route("required")
public class RequiredComboView extends AbstractDemo {

    @Override
    protected void initView() {
        addCard("Required — AutoSelectComboBox vs standard ComboBox",
                requiredAutoSelect(),
                requiredAutoSelectWithInitialValue(),
                requiredStandardComboBox(),
                requiredStandardWithInitialValue(),
                new Anchor("#", "Focus target for testing"));
    }

    private Component requiredAutoSelect() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("AutoSelect — Required (no initial value)");
        combo.setHelperText("Leave empty and blur to see the required error.");
        combo.setItems("Bar", "Foo", "Baz", "Quizzle", "Quux");
        combo.setRequired(true);
        combo.setI18n(new ComboBoxI18n().setRequiredErrorMessage("This field is required"));

        Span value = new Span("Value: none");
        combo.addValueChangeListener(e -> value.setText("Value: " + e.getValue()));

        VerticalLayout vl = new VerticalLayout(combo, value);
        vl.setPadding(false);
        return vl;
    }

    private Component requiredAutoSelectWithInitialValue() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("AutoSelect — Required (initial: Foo)");
        combo.setHelperText("Clear the value and blur to trigger required validation.");
        combo.setItems("Bar", "Foo", "Baz", "Quizzle", "Quux");
        combo.setRequired(true);
        combo.setValue("Foo");
        combo.setI18n(new ComboBoxI18n().setRequiredErrorMessage("This field is required"));

        Span value = new Span("Value: Foo");
        combo.addValueChangeListener(e -> value.setText("Value: " + e.getValue()));

        VerticalLayout vl = new VerticalLayout(combo, value);
        vl.setPadding(false);
        return vl;
    }

    private ComboBox<String> requiredStandardComboBox() {
        ComboBox<String> combo = new ComboBox<>("Standard — Required (no initial value)");
        combo.setHelperText("Standard ComboBox for comparison.");
        combo.setItems("Bar", "Foo", "Baz", "Quizzle", "Quux");
        combo.setRequired(true);
        combo.setI18n(new ComboBoxI18n().setRequiredErrorMessage("This field is required"));
        return combo;
    }

    private ComboBox<String> requiredStandardWithInitialValue() {
        ComboBox<String> combo = new ComboBox<>("Standard — Required (initial: Foo)");
        combo.setHelperText("Standard ComboBox for comparison.");
        combo.setItems("Bar", "Foo", "Baz", "Quizzle", "Quux");
        combo.setRequired(true);
        combo.setValue("Foo");
        combo.setI18n(new ComboBoxI18n().setRequiredErrorMessage("This field is required"));
        return combo;
    }
}

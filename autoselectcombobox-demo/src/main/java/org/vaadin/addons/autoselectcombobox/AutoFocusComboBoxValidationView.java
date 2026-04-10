package org.vaadin.addons.autoselectcombobox;

import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Auto Focus")
@Menu(order = 5)
@Route("auto-focus")
@Uses(Icon.class)
public class AutoFocusComboBoxValidationView extends AbstractDemo {

    @Override
    protected void initView() {
        AutoSelectComboBox<String> combo = new AutoSelectComboBox<>("Autofocus on load");
        combo.setHelperText("This combo box receives focus automatically when the page loads.");
        combo.setItems("Test");
        combo.setValue("Test");
        combo.setAutofocus(true);

        addCard("Autofocus behavior", combo,
                new Anchor("#", "Focus target for testing"));
    }
}

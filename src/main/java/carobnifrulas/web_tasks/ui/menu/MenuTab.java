package carobnifrulas.web_tasks.ui.menu;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.dom.DomEventListener;

public interface MenuTab {

    String getTabName();
    VaadinIcon getTabIcon();
    DomEventListener onTabClick();

    default Tab createTab() {
        Icon icon = getTabIcon() != null ? getTabIcon().create() : null;

        com.vaadin.flow.component.html.Span text =
                new com.vaadin.flow.component.html.Span(getTabName());

        Tab tab = (icon == null) ? new Tab(text) : new Tab(icon, text);

        tab.getElement().addEventListener("click", onTabClick());
        return tab;
    }
}
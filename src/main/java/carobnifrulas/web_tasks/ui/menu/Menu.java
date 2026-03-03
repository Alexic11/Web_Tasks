package carobnifrulas.web_tasks.ui.menu;

import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.views.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Menu {

    private final ObjectProvider<DashboardView> dashboardViewProvider;

    public Menu(ObjectProvider<DashboardView> dashboardViewProvider) {
        this.dashboardViewProvider = dashboardViewProvider;
    }

    public View getDefaultView() {
        return new BoardsView();
    }

    public Tabs getVerticalTabs() {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setWidthFull();

        List<MenuTab> items = new ArrayList<>();

        items.add(simple("Boards", VaadinIcon.DASHBOARD, e -> MainView.getMainView().setContent(new BoardsView())));
        items.add(simple("My Tasks", VaadinIcon.TASKS, e -> MainView.getMainView().setContent(new MyTasksView())));

        // ✅ Dashboard vide svi (admin vidi sve, user vidi samo OWNER — to rješava DashboardView)
        items.add(dashboardViewProvider.getObject());

        items.add(simple("History", VaadinIcon.ARCHIVE, e -> MainView.getMainView().setContent(new ArchivedBoardsView())));

        var mv = MainView.getMainView();
        var user = (mv == null) ? null : mv.getLoggedUser();

        // admin-only (MVP)
        if (user != null && "admin@local".equalsIgnoreCase(user.getEmail())) {
            items.add(simple("Admin", VaadinIcon.TOOLS,
                    e -> MainView.getMainView().setContent(new AdminUsersView())));
        }

        items.forEach(it -> tabs.add(it.createTab()));
        return tabs;
    }

    private MenuTab simple(String name, VaadinIcon icon, DomEventListener click) {
        return new MenuTab() {
            @Override public String getTabName() { return name; }
            @Override public VaadinIcon getTabIcon() { return icon; }
            @Override public DomEventListener onTabClick() { return click; }
        };
    }
}
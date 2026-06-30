package carobnifrulas.web_tasks.ui.menu;

import carobnifrulas.web_tasks.security.model.SecurityUtils;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.views.AdminUsersView;
import carobnifrulas.web_tasks.ui.views.ArchivedBoardsView;
import carobnifrulas.web_tasks.ui.views.BoardsView;
import carobnifrulas.web_tasks.ui.views.DashboardView;
import carobnifrulas.web_tasks.ui.views.MyTasksView;
import carobnifrulas.web_tasks.ui.views.View;
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
        tabs.addClassName("app-menu-tabs");

        List<MenuTab> items = new ArrayList<>();

        items.add(tab("Boards", VaadinIcon.DASHBOARD,
                e -> open(new BoardsView())));

        items.add(tab("My Tasks", VaadinIcon.TASKS,
                e -> open(new MyTasksView())));

        items.add(dashboardViewProvider.getObject());

        items.add(tab("History", VaadinIcon.ARCHIVE,
                e -> open(new ArchivedBoardsView())));

        if (isAdmin()) {
            items.add(tab("Admin", VaadinIcon.TOOLS,
                    e -> open(new AdminUsersView())));
        }

        items.forEach(it -> tabs.add(it.createTab()));
        return tabs;
    }

    private void open(View view) {
        MainView mv = MainView.getMainView();
        if (mv != null) {
            mv.setContent(view);
        }
    }

    private boolean isAdmin() {
        MainView mv = MainView.getMainView();
        if (mv == null || mv.getLoggedUser() == null) {
            return false;
        }

        return SecurityUtils.isGlobalAdmin(mv.getLoggedUser());
    }

    private MenuTab tab(String name, VaadinIcon icon, DomEventListener click) {
        return new MenuTab() {
            @Override
            public String getTabName() {
                return name;
            }

            @Override
            public VaadinIcon getTabIcon() {
                return icon;
            }

            @Override
            public DomEventListener onTabClick() {
                return click;
            }
        };
    }
}
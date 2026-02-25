package carobnifrulas.web_tasks.ui.menu;

import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.views.*;
import com.vaadin.flow.component.tabs.Tabs;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Menu {

    private final BoardsView boardsView;
    private final MyTasksView myTasksView;
    private final AdminUsersView adminUsersView;

    public Menu(BoardsView boardsView, MyTasksView myTasksView, AdminUsersView adminUsersView) {
        this.boardsView = boardsView;
        this.myTasksView = myTasksView;
        this.adminUsersView = adminUsersView;
    }

    public View getDefaultView() {
        return boardsView;
    }

    public Tabs getVerticalTabs() {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.setWidthFull();

        List<MenuTab> items = new ArrayList<>();
        items.add(boardsView);
        items.add(myTasksView);

        // admin-only tab (MVP uslov)
        var mv = MainView.getMainView();
        if (mv != null && mv.getLoggedUser() != null) {
            if ("admin@local".equalsIgnoreCase(mv.getLoggedUser().getEmail())) {
                items.add(adminUsersView);
            }
        }

        items.forEach(it -> tabs.add(it.createTab()));
        return tabs;
    }
}
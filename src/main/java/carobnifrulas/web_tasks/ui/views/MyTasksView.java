package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MyTasksView extends View implements MenuTab {

    private final Grid<Card> grid = new Grid<>(Card.class, false);

    @Override
    public void setElements() {
        add(new com.vaadin.flow.component.html.H2("My Tasks"));

        grid.setWidthFull();
        grid.addColumn(Card::getTitle).setHeader("Task").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(c -> c.getDueAt() == null ? "" : c.getDueAt().toString()).setHeader("Rok");
        grid.addColumn(c -> c.getBoardId()).setHeader("Board ID");

        List<Card> cards = services.cardService.findAssignedTo(loggedUser.getId());
        grid.setItems(cards);
        grid.setAllRowsVisible(true);

        add(grid);
    }

    // MenuTab
    @Override public String getTabName() { return "My Tasks"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.TASKS; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}
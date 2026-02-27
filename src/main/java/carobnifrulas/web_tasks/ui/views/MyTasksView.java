package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MyTasksView extends View implements MenuTab {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Grid<CardRepository.MyTaskRow> grid =
            new Grid<>(CardRepository.MyTaskRow.class, false);

    @Override
    public void setElements() {
        add(new com.vaadin.flow.component.html.H2("My Tasks"));

        grid.setWidthFull();

        grid.addColumn(CardRepository.MyTaskRow::getTitle)
                .setHeader("Task")
                .setAutoWidth(true)
                .setFlexGrow(2);

        grid.addColumn(r -> {
                    Integer p = r.getPriority();
                    if (p == null) p = 1;
                    return "P" + p;
                })
                .setHeader("Prioritet")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> r.getBoardName() + " / " + r.getListTitle())
                .setHeader("Board / List")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(r -> r.getDueAt() == null ? "—" : DT_FMT.format(r.getDueAt()))
                .setHeader("Rok")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addItemClickListener(ev -> {
            try {
                Long cardId = ev.getItem().getCardId();
                Card c = services.cardService.requireById(cardId);
                TaskDialog.edit(services, c, loggedUser.getId()).open();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        refresh();
        grid.setAllRowsVisible(true);

        add(grid);
    }

    private void refresh() {
        List<CardRepository.MyTaskRow> rows = services.cardService.findMyTasks(loggedUser.getId());
        grid.setItems(rows);
    }

    // MenuTab
    @Override public String getTabName() { return "My Tasks"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.TASKS; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}

package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class MyTasksView extends View implements MenuTab {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Grid<CardRepository.MyTaskRow> grid =
            new Grid<>(CardRepository.MyTaskRow.class, false);

    @Override
    public void setElements() {
        add(new H2("My Tasks"));

        grid.setWidthFull();

        grid.addColumn(CardRepository.MyTaskRow::getTitle)
                .setHeader("Task")
                .setAutoWidth(true)
                .setFlexGrow(2);

        // ✅ Prioritet badge
        grid.addComponentColumn(r -> buildPriorityBadge(r.getPriority()))
                .setHeader("Prioritet")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> r.getBoardName() + " / " + r.getListTitle())
                .setHeader("Board / List")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // ✅ Rok sa overdue styling
        grid.addComponentColumn(r -> buildDueLabel(r.getDueAt()))
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

    private Span buildPriorityBadge(Integer p) {
        int pr = (p == null) ? 1 : p;

        Span s = new Span("P" + pr);
        s.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "2px 10px")
                .set("border-radius", "999px")
                .set("font-weight", "600")
                .set("border", "1px solid var(--lumo-contrast-20pct)");

        switch (pr) {
            case 5 -> s.getStyle()
                    .set("background", "var(--lumo-error-color-10pct)")
                    .set("color", "var(--lumo-error-text-color)");
            case 4 -> s.getStyle()
                    .set("background", "var(--lumo-warning-color-10pct)")
                    .set("color", "var(--lumo-warning-text-color)");
            case 3 -> s.getStyle()
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("color", "var(--lumo-primary-text-color)");
            case 2 -> s.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("color", "var(--lumo-body-text-color)");
            default -> s.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");
        }

        return s;
    }

    private Span buildDueLabel(LocalDateTime dueAt) {
        String txt = (dueAt == null) ? "—" : DT_FMT.format(dueAt);

        Span s = new Span(txt);
        s.getStyle().set("font-size", "var(--lumo-font-size-s)");

        if (dueAt != null && dueAt.isBefore(LocalDateTime.now())) {
            s.getStyle()
                    .set("color", "var(--lumo-error-text-color)")
                    .set("font-weight", "600");
        } else {
            s.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");
        }

        return s;
    }

    // MenuTab
    @Override public String getTabName() { return "My Tasks"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.TASKS; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}

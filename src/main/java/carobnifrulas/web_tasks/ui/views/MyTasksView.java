package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.DomEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MyTasksView extends View implements MenuTab {

    @Override
    public void setElements() {
        add(new H2("My Tasks"));

        List<CardRepository.MyTaskRow> all = services.cardService.findMyTasks(loggedUser.getId());

        // ✅ Izbaci one koji su u DONE listi (zadnja lista na boardu)
        // pošto taskovi mogu biti sa više boardova, napravimo mapu boardId -> lastListId
        Map<Long, Long> boardLastListId = all.stream()
                .map(CardRepository.MyTaskRow::getBoardId)
                .distinct()
                .collect(Collectors.toMap(
                        bId -> bId,
                        bId -> services.listService.requireLastListId(bId)
                ));

        List<CardRepository.MyTaskRow> active = all.stream()
                .filter(r -> {
                    Long lastId = boardLastListId.get(r.getBoardId());
                    return lastId == null || !lastId.equals(r.getListId());
                })
                .toList();

        if (active.isEmpty()) {
            add(new Paragraph("Nema taskova koji su dodijeljeni tebi (ili su svi već u Done)."));
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        List<CardRepository.MyTaskRow> overdue = active.stream()
                .filter(r -> r.getDueAt() != null && r.getDueAt().isBefore(now))
                .sorted(Comparator.comparing(CardRepository.MyTaskRow::getDueAt))
                .toList();

        List<CardRepository.MyTaskRow> todayDue = active.stream()
                .filter(r -> r.getDueAt() != null
                        && r.getDueAt().toLocalDate().equals(today)
                        && !r.getDueAt().isBefore(now))
                .sorted(Comparator.comparing(CardRepository.MyTaskRow::getDueAt))
                .toList();

        List<CardRepository.MyTaskRow> upcoming = active.stream()
                .filter(r -> r.getDueAt() != null && r.getDueAt().toLocalDate().isAfter(today))
                .sorted(Comparator.comparing(CardRepository.MyTaskRow::getDueAt))
                .toList();

        List<CardRepository.MyTaskRow> noDue = active.stream()
                .filter(r -> r.getDueAt() == null)
                .sorted(Comparator.comparing(CardRepository.MyTaskRow::getUpdatedAt).reversed())
                .toList();

        add(summaryRow(active.size(), overdue.size(), todayDue.size(), upcoming.size(), noDue.size()));

        add(section("Overdue", overdue, VaadinIcon.WARNING));
        add(section("Due today", todayDue, VaadinIcon.CLOCK));
        add(section("Upcoming", upcoming, VaadinIcon.CALENDAR));
        add(section("No due date", noDue, VaadinIcon.MINUS));
    }

    private Component summaryRow(int all, int overdue, int today, int upcoming, int noDue) {
        HorizontalLayout row = new HorizontalLayout();
        row.setPadding(false);
        row.setSpacing(true);

        row.add(badge("Total: " + all));
        row.add(badge("Overdue: " + overdue));
        row.add(badge("Today: " + today));
        row.add(badge("Upcoming: " + upcoming));
        row.add(badge("No due: " + noDue));

        return row;
    }

    private Span badge(String text) {
        Span s = new Span(text);
        s.getStyle()
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("font-size", "var(--lumo-font-size-s)");
        return s;
    }

    private Component section(String title, List<CardRepository.MyTaskRow> rows, VaadinIcon icon) {
        HorizontalLayout header = new HorizontalLayout(icon.create(), new Span(title + " (" + rows.size() + ")"));
        header.setDefaultVerticalComponentAlignment(HorizontalLayout.Alignment.CENTER);
        header.getStyle().set("font-weight", "600");

        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        wrap.add(header);

        if (rows.isEmpty()) {
            Paragraph empty = new Paragraph("Nema taskova u sekciji: " + title + ".");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
            wrap.add(empty);
            return wrap;
        }

        Grid<CardRepository.MyTaskRow> grid = buildGrid();
        grid.setItems(rows);
        grid.setAllRowsVisible(true);

        wrap.add(grid);
        return wrap;
    }

    private Grid<CardRepository.MyTaskRow> buildGrid() {
        Grid<CardRepository.MyTaskRow> grid = new Grid<>(CardRepository.MyTaskRow.class, false);
        grid.setWidthFull();

        grid.addColumn(CardRepository.MyTaskRow::getTitle)
                .setHeader("Task")
                .setAutoWidth(true)
                .setFlexGrow(2);

        grid.addColumn(r -> r.getBoardName() + " / " + r.getListTitle())
                .setHeader("Board / List")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(r -> r.getDueAt() == null ? "" : r.getDueAt().toString())
                .setHeader("Due")
                .setAutoWidth(true)
                .setFlexGrow(0);

        // ✅ Mark done (premjesti u zadnju listu)
        grid.addComponentColumn(r -> {
            Button done = new Button("Done", VaadinIcon.CHECK.create(), e -> {
                try {
                    services.cardService.markDone(r.getCardId(), loggedUser.getId());
                    Notification.show("Prebačeno u Done.");
                    MainView.getMainView().setContent(new MyTasksView()); // refresh
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });
            return done;
        }).setHeader("Akcija").setAutoWidth(true).setFlexGrow(0);

        // ✅ Klik na red -> otvori TaskDialog edit
        grid.addItemClickListener(ev -> {
            Long cardId = ev.getItem().getCardId();
            Card card = services.cardService.requireById(cardId);
            TaskDialog.edit(services, card, loggedUser.getId()).open();
        });

        return grid;
    }

    @Override public String getTabName() { return "My Tasks"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.TASKS; }

    @Override public DomEventListener onTabClick() {
        // novi instance view (izbjegava Vaadin state-tree probleme)
        return e -> MainView.getMainView().setContent(new MyTasksView());
    }
}
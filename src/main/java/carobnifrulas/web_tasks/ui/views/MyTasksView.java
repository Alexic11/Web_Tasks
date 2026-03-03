package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class MyTasksView extends View implements MenuTab {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Grid<CardRepository.MyTaskRow> grid =
            new Grid<>(CardRepository.MyTaskRow.class, false);

    // ✅ UI state
    private final FilterState filterState = new FilterState();

    // ✅ data cache (za filtering bez DB na svako slovo)
    private List<CardRepository.MyTaskRow> allRows = List.of();

    private static final class FilterState {
        String titleQuery;      // search po title
        String boardQuery;      // search po board/list
        Integer priority;       // null = svi
        boolean overdueOnly;

        void reset() {
            titleQuery = "";
            boardQuery = "";
            priority = null;
            overdueOnly = false;
        }
    }

    @Override
    public void setElements() {
        add(new H2("My Tasks"));

        // ===== FILTER BAR =====
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        TextField titleSearch = new TextField();
        titleSearch.setLabel("Search task");
        titleSearch.setWidth("280px");
        titleSearch.setClearButtonVisible(true);
        titleSearch.setValue(filterState.titleQuery == null ? "" : filterState.titleQuery);
        titleSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        titleSearch.setValueChangeTimeout(300);

        TextField boardSearch = new TextField();
        boardSearch.setLabel("Search board/list");
        boardSearch.setWidth("320px");
        boardSearch.setClearButtonVisible(true);
        boardSearch.setValue(filterState.boardQuery == null ? "" : filterState.boardQuery);
        boardSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        boardSearch.setValueChangeTimeout(300);

        Select<Integer> pr = new Select<>();
        pr.setLabel("Prioritet");
        pr.setWidth("180px");
        pr.setEmptySelectionAllowed(true);
        pr.setEmptySelectionCaption("Svi");
        pr.setItems(1, 2, 3, 4, 5);
        pr.setItemLabelGenerator(p -> p == null ? "Svi" : "P" + p);
        pr.setValue(filterState.priority);

        Checkbox overdue = new Checkbox("Overdue");
        overdue.setValue(filterState.overdueOnly);

        Button reset = new Button("Reset");

        Span count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        bar.add(titleSearch, boardSearch, pr, overdue, reset, count);
        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("margin-top", "8px");
        add(bar);

        // ===== GRID =====
        grid.setWidthFull();

        grid.addColumn(CardRepository.MyTaskRow::getTitle)
                .setHeader("Task")
                .setAutoWidth(true)
                .setFlexGrow(2);

        grid.addComponentColumn(r -> buildPriorityBadge(r.getPriority()))
                .setHeader("Prioritet")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> r.getBoardName() + " / " + r.getListTitle())
                .setHeader("Board / List")
                .setAutoWidth(true)
                .setFlexGrow(1);

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

        grid.setAllRowsVisible(true);
        add(grid);

        // ===== FILTER LOGIC =====
        Runnable applyFilters = () -> {
            List<CardRepository.MyTaskRow> filtered = new ArrayList<>();

            String tq = filterState.titleQuery == null ? "" : filterState.titleQuery.trim().toLowerCase();
            String bq = filterState.boardQuery == null ? "" : filterState.boardQuery.trim().toLowerCase();
            Integer prio = filterState.priority;
            boolean overdueOnly = filterState.overdueOnly;

            LocalDateTime now = LocalDateTime.now();

            for (CardRepository.MyTaskRow r : allRows) {
                // title search
                if (!tq.isEmpty()) {
                    String t = r.getTitle() == null ? "" : r.getTitle().toLowerCase();
                    if (!t.contains(tq)) continue;
                }

                // board/list search
                if (!bq.isEmpty()) {
                    String bl = (safe(r.getBoardName()) + " / " + safe(r.getListTitle())).toLowerCase();
                    if (!bl.contains(bq)) continue;
                }

                // priority filter
                if (prio != null) {
                    int p = r.getPriority() == null ? 1 : r.getPriority();
                    if (!prio.equals(p)) continue;
                }

                // overdue filter
                if (overdueOnly) {
                    if (r.getDueAt() == null) continue;
                    if (!r.getDueAt().isBefore(now)) continue;
                }

                filtered.add(r);
            }

            grid.setItems(filtered);
            count.setText("Prikaz: " + filtered.size() + " / " + allRows.size());
        };

        // listeners (live)
        titleSearch.addValueChangeListener(e -> {
            filterState.titleQuery = e.getValue();
            applyFilters.run();
        });

        boardSearch.addValueChangeListener(e -> {
            filterState.boardQuery = e.getValue();
            applyFilters.run();
        });

        pr.addValueChangeListener(e -> {
            filterState.priority = e.getValue();
            applyFilters.run();
        });

        overdue.addValueChangeListener(e -> {
            filterState.overdueOnly = Boolean.TRUE.equals(e.getValue());
            applyFilters.run();
        });

        reset.addClickListener(e -> {
            filterState.reset();
            titleSearch.setValue("");
            boardSearch.setValue("");
            pr.clear();
            overdue.setValue(false);
            applyFilters.run();
        });

        // ===== INITIAL LOAD =====
        refresh();       // učita allRows
        applyFilters.run(); // prikaže i setuje count
    }

    private void refresh() {
        allRows = services.cardService.findMyTasks(loggedUser.getId());
        if (allRows == null) allRows = List.of();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
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
package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@org.springframework.stereotype.Component
public class MyTasksView extends View implements MenuTab {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final FilterState filterState = new FilterState();
    private List<CardRepository.MyTaskRow> allRows = List.of();

    private Span count;
    private VerticalLayout sectionsWrap;
    private HorizontalLayout summaryRow;

    private static final class FilterState {
        String titleQuery;
        String boardQuery;
        Integer priority;
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

        count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        add(buildFilterBar());

        summaryRow = new HorizontalLayout();
        summaryRow.setWidthFull();
        summaryRow.setSpacing(true);
        summaryRow.getStyle().set("margin-top", "8px");
        add(summaryRow);

        sectionsWrap = new VerticalLayout();
        sectionsWrap.setPadding(false);
        sectionsWrap.setSpacing(true);
        sectionsWrap.setWidthFull();
        add(sectionsWrap);

        refresh();
        applyFiltersAndRender();
    }

    private Component buildFilterBar() {
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
        reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(titleSearch, boardSearch, pr, overdue, reset, count);
        bar.setFlexGrow(1, boardSearch);

        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "12px")
                .set("margin-top", "8px")
                .set("background", "white");

        titleSearch.addValueChangeListener(e -> {
            filterState.titleQuery = e.getValue();
            applyFiltersAndRender();
        });

        boardSearch.addValueChangeListener(e -> {
            filterState.boardQuery = e.getValue();
            applyFiltersAndRender();
        });

        pr.addValueChangeListener(e -> {
            filterState.priority = e.getValue();
            applyFiltersAndRender();
        });

        overdue.addValueChangeListener(e -> {
            filterState.overdueOnly = Boolean.TRUE.equals(e.getValue());
            applyFiltersAndRender();
        });

        reset.addClickListener(e -> {
            filterState.reset();
            titleSearch.setValue("");
            boardSearch.setValue("");
            pr.clear();
            overdue.setValue(false);
            applyFiltersAndRender();
        });

        return bar;
    }

    private void refresh() {
        allRows = services.cardService.findMyTasks(loggedUser.getId());
        if (allRows == null) {
            allRows = List.of();
        }
    }

    private void applyFiltersAndRender() {
        List<CardRepository.MyTaskRow> filtered = applyFilters(allRows);

        count.setText("Prikaz: " + filtered.size() + " / " + allRows.size());

        renderSummary(filtered);
        renderSections(filtered);
    }

    private List<CardRepository.MyTaskRow> applyFilters(List<CardRepository.MyTaskRow> source) {
        List<CardRepository.MyTaskRow> filtered = new ArrayList<>();

        String tq = filterState.titleQuery == null ? "" : filterState.titleQuery.trim().toLowerCase();
        String bq = filterState.boardQuery == null ? "" : filterState.boardQuery.trim().toLowerCase();
        Integer prio = filterState.priority;
        boolean overdueOnly = filterState.overdueOnly;

        LocalDateTime now = LocalDateTime.now();

        for (CardRepository.MyTaskRow r : source) {
            if (!tq.isEmpty()) {
                String t = safe(r.getTitle()).toLowerCase();
                if (!t.contains(tq)) {
                    continue;
                }
            }

            if (!bq.isEmpty()) {
                String bl = (safe(r.getBoardName()) + " / " + safe(r.getListTitle())).toLowerCase();
                if (!bl.contains(bq)) {
                    continue;
                }
            }

            if (prio != null) {
                int p = r.getPriority() == null ? 1 : r.getPriority();
                if (!prio.equals(p)) {
                    continue;
                }
            }

            if (overdueOnly) {
                if (r.getDueAt() == null || !r.getDueAt().isBefore(now)) {
                    continue;
                }
            }

            filtered.add(r);
        }

        return filtered;
    }

    private void renderSummary(List<CardRepository.MyTaskRow> rows) {
        summaryRow.removeAll();

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        int total = rows.size();
        int overdue = 0;
        int todayCount = 0;
        int highPriority = 0;
        int noDue = 0;

        for (CardRepository.MyTaskRow r : rows) {
            Integer p = r.getPriority() == null ? 1 : r.getPriority();
            if (p >= 4) {
                highPriority++;
            }

            if (r.getDueAt() == null) {
                noDue++;
            } else {
                if (r.getDueAt().isBefore(now)) {
                    overdue++;
                }
                if (r.getDueAt().toLocalDate().equals(today)) {
                    todayCount++;
                }
            }
        }

        summaryRow.add(
                buildSummaryCard("Ukupno", String.valueOf(total), "var(--lumo-primary-color-10pct)"),
                buildSummaryCard("Overdue", String.valueOf(overdue), "var(--lumo-error-color-10pct)"),
                buildSummaryCard("Today", String.valueOf(todayCount), "var(--lumo-warning-color-10pct)"),
                buildSummaryCard("High Priority", String.valueOf(highPriority), "var(--lumo-success-color-10pct)"),
                buildSummaryCard("No Due Date", String.valueOf(noDue), "var(--lumo-contrast-10pct)")
        );
    }

    private Component buildSummaryCard(String label, String value, String background) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.setWidth("190px");

        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "14px")
                .set("background", background)
                .set("box-sizing", "border-box");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "6px");

        card.add(valueSpan, labelSpan);
        return card;
    }

    private void renderSections(List<CardRepository.MyTaskRow> rows) {
        sectionsWrap.removeAll();

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDate next7 = today.plusDays(7);

        List<CardRepository.MyTaskRow> overdueRows = new ArrayList<>();
        List<CardRepository.MyTaskRow> todayRows = new ArrayList<>();
        List<CardRepository.MyTaskRow> next7Rows = new ArrayList<>();
        List<CardRepository.MyTaskRow> noDueRows = new ArrayList<>();

        for (CardRepository.MyTaskRow r : rows) {
            LocalDateTime due = r.getDueAt();

            if (due == null) {
                noDueRows.add(r);
            } else if (due.isBefore(now)) {
                overdueRows.add(r);
            } else if (due.toLocalDate().equals(today)) {
                todayRows.add(r);
            } else if (!due.toLocalDate().isAfter(next7)) {
                next7Rows.add(r);
            } else {
                next7Rows.add(r);
            }
        }

        if (!overdueRows.isEmpty()) {
            sectionsWrap.add(buildSection("Overdue", overdueRows, true));
        }
        if (!todayRows.isEmpty()) {
            sectionsWrap.add(buildSection("Today", todayRows, false));
        }
        if (!next7Rows.isEmpty()) {
            sectionsWrap.add(buildSection("Next 7 Days", next7Rows, false));
        }
        if (!noDueRows.isEmpty()) {
            sectionsWrap.add(buildSection("No Due Date", noDueRows, false));
        }

        if (rows.isEmpty()) {
            VerticalLayout empty = new VerticalLayout();
            empty.setPadding(false);
            empty.setSpacing(false);
            empty.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
            empty.setWidthFull();
            empty.getStyle()
                    .set("padding", "32px")
                    .set("border", "1px dashed var(--lumo-contrast-20pct)")
                    .set("border-radius", "14px")
                    .set("color", "var(--lumo-secondary-text-color)");

            Span icon = new Span("📭");
            icon.getStyle().set("font-size", "28px");

            Span txt = new Span("Nema taskova za prikaz.");
            txt.getStyle().set("margin-top", "8px");

            empty.add(icon, txt);
            sectionsWrap.add(empty);
        }
    }

    private Component buildSection(String title, List<CardRepository.MyTaskRow> rows, boolean overdueSection) {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();

        H3 header = new H3(title + " (" + rows.size() + ")");
        header.getStyle()
                .set("margin", "8px 0 0 0")
                .set("color", overdueSection ? "var(--lumo-error-text-color)" : "inherit");

        FlexLayout cards = new FlexLayout();
        cards.setWidthFull();
        cards.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        cards.getStyle().set("gap", "12px");

        for (CardRepository.MyTaskRow row : rows) {
            cards.add(buildTaskCard(row, overdueSection));
        }

        wrap.add(header, cards);
        return wrap;
    }

    private Component buildTaskCard(CardRepository.MyTaskRow row, boolean overdueSection) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(true);
        card.setWidth("340px");

        card.getStyle()
                .set("border", overdueSection
                        ? "1px solid var(--lumo-error-color-30pct)"
                        : "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "14px")
                .set("background", overdueSection
                        ? "linear-gradient(to bottom right, var(--lumo-error-color-10pct), white)"
                        : "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.05)")
                .set("cursor", "pointer")
                .set("box-sizing", "border-box");

        card.addClickListener(e -> openTask(row.getCardId()));

        Span title = new Span(safe(row.getTitle()));
        title.getStyle()
                .set("font-weight", "700")
                .set("font-size", "var(--lumo-font-size-m)");

        Span board = new Span(safe(row.getBoardName()) + " / " + safe(row.getListTitle()));
        board.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout meta = new HorizontalLayout();
        meta.setSpacing(true);
        meta.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        meta.add(buildPriorityBadge(row.getPriority()), buildDueLabel(row.getDueAt()));

        card.add(title, board, meta);
        return card;
    }

    private void openTask(Long cardId) {
        try {
            Card c = services.cardService.requireById(cardId);
            TaskDialog.edit(services, c, loggedUser.getId()).open();
        } catch (Exception ex) {
            Notification.show(ex.getMessage());
        }
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
        String txt = (dueAt == null) ? "No due date" : DT_FMT.format(dueAt);

        Span s = new Span(txt);
        s.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "2px 10px")
                .set("border-radius", "999px")
                .set("background", "var(--lumo-contrast-5pct)");

        if (dueAt != null && dueAt.isBefore(LocalDateTime.now())) {
            s.getStyle()
                    .set("color", "var(--lumo-error-text-color)")
                    .set("font-weight", "700")
                    .set("background", "var(--lumo-error-color-10pct)");
        } else {
            s.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");
        }

        return s;
    }

    @Override
    public String getTabName() {
        return "My Tasks";
    }

    @Override
    public VaadinIcon getTabIcon() {
        return VaadinIcon.TASKS;
    }

    @Override
    public DomEventListener onTabClick() {
        return e -> MainView.getMainView().setContent(this);
    }
}
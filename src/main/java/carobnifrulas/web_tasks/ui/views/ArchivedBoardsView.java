package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.Board;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.component.textfield.TextField;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ArchivedBoardsView extends View implements MenuTab {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Grid<Board> grid = new Grid<>(Board.class, false);

    private List<Board> allBoards = List.of();

    private final FilterState filterState = new FilterState();

    private Span count;
    private HorizontalLayout summaryRow;

    private static final class FilterState {
        String nameQuery;
        String archivedQuery;

        void reset() {
            nameQuery = "";
            archivedQuery = "";
        }
    }

    @Override
    public void setElements() {
        add(buildHeaderSection());

        count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        summaryRow = new HorizontalLayout();
        summaryRow.setWidthFull();
        summaryRow.setSpacing(true);
        summaryRow.getStyle().set("margin-top", "6px");
        add(summaryRow);

        add(buildFilterBar());

        configureGrid();

        VerticalLayout gridWrap = new VerticalLayout(grid);
        gridWrap.setPadding(false);
        gridWrap.setSpacing(false);
        gridWrap.setWidthFull();
        gridWrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "12px")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)");

        add(gridWrap);

        refreshAll();
        applyFiltersAndRender();
    }

    private com.vaadin.flow.component.Component buildHeaderSection() {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "18px")
                .set("padding", "18px")
                .set("background", "linear-gradient(to right, var(--lumo-contrast-5pct), white)");

        H2 title = new H2("History");
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph("Pregled zatvorenih boardova i mogućnost njihovog ponovnog otvaranja.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout left = new VerticalLayout(title, subtitle);
        left.setPadding(false);
        left.setSpacing(false);

        wrap.add(left);
        return wrap;
    }

    private com.vaadin.flow.component.Component buildFilterBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        TextField nameSearch = new TextField();
        nameSearch.setLabel("Search naziv");
        nameSearch.setWidth("320px");
        nameSearch.setClearButtonVisible(true);
        nameSearch.setValue(filterState.nameQuery == null ? "" : filterState.nameQuery);
        nameSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        nameSearch.setValueChangeTimeout(300);

        TextField archivedSearch = new TextField();
        archivedSearch.setLabel("Search zatvoren");
        archivedSearch.setWidth("240px");
        archivedSearch.setClearButtonVisible(true);
        archivedSearch.setValue(filterState.archivedQuery == null ? "" : filterState.archivedQuery);
        archivedSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        archivedSearch.setValueChangeTimeout(300);

        Button reset = new Button("Reset");
        reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(nameSearch, archivedSearch, reset, count);
        bar.setFlexGrow(1, nameSearch);

        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "12px")
                .set("margin-top", "8px")
                .set("background", "white");

        nameSearch.addValueChangeListener(e -> {
            filterState.nameQuery = e.getValue();
            applyFiltersAndRender();
        });

        archivedSearch.addValueChangeListener(e -> {
            filterState.archivedQuery = e.getValue();
            applyFiltersAndRender();
        });

        reset.addClickListener(e -> {
            filterState.reset();
            nameSearch.setValue("");
            archivedSearch.setValue("");
            applyFiltersAndRender();
        });

        return bar;
    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.setWidthFull();
        grid.addClassName("boards-grid");
        grid.getStyle().set("cursor", "pointer");

        grid.addColumn(Board::getName)
                .setHeader("Naziv")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(this::buildArchivedBadge)
                .setHeader("Zatvoren")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(b -> {
            Button reopen = new Button("Reopen", VaadinIcon.UNLOCK.create());
            reopen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            boolean isGlobalAdmin = carobnifrulas.web_tasks.security.model.SecurityUtils.isGlobalAdmin(loggedUser);

            boolean canReopen;
            if (isGlobalAdmin) {
                canReopen = true;
            } else {
                try {
                    BoardRole r = services.boardMemberService.getRole(b.getId(), loggedUser.getId());
                    canReopen = (r == BoardRole.OWNER);
                } catch (Exception ex) {
                    canReopen = false;
                }
            }

            reopen.setVisible(canReopen);

            reopen.addClickListener(e -> {
                ConfirmDialog cd = new ConfirmDialog();
                cd.setHeader("Ponovo otvoriti board?");
                cd.setText("Board će se vratiti iz History u aktivne boardove.");
                cd.setCancelable(true);
                cd.setConfirmText("Reopen");
                cd.setConfirmButtonTheme("primary");

                cd.addConfirmListener(ev -> {
                    try {
                        services.boardService.reopenBoard(b.getId(), loggedUser.getId());
                        Notification.show("Board ponovo otvoren.");
                        refreshAll();
                        applyFiltersAndRender();
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage());
                    }
                });

                cd.open();
            });

            return reopen;
        }).setHeader("Akcija").setAutoWidth(true);

        grid.addItemDoubleClickListener(ev ->
                MainView.getMainView().setContent(
                        new BoardView(ev.getItem().getId(), BoardView.BackTarget.HISTORY)
                )
        );

        grid.setAllRowsVisible(true);
    }

    private com.vaadin.flow.component.Component buildArchivedBadge(Board b) {
        String txt = b.getArchivedAt() == null ? "—" : DT_FMT.format(b.getArchivedAt());

        Span badge = new Span(txt);
        badge.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "600")
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("color", "var(--lumo-secondary-text-color)");

        return badge;
    }

    private void refreshAll() {
        List<Board> boards = services.boardService.listArchivedBoardsFor(loggedUser);
        allBoards = (boards == null) ? List.of() : boards;
    }

    private void applyFiltersAndRender() {
        String nq = filterState.nameQuery == null ? "" : filterState.nameQuery.trim().toLowerCase();
        String aq = filterState.archivedQuery == null ? "" : filterState.archivedQuery.trim().toLowerCase();

        List<Board> filtered = new ArrayList<>();

        for (Board b : allBoards) {
            if (!nq.isEmpty()) {
                String name = b.getName() == null ? "" : b.getName().toLowerCase();
                if (!name.contains(nq)) {
                    continue;
                }
            }

            if (!aq.isEmpty()) {
                String arch = (b.getArchivedAt() == null) ? "—" : DT_FMT.format(b.getArchivedAt());
                if (!arch.toLowerCase().contains(aq)) {
                    continue;
                }
            }

            filtered.add(b);
        }

        grid.setItems(filtered);
        count.setText("Prikaz: " + filtered.size() + " / " + allBoards.size());
        renderSummary(filtered);
    }

    private void renderSummary(List<Board> filtered) {
        summaryRow.removeAll();

        int total = filtered.size();
        int archivedToday = 0;
        int archivedThisWeek = 0;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minusDays(7);

        for (Board b : filtered) {
            if (b.getArchivedAt() != null) {
                if (b.getArchivedAt().toLocalDate().equals(now.toLocalDate())) {
                    archivedToday++;
                }
                if (b.getArchivedAt().isAfter(weekAgo)) {
                    archivedThisWeek++;
                }
            }
        }

        summaryRow.add(
                buildSummaryCard("Ukupno arhiviranih", String.valueOf(total), "var(--lumo-primary-color-10pct)"),
                buildSummaryCard("Zatvoreni danas", String.valueOf(archivedToday), "var(--lumo-warning-color-10pct)"),
                buildSummaryCard("Zatvoreni ove sedmice", String.valueOf(archivedThisWeek), "var(--lumo-contrast-10pct)")
        );
    }

    private com.vaadin.flow.component.Component buildSummaryCard(String label, String value, String background) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.setWidth("240px");

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

    @Override
    public String getTabName() {
        return "History";
    }

    @Override
    public VaadinIcon getTabIcon() {
        return VaadinIcon.ARCHIVE;
    }

    @Override
    public DomEventListener onTabClick() {
        return e -> MainView.getMainView().setContent(this);
    }
}
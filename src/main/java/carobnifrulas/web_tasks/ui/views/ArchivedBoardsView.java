package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.Board;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.component.textfield.TextField;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ArchivedBoardsView extends View implements MenuTab {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Grid<Board> grid = new Grid<>(Board.class, false);

    // ✅ cache (za live filtering bez DB na svako slovo)
    private List<Board> allBoards = List.of();

    // ✅ state
    private final FilterState filterState = new FilterState();

    private static final class FilterState {
        String nameQuery;     // search po nazivu
        String archivedQuery; // search po datumu zatvaranja (formatirani string)

        void reset() {
            nameQuery = "";
            archivedQuery = "";
        }
    }

    @Override
    public void setElements() {
        add(new H2("History"));

        configureGrid();

        // ===== FILTER BAR =====
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

        Span count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        bar.add(nameSearch, archivedSearch, reset, count);

        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("margin-top", "8px");

        add(bar);

        // ===== LOAD + APPLY FILTERS =====
        refreshAll(); // napuni allBoards

        Runnable applyFilters = () -> {
            String nq = filterState.nameQuery == null ? "" : filterState.nameQuery.trim().toLowerCase();
            String aq = filterState.archivedQuery == null ? "" : filterState.archivedQuery.trim().toLowerCase();

            List<Board> filtered = new ArrayList<>();

            for (Board b : allBoards) {
                // name filter
                if (!nq.isEmpty()) {
                    String name = b.getName() == null ? "" : b.getName().toLowerCase();
                    if (!name.contains(nq)) continue;
                }

                // archivedAt filter (formatirani string)
                if (!aq.isEmpty()) {
                    String arch = (b.getArchivedAt() == null) ? "—" : DT_FMT.format(b.getArchivedAt());
                    if (!arch.toLowerCase().contains(aq)) continue;
                }

                filtered.add(b);
            }

            grid.setItems(filtered);
            count.setText("Prikaz: " + filtered.size() + " / " + allBoards.size());
        };

        nameSearch.addValueChangeListener(e -> {
            filterState.nameQuery = e.getValue();
            applyFilters.run();
        });

        archivedSearch.addValueChangeListener(e -> {
            filterState.archivedQuery = e.getValue();
            applyFilters.run();
        });

        reset.addClickListener(e -> {
            filterState.reset();
            nameSearch.setValue("");
            archivedSearch.setValue("");
            applyFilters.run();
        });

        // inicijalno
        applyFilters.run();

        add(grid);
    }

    private void configureGrid() {
        grid.setWidthFull();
        grid.addClassName("boards-grid");
        grid.getStyle().set("cursor", "pointer");

        grid.addColumn(Board::getName)
                .setHeader("Naziv")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(b -> b.getArchivedAt() == null ? "—" : DT_FMT.format(b.getArchivedAt()))
                .setHeader("Zatvoren")
                .setAutoWidth(true)
                .setFlexGrow(0);

        // ✅ REOPEN (samo global admin ili OWNER)
        grid.addComponentColumn(b -> {
            Button reopen = new Button("Reopen", VaadinIcon.UNLOCK.create());

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

                        // refresh cache + grid (filteri ostaju)
                        refreshAll();
                        // nakon refreshAll, setElements se ne zove ponovo, zato ručno re-apply:
                        // najlakše: samo resetuje iteme pa filter bar radi dalje
                        // (applyFilters je lokalni runnable u setElements; zato ovdje samo reload view)
                        MainView.getMainView().setContent(this);
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage());
                    }
                });

                cd.open();
            });

            return reopen;
        }).setHeader("Akcija").setAutoWidth(true);

        grid.addItemDoubleClickListener(ev ->
                MainView.getMainView().setContent(new BoardView(ev.getItem().getId()))
        );

        grid.setAllRowsVisible(true);
    }

    private void refreshAll() {
        List<Board> boards = services.boardService.listArchivedBoardsFor(loggedUser);
        allBoards = (boards == null) ? List.of() : boards;
    }

    // MenuTab
    @Override public String getTabName() { return "History"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.ARCHIVE; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}
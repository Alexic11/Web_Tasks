package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.Board;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BoardsView extends View implements MenuTab {

    private final Grid<Board> grid = new Grid<>(Board.class, false);

    // ✅ cache za live search
    private List<Board> allBoards = List.of();

    // ✅ state
    private final FilterState filterState = new FilterState();

    private static final class FilterState {
        String nameQuery;

        void reset() {
            nameQuery = "";
        }
    }

    @Override
    public void setElements() {
        add(new H2("Boards"));

        Button create = new Button("Novi board", e -> openCreateDialog());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.setIcon(VaadinIcon.PLUS.create());

        configureGrid();

        // ===== FILTER BAR =====
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        TextField nameSearch = new TextField();
        nameSearch.setLabel("Search naziv");
        nameSearch.setWidth("360px");
        nameSearch.setClearButtonVisible(true);
        nameSearch.setValue(filterState.nameQuery == null ? "" : filterState.nameQuery);
        nameSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        nameSearch.setValueChangeTimeout(300);

        Button reset = new Button("Reset");
        reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);


        Span count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        bar.add(nameSearch, reset, count);

        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("margin-top", "8px");

        // ===== LOAD + APPLY FILTERS =====
        refreshAll();

        Runnable applyFilters = () -> {
            String nq = filterState.nameQuery == null ? "" : filterState.nameQuery.trim().toLowerCase();

            List<Board> filtered = new ArrayList<>();
            for (Board b : allBoards) {
                if (!nq.isEmpty()) {
                    String name = b.getName() == null ? "" : b.getName().toLowerCase();
                    if (!name.contains(nq)) continue;
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

        reset.addClickListener(e -> {
            filterState.reset();
            nameSearch.setValue("");
            applyFilters.run();
        });

        // inicijalno
        applyFilters.run();

        add(create, bar, grid);
    }

    private void configureGrid() {
        grid.setWidthFull();
        grid.addClassName("boards-grid");
        grid.getStyle().set("cursor", "pointer");

        // =========================
        // COLUMNS
        // =========================

        grid.addColumn(Board::getName)
                .setHeader("Naziv")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(b -> {
            Button open = new Button("Otvori",
                    e -> MainView.getMainView().setContent(new BoardView(b.getId())));
            open.setIcon(VaadinIcon.ARROW_RIGHT.create());
            open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            return open;
        }).setHeader("Akcija").setAutoWidth(true);

        // =========================
        // ✅ ZATVORI (sa confirm dialogom)
        // =========================
        grid.addComponentColumn(b -> {
            Button close = new Button("Zatvori", VaadinIcon.LOCK.create());

            close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);


            boolean isGlobalAdmin = carobnifrulas.web_tasks.security.model.SecurityUtils.isGlobalAdmin(loggedUser);
            if (isGlobalAdmin) {
                close.setVisible(true);
            } else {
                try {
                    BoardRole r = services.boardMemberService.getRole(b.getId(), loggedUser.getId());
                    close.setVisible(r == BoardRole.OWNER || r == BoardRole.ADMIN);
                } catch (Exception ex) {
                    close.setVisible(false);
                }
            }

            close.addClickListener(e -> {
                long openCnt;
                try {
                    openCnt = services.cardService.countOpenTasks(b.getId());
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                    return;
                }

                if (openCnt > 0) {
                    ConfirmDialog info = new ConfirmDialog();
                    info.setHeader("Ne možeš zatvoriti board");
                    info.setText("Board '" + b.getName() + "' ima još otvorenih taskova: " + openCnt +
                            ". Premjesti sve taskove u Done pa pokušaj ponovo.");
                    info.setConfirmText("OK");
                    info.setConfirmButtonTheme("primary");
                    info.setCancelable(false);
                    info.open();
                    return;
                }

                ConfirmDialog cd = new ConfirmDialog();
                cd.setHeader("Zatvori board?");
                cd.setText("Jesi li siguran da želiš zatvoriti board '" + b.getName() + "' ? Board će preći u History.");
                cd.setCancelable(true);

                cd.setConfirmText("Zatvori");
                cd.setConfirmButtonTheme("error primary");

                cd.addConfirmListener(ev -> {
                    try {
                        services.boardService.archiveBoard(b.getId(), loggedUser.getId());
                        Notification.show("Board '" + b.getName() + "' je zatvoren.");

                        // refresh cache + ostani u istom view-u (najjednostavnije)
                        refreshAll();
                        MainView.getMainView().setContent(this);
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage());
                    }
                });

                cd.open();
            });

            return close;
        }).setHeader("Zatvori").setAutoWidth(true);

        // =========================
        // ✅ DOUBLE CLICK
        // =========================
        grid.addItemDoubleClickListener(ev ->
                MainView.getMainView().setContent(new BoardView(ev.getItem().getId()))
        );

        grid.setAllRowsVisible(true);
    }

    // ✅ učitaj jednom (za filtering)
    private void refreshAll() {
        List<Board> boards = services.boardService.listBoardsFor(loggedUser);
        allBoards = (boards == null) ? List.of() : boards;
    }

    private void openCreateDialog() {
        Dialog d = new Dialog();
        d.setHeaderTitle("Kreiraj board");

        TextField name = new TextField("Naziv");
        name.setWidthFull();
        name.setPlaceholder("npr. Sprint 1");

        Button save = new Button("Sačuvaj", e -> {
            if (name.getValue() == null || name.getValue().trim().isEmpty()) {
                Notification.show("Unesi naziv boarda.");
                return;
            }

            try {
                services.boardService.createBoard(name.getValue().trim(), loggedUser.getId());
                d.close();

                // refresh cache + reload view (da filter bar/count bude tačan odmah)
                refreshAll();
                MainView.getMainView().setContent(this);
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);


        Button cancel = new Button("Otkaži", e -> d.close());

        cancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY);


        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        d.add(new VerticalLayout(name, actions));
        d.open();
    }

    // MenuTab
    @Override public String getTabName() { return "Boards"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.DASHBOARD; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}
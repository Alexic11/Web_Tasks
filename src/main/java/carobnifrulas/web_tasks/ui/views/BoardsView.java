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
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;
import java.util.ArrayList;
import java.util.List;

public class BoardsView extends View implements MenuTab {

    private final Grid<Board> grid = new Grid<>(Board.class, false);

    private List<Board> allBoards = List.of();
    private final FilterState filterState = new FilterState();

    private Span count;
    private HorizontalLayout summaryRow;

    private static final class FilterState {
        String nameQuery;

        void reset() {
            nameQuery = "";
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
                .set("background", "linear-gradient(to right, var(--lumo-primary-color-10pct), white)");

        H2 title = new H2("Boards");
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph("Pregled aktivnih boardova, brzo pretraživanje i kreiranje novih boardova.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)");

        Button create = new Button("Novi board", e -> openCreateDialog());
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.setIcon(VaadinIcon.PLUS.create());

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        VerticalLayout left = new VerticalLayout(title, subtitle);
        left.setPadding(false);
        left.setSpacing(false);

        top.add(left, create);
        wrap.add(top);

        return wrap;
    }

    private com.vaadin.flow.component.Component buildFilterBar() {
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

        bar.add(nameSearch, reset, count);
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

        reset.addClickListener(e -> {
            filterState.reset();
            nameSearch.setValue("");
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

        grid.addComponentColumn(b -> {
            Button open = new Button("Otvori",
                    e -> MainView.getMainView().setContent(
                            new BoardView(b.getId(), BoardView.BackTarget.BOARDS)
                    ));
            open.setIcon(VaadinIcon.ARROW_RIGHT.create());
            open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            return open;
        }).setHeader("Akcija").setAutoWidth(true);

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
                        refreshAll();
                        applyFiltersAndRender();
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage());
                    }
                });

                cd.open();
            });

            return close;
        }).setHeader("Zatvori").setAutoWidth(true);

        grid.addItemDoubleClickListener(ev ->
                MainView.getMainView().setContent(
                        new BoardView(ev.getItem().getId(), BoardView.BackTarget.BOARDS)
                )
        );

        grid.setAllRowsVisible(true);
    }

    private void refreshAll() {
        List<Board> boards = services.boardService.listBoardsFor(loggedUser);
        allBoards = (boards == null) ? List.of() : boards;
    }

    private void applyFiltersAndRender() {
        String nq = filterState.nameQuery == null ? "" : filterState.nameQuery.trim().toLowerCase();

        List<Board> filtered = new ArrayList<>();
        for (Board b : allBoards) {
            if (!nq.isEmpty()) {
                String name = b.getName() == null ? "" : b.getName().toLowerCase();
                if (!name.contains(nq)) {
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
        int closable = 0;
        int ownerBoards = 0;

        boolean isGlobalAdmin = carobnifrulas.web_tasks.security.model.SecurityUtils.isGlobalAdmin(loggedUser);

        for (Board b : filtered) {
            try {
                BoardRole role = services.boardMemberService.getRole(b.getId(), loggedUser.getId());

                if (isGlobalAdmin || role == BoardRole.OWNER || role == BoardRole.ADMIN) {
                    closable++;
                }

                if (role == BoardRole.OWNER) {
                    ownerBoards++;
                }
            } catch (Exception ignored) {
            }
        }

        summaryRow.add(
                buildSummaryCard("Ukupno boardova", String.valueOf(total), "var(--lumo-primary-color-10pct)"),
                buildSummaryCard("Mogu zatvoriti", String.valueOf(closable), "var(--lumo-warning-color-10pct)"),
                buildSummaryCard("Moji OWNER boardovi", String.valueOf(ownerBoards), "var(--lumo-success-color-10pct)")
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
                refreshAll();
                applyFiltersAndRender();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Otkaži", e -> d.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(save, cancel);

        VerticalLayout layout = new VerticalLayout(name, actions);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setWidth("420px");

        d.add(layout);
        d.open();
    }

    @Override
    public String getTabName() {
        return "Boards";
    }

    @Override
    public VaadinIcon getTabIcon() {
        return VaadinIcon.DASHBOARD;
    }

    @Override
    public DomEventListener onTabClick() {
        return e -> MainView.getMainView().setContent(this);
    }
}
package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.Board;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;


@Component
public class BoardsView extends View implements MenuTab {

    private final Grid<Board> grid = new Grid<>(Board.class, false);

    @Override
    public void setElements() {
        add(new com.vaadin.flow.component.html.H2("Boards"));

        Button create = new Button("Novi board", e -> openCreateDialog());
        create.setIcon(VaadinIcon.PLUS.create());

        configureGrid();
        refresh();

        add(create, grid);
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
            return open;
        }).setHeader("Akcija").setAutoWidth(true);

        // =========================
        // ✅ ZATVORI (sa confirm dialogom)  [2A]
        // =========================

        grid.addComponentColumn(b -> {
            Button close = new Button("Zatvori", VaadinIcon.LOCK.create());

            // vidljivost: global admin ili OWNER/ADMIN na boardu
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
                ConfirmDialog cd = new ConfirmDialog();
                cd.setHeader("Zatvori board?");
                cd.setText("Jesi li siguran da želiš zatvoriti ovaj board? Board će preći u History.");
                cd.setCancelable(true);

                cd.setConfirmText("Zatvori");
                cd.setConfirmButtonTheme("error primary");

                cd.addConfirmListener(ev -> {
                    try {
                        services.boardService.archiveBoard(b.getId(), loggedUser.getId());
                        Notification.show("Board zatvoren.");
                        refresh();
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


    private void refresh() {
        List<Board> boards = services.boardService.listBoardsFor(loggedUser);
        grid.setItems(boards);
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
            services.boardService.createBoard(name.getValue().trim(), loggedUser.getId());
            d.close();
            refresh();
        });

        Button cancel = new Button("Otkaži", e -> d.close());

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        d.add(new VerticalLayout(name, actions));
        d.open();
    }

    // MenuTab
    @Override public String getTabName() { return "Boards"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.DASHBOARD; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}

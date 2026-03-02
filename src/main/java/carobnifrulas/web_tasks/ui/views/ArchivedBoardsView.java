package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.Board;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ArchivedBoardsView extends View implements MenuTab {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Grid<Board> grid = new Grid<>(Board.class, false);

    @Override
    public void setElements() {
        add(new com.vaadin.flow.component.html.H2("History"));

        configureGrid();
        refresh();

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

            boolean canReopen = false;
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
                        refresh();
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

    private void refresh() {
        List<Board> boards = services.boardService.listArchivedBoardsFor(loggedUser);
        grid.setItems(boards);
    }

    // MenuTab
    @Override public String getTabName() { return "History"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.ARCHIVE; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}

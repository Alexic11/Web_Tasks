package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.user.User;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;

public class BoardMembersDialog extends Dialog {

    private final Long boardId;
    private final Long actorUserId;
    private final ServicesHolder services;

    private ComboBox<User> userBox;

    private final Grid<BoardMemberRepository.MemberRow> grid =
            new Grid<>(BoardMemberRepository.MemberRow.class, false);

    public BoardMembersDialog(Long boardId, Long actorUserId, ServicesHolder services) {
        this.boardId = boardId;
        this.actorUserId = actorUserId;
        this.services = services;

        setHeaderTitle("Members");
        setWidth("820px");

        add(buildTop());
        add(buildGrid());

        refresh();
    }

    private Component buildTop() {
        userBox = new ComboBox<>("Korisnik");
        userBox.setPlaceholder("Odaberi korisnika...");
        userBox.setWidth("360px");
        userBox.setClearButtonVisible(true);
        userBox.setItemLabelGenerator(u -> u.getFullName() + " (" + u.getEmail() + ")");
        userBox.setItems(services.boardMemberService.listUsersNotInBoard(boardId));

        Select<BoardRole> role = new Select<>();
        role.setLabel("Rola");
        role.setItems(BoardRole.ADMIN, BoardRole.MEMBER, BoardRole.VIEWER);
        role.setValue(BoardRole.MEMBER);
        role.setWidth("180px");

        Button add = new Button("Dodaj", e -> {
            try {
                if (userBox.getValue() == null) {
                    Notification.show("Odaberi korisnika.");
                    return;
                }

                services.boardMemberService.addMemberByUserId(
                        boardId,
                        actorUserId,
                        userBox.getValue().getId(),
                        role.getValue()
                );

                userBox.clear();
                role.setValue(BoardRole.MEMBER);
                refresh();
                Notification.show("Dodato.");
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        HorizontalLayout row = new HorizontalLayout(userBox, role, add);
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        row.setWidthFull();

        VerticalLayout wrap = new VerticalLayout(new H4("Dodaj člana"), row);
        wrap.setPadding(false);
        wrap.setSpacing(true);
        return wrap;
    }

    private Component buildGrid() {
        grid.setWidthFull();

        grid.addColumn(BoardMemberRepository.MemberRow::getEmail)
                .setHeader("Email").setAutoWidth(true).setFlexGrow(1);

        grid.addColumn(BoardMemberRepository.MemberRow::getFullName)
                .setHeader("Ime").setAutoWidth(true).setFlexGrow(1);

        grid.addColumn(BoardMemberRepository.MemberRow::getRole)
                .setHeader("Rola").setAutoWidth(true);

        grid.addComponentColumn(r -> {
            BoardRole current = BoardRole.valueOf(r.getRole());
            if (current == BoardRole.OWNER) {
                return new Span("—");
            }

            Select<BoardRole> sel = new Select<>();
            sel.setItems(BoardRole.ADMIN, BoardRole.MEMBER, BoardRole.VIEWER);
            sel.setValue(current);
            sel.addValueChangeListener(ev -> {
                if (ev.getValue() == null) return;
                try {
                    services.boardMemberService.changeRole(boardId, actorUserId, r.getUserId(), ev.getValue());
                    Notification.show("Sačuvano.");
                    refresh();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                    refresh();
                }
            });
            return sel;
        }).setHeader("Promijeni rolu").setAutoWidth(true);

        grid.addComponentColumn(r -> {
            BoardRole current = BoardRole.valueOf(r.getRole());
            if (current == BoardRole.OWNER) {
                return new Span("");
            }

            Button remove = new Button("Ukloni", e -> {
                try {
                    services.boardMemberService.removeMember(boardId, actorUserId, r.getUserId());
                    Notification.show("Uklonjeno.");
                    refresh();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });
            return remove;
        }).setHeader("Ukloni").setAutoWidth(true);

        grid.setAllRowsVisible(true);

        VerticalLayout wrap = new VerticalLayout(new H4("Članovi"), grid);
        wrap.setPadding(false);
        wrap.setSpacing(true);
        return wrap;
    }

    private void refresh() {
        grid.setItems(services.boardMemberService.listMemberRows(boardId));
        if (userBox != null) {
            userBox.setItems(services.boardMemberService.listUsersNotInBoard(boardId));
        }
    }
}

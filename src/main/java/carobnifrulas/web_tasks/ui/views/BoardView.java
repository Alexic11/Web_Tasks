package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.list.ListEntity;
import carobnifrulas.web_tasks.ui.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;

import java.util.List;

public class BoardView extends View {

    private final Long boardId;

    public BoardView(Long boardId) {
        this.boardId = boardId;
    }

    @Override
    public void setElements() {
        var board = services.boardService.requireMemberBoard(boardId, loggedUser.getId());

        // ✅ Moj role na ovom boardu
        BoardRole myRole = services.boardMemberService.getRole(boardId, loggedUser.getId());
        boolean canManageMembers = (myRole == BoardRole.OWNER || myRole == BoardRole.ADMIN);
        boolean canWrite = (myRole != BoardRole.VIEWER); // OWNER/ADMIN/MEMBER

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        // Lijevo: back + title
        HorizontalLayout left = new HorizontalLayout();
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Button back = new Button("Nazad", e -> MainView.getMainView().setContent(services.menu.getDefaultView()));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());

        H2 title = new H2(board.getName());
        left.add(back, title);

        // Desno: role badge + Members (ako smije)
        HorizontalLayout right = new HorizontalLayout();
        right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Span roleBadge = new Span(myRole.name());
        roleBadge.getStyle()
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("font-size", "var(--lumo-font-size-s)");

        Button membersBtn = new Button("Members", VaadinIcon.USERS.create(), e -> {
            new BoardMembersDialog(boardId, loggedUser.getId(),services).open();
        });
        membersBtn.setVisible(canManageMembers);

        right.add(roleBadge, membersBtn);

        top.add(left, right);
        add(top);

        if (!canWrite) {
            Paragraph p = new Paragraph("VIEWER režim: možeš samo pregledati board.");
            p.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(p);
        }

        List<ListEntity> lists = services.listService.findByBoard(boardId);
        if (lists.isEmpty()) {
            add(new Paragraph("Nema lista na ovom boardu."));
            return;
        }

        HorizontalLayout columns = new HorizontalLayout();
        columns.setWidthFull();
        columns.setSpacing(true);

        for (int i = 0; i < lists.size(); i++) {
            ListEntity list = lists.get(i);
            columns.add(buildColumn(list, lists, i, canWrite));
        }

        add(columns);
    }

    private Component buildColumn(ListEntity list, List<ListEntity> allLists, int idx, boolean canWrite) {
        VerticalLayout col = new VerticalLayout();
        col.setPadding(true);
        col.setSpacing(true);
        col.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "12px")
                .set("min-width", "320px");

        H3 h = new H3(list.getTitle());
        col.add(h);

        List<Card> cards = services.cardService.findByList(list.getId());
        for (Card c : cards) {
            col.add(renderCard(c, allLists, idx, canWrite));
        }

        return col;
    }

    private Component renderCard(Card c, List<ListEntity> allLists, int idx, boolean canWrite) {
        VerticalLayout box = new VerticalLayout();
        box.setPadding(true);
        box.setSpacing(false);
        box.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "12px");

        Span title = new Span(c.getTitle());
        title.getStyle().set("font-weight", "700");

        Span assignee = new Span("Assigned: " + (c.getAssignedTo() == null ? "-" : c.getAssignedTo()));
        assignee.getStyle().set("font-size", "var(--lumo-font-size-s)");

        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button take = new Button("Preuzmi", e -> {
            services.cardService.assignToMe(c.getId(), loggedUser.getId());
            MainView.getMainView().setContent(new BoardView(boardId));
        });

        Button release = new Button("Pusti", e -> {
            services.cardService.unassign(c.getId(), loggedUser.getId());
            MainView.getMainView().setContent(new BoardView(boardId));
        });

        boolean iAmAssignee = c.getAssignedTo() != null && c.getAssignedTo().equals(loggedUser.getId());
        take.setEnabled(canWrite && c.getAssignedTo() == null);
        release.setEnabled(canWrite && iAmAssignee);

        Button left = new Button(VaadinIcon.ARROW_LEFT.create(), e -> {
            if (!canWrite) return;
            if (idx == 0) return;
            services.cardService.moveToList(c.getId(), allLists.get(idx - 1).getId());
            MainView.getMainView().setContent(new BoardView(boardId));
        });

        Button right = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> {
            if (!canWrite) return;
            if (idx >= allLists.size() - 1) return;
            services.cardService.moveToList(c.getId(), allLists.get(idx + 1).getId());
            MainView.getMainView().setContent(new BoardView(boardId));
        });

        left.setEnabled(canWrite && idx > 0);
        right.setEnabled(canWrite && idx < allLists.size() - 1);

        actions.add(take, release, left, right);

        box.add(title, assignee, actions);
        return box;
    }
}
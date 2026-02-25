package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.list.ListEntity;
import carobnifrulas.web_tasks.ui.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
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

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Button back = new Button("Nazad", e -> MainView.getMainView().setContent(services.menu.getDefaultView()));
        back.setIcon(com.vaadin.flow.component.icon.VaadinIcon.ARROW_LEFT.create());

        H2 title = new H2(board.getName());
        top.add(back, title);

        add(top);

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
            columns.add(buildColumn(list, lists, i));
        }

        add(columns);
    }

    private Component buildColumn(ListEntity list, List<ListEntity> allLists, int idx) {
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
            col.add(renderCard(c, allLists, idx));
        }

        return col;
    }

    private Component renderCard(Card c, List<ListEntity> allLists, int idx) {
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
        take.setEnabled(c.getAssignedTo() == null);
        release.setEnabled(iAmAssignee);

        Button left = new Button(com.vaadin.flow.component.icon.VaadinIcon.ARROW_LEFT.create(), e -> {
            if (idx == 0) return;
            services.cardService.moveToList(c.getId(), allLists.get(idx - 1).getId());
            MainView.getMainView().setContent(new BoardView(boardId));
        });

        Button right = new Button(com.vaadin.flow.component.icon.VaadinIcon.ARROW_RIGHT.create(), e -> {
            if (idx >= allLists.size() - 1) return;
            services.cardService.moveToList(c.getId(), allLists.get(idx + 1).getId());
            MainView.getMainView().setContent(new BoardView(boardId));
        });

        actions.add(take, release, left, right);

        box.add(title, assignee, actions);
        return box;
    }
}
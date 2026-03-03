package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.activity.CardActivity;
import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.ui.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskDialog extends Dialog {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ServicesHolder services;
    private final Long boardId;
    private final Long listId;     // za create
    private final Long actorUserId;
    private final Card existing;   // null => create

    public static TaskDialog create(ServicesHolder services, Long boardId, Long listId, Long actorUserId) {
        return new TaskDialog(services, boardId, listId, actorUserId, null);
    }

    public static TaskDialog edit(ServicesHolder services, Card existing, Long actorUserId) {
        return new TaskDialog(services, existing.getBoardId(), existing.getListId(), actorUserId, existing);
    }

    private TaskDialog(ServicesHolder services, Long boardId, Long listId, Long actorUserId, Card existing) {
        this.services = services;
        this.boardId = boardId;
        this.listId = listId;
        this.actorUserId = actorUserId;
        this.existing = existing;

        boolean isEdit = existing != null;

        BoardRole myRole = services.boardMemberService.getRole(boardId, actorUserId);
        boolean canWrite = myRole != BoardRole.VIEWER;

        setHeaderTitle(isEdit ? "Uredi task" : "Novi task");
        setWidth("980px");
        setHeight("720px");
        setDraggable(true);
        setResizable(true);

        // =========================
        // ✅ FIELDS
        // =========================
        TextField title = new TextField("Naslov");
        title.setWidthFull();

        TextArea desc = new TextArea("Opis");
        desc.setWidthFull();
        desc.setMinHeight("220px");
        desc.setMaxHeight("320px");

        DateTimePicker due = new DateTimePicker("Rok (opciono)");
        due.setWidth("280px");

        Select<Integer> priority = new Select<>();
        priority.setLabel("Prioritet");
        priority.setItems(1, 2, 3, 4, 5);
        priority.setValue(1);
        priority.setWidth("260px");
        priority.setItemLabelGenerator(p -> switch (p) {
            case 1 -> "1 - Normalno";
            case 2 -> "2 - Nisko";
            case 3 -> "3 - Srednje";
            case 4 -> "4 - Visoko";
            case 5 -> "5 - HITNO (kritično)";
            default -> String.valueOf(p);
        });

        Select<Long> assignedTo = new Select<>();
        assignedTo.setLabel("Dodijeli (opciono)");
        assignedTo.setWidth("420px");
        assignedTo.setEmptySelectionAllowed(true);
        assignedTo.setEmptySelectionCaption("— niko —");

        List<BoardMemberRepository.AssigneeRow> rows = services.boardMemberService.listAssignees(boardId);

        Map<Long, String> labels = rows.stream().collect(Collectors.toMap(
                BoardMemberRepository.AssigneeRow::getUserId,
                r -> {
                    String name = (r.getFullName() == null || r.getFullName().isBlank()) ? "" : r.getFullName().trim();
                    if (!name.isEmpty()) return name + " (" + r.getEmail() + ")";
                    return r.getEmail();
                }
        ));

        assignedTo.setItems(labels.keySet());
        assignedTo.setItemLabelGenerator(id -> labels.getOrDefault(id, String.valueOf(id)));

        if (isEdit) {
            title.setValue(nullSafe(existing.getTitle()));
            desc.setValue(nullSafe(existing.getDescription()));
            due.setValue(existing.getDueAt());
            assignedTo.setValue(existing.getAssignedTo());
            Integer p = existing.getPriority();
            priority.setValue(p == null ? 1 : p);
        }

        title.setReadOnly(!canWrite);
        desc.setReadOnly(!canWrite);
        due.setReadOnly(!canWrite);
        priority.setReadOnly(!canWrite);
        assignedTo.setReadOnly(!canWrite);

        Button save = new Button("Sačuvaj");
        Button cancel = new Button("Otkaži", e -> close());
        save.setEnabled(canWrite);

        save.addClickListener(e -> {
            try {
                LocalDateTime dueVal = due.getValue();
                Long assigneeId = assignedTo.getValue();
                Integer pr = priority.getValue();

                if (!isEdit) {
                    services.cardService.createCard(
                            boardId, listId,
                            title.getValue(),
                            desc.getValue(),
                            dueVal,
                            pr,
                            assigneeId,
                            actorUserId
                    );
                } else {
                    services.cardService.updateCard(
                            existing.getId(),
                            actorUserId,
                            title.getValue(),
                            desc.getValue(),
                            dueVal,
                            pr,
                            assigneeId
                    );
                }

                close();
                Notification.show("Sačuvano.");
                MainView.getMainView().setContent(new BoardView(boardId));

            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        HorizontalLayout actions = new HorizontalLayout(save, cancel);

        HorizontalLayout row2 = new HorizontalLayout(due, priority, assignedTo);
        row2.setWidthFull();
        row2.setFlexGrow(1, assignedTo);

        // =========================
        // ✅ DETAILS "CARD"
        // =========================
        VerticalLayout detailsCard = new VerticalLayout(
                new H4(isEdit ? "Detalji" : "Kreiranje"),
                title,
                desc,
                row2,
                actions
        );
        detailsCard.setPadding(false);
        detailsCard.setSpacing(true);
        detailsCard.setWidthFull();
        applyCardStyle(detailsCard);

        // create: samo detalji
        if (!isEdit) {
            add(detailsCard);
            return;
        }

        // edit: komentari i activity ispod
        VerticalLayout commentsCard = buildCommentsSection(existing.getId(), canWrite);
        applyCardStyle(commentsCard);

        VerticalLayout activityCard = buildActivitySection(existing.getId());
        applyCardStyle(activityCard);

        VerticalLayout root = new VerticalLayout(detailsCard, commentsCard, activityCard);
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        add(root);
    }

    private VerticalLayout buildActivitySection(Long cardId) {
        Grid<CardActivity> activityGrid = new Grid<>(CardActivity.class, false);
        activityGrid.setWidthFull();

        // scroll: da ne raste beskonačno
        activityGrid.setHeight("280px");

        activityGrid.addColumn(a -> formatInstant(a.getCreatedAt()))
                .setHeader("Vrijeme")
                .setAutoWidth(true)
                .setFlexGrow(0);

        activityGrid.addColumn(CardActivity::getActorEmail)
                .setHeader("Ko")
                .setAutoWidth(true)
                .setFlexGrow(1);

        activityGrid.addColumn(CardActivity::getAction)
                .setHeader("Akcija")
                .setAutoWidth(true)
                .setFlexGrow(0);

        activityGrid.addColumn(a -> nullSafe(a.getOldValue()))
                .setHeader("Staro")
                .setAutoWidth(true)
                .setFlexGrow(1);

        activityGrid.addColumn(a -> nullSafe(a.getNewValue()))
                .setHeader("Novo")
                .setAutoWidth(true)
                .setFlexGrow(1);

        List<CardActivity> acts = services.cardActivityService.listForCard(cardId);
        activityGrid.setItems(acts);

        VerticalLayout wrap = new VerticalLayout(
                new H4("Activity"),
                activityGrid
        );
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();

        return wrap;
    }

    private VerticalLayout buildCommentsSection(Long cardId, boolean canWrite) {
        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        H3 title = new H3("Komentari");
        title.getStyle().set("margin", "0");

        MessageList list = new MessageList();
        list.setWidth("95%");

        // scroll + border unutar card-a
        list.getStyle()
                .set("max-height", "220px")
                .set("overflow", "auto")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "10px")
                .set("padding", "8px");

        Span hint = new Span(
                canWrite ? "Napiši komentar i pritisni Enter." : "Nemaš prava da dodaješ komentare."
        );
        hint.getStyle().set("font-size", "var(--lumo-font-size-s)");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Runnable reload = () -> {
            try {
                var rows = services.cardCommentService.listForCard(cardId, actorUserId);
                var items = new ArrayList<MessageListItem>();

                for (var r : rows) {
                    String author = (r.getAuthorName() != null && !r.getAuthorName().isBlank())
                            ? r.getAuthorName()
                            : r.getAuthorEmail();

                    java.time.Instant when = null;
                    if (r.getCreatedAt() != null) {
                        when = r.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
                    }

                    items.add(new MessageListItem(r.getBody(), when, author));
                }

                list.setItems(items);
            } catch (Exception ex) {
                Notification.show("Ne mogu učitati komentare: " + ex.getMessage(),
                        4000, Notification.Position.MIDDLE);
            }
        };

        reload.run();

        MessageInput input = new MessageInput();
        input.setWidthFull();
        input.setEnabled(canWrite);

        input.addSubmitListener(e -> {
            try {
                services.cardCommentService.addComment(cardId, actorUserId, e.getValue());
                reload.run();
            } catch (Exception ex) {
                Notification.show("Greška: " + ex.getMessage(),
                        4000, Notification.Position.MIDDLE);
            }
        });

        root.add(title, list, hint, input);
        return root;
    }

    private static void applyCardStyle(VerticalLayout layout) {
        layout.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "12px");
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String formatInstant(java.time.Instant ins) {
        if (ins == null) return "—";
        LocalDateTime dt = LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
        return DT_FMT.format(dt);
    }
}
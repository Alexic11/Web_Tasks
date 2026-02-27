package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.ui.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskDialog extends Dialog {

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

        TextField title = new TextField("Naslov");
        title.setWidthFull();

        TextArea desc = new TextArea("Opis");
        desc.setWidthFull();
        desc.setMinHeight("220px");
        desc.setMaxHeight("320px");

        DateTimePicker due = new DateTimePicker("Rok (opciono)");
        due.setWidth("280px");

        // ✅ Prioritet 1-5
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

        // ✅ Opcioni assignee
        Select<Long> assignedTo = new Select<>();
        assignedTo.setLabel("Dodijeli (opciono)");
        assignedTo.setWidth("420px");
        assignedTo.setEmptySelectionAllowed(true);
        assignedTo.setEmptySelectionCaption("— niko —");

        // učitaj članove boarda (id + fullName + email)
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
            due.setValue(existing.getDueAt());                 // može null
            assignedTo.setValue(existing.getAssignedTo());    // može null

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
                LocalDateTime dueVal = due.getValue();      // može null
                Long assigneeId = assignedTo.getValue();    // može null
                Integer pr = priority.getValue();           // ne bi trebalo null, ali može ako user obriše

                if (!isEdit) {
                    // ✅ createCard proširen da prima priority
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
                    // ✅ updateCard proširen da prima priority
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

        VerticalLayout content = new VerticalLayout(
                new H4(isEdit ? "Detalji" : "Kreiranje"),
                title,
                desc,
                row2,
                actions
        );
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        add(content);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}

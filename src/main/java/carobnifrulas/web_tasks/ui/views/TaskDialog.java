package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardRealtimeBus;
import carobnifrulas.web_tasks.card.activity.CardActivity;
import carobnifrulas.web_tasks.card.attachment.CardAttachment;
import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.user.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class TaskDialog extends Dialog {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ServicesHolder services;
    private final Long boardId;
    private final Long listId;
    private final Long actorUserId;
    private final Card existing;

    private Grid<CardActivity> activityGrid;
    private Long currentActivityCardId;

    private Div attachmentsFilesWrap;
    private boolean attachmentsCanWrite;

    private MessageList commentsList;
    private Runnable reloadComments;

    private Registration realtimeRegistration;

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
        setWidth("1040px");
        setMaxWidth("96vw");
        setHeight("780px");
        setDraggable(true);
        setResizable(true);

        TextField title = new TextField("Naslov");
        title.setWidthFull();

        TextArea desc = new TextArea("Opis");
        desc.setWidthFull();
        desc.setMinHeight("220px");
        desc.setMaxHeight("320px");
        desc.setPlaceholder("Unesi opis taska...");

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

        ComboBox<Long> assignedTo = new ComboBox<>();
        assignedTo.setLabel("Dodijeli (opciono)");
        assignedTo.setWidth("420px");
        assignedTo.setPlaceholder("Nedodijeljeno");
        assignedTo.setClearButtonVisible(true);

        List<BoardMemberRepository.AssigneeRow> rows = services.boardMemberService.listAssignees(boardId);

        Map<Long, String> labels = rows.stream().collect(Collectors.toMap(
                BoardMemberRepository.AssigneeRow::getUserId,
                r -> {
                    String name = (r.getFullName() == null || r.getFullName().isBlank()) ? "" : r.getFullName().trim();
                    if (!name.isEmpty()) {
                        return name + " (" + r.getEmail() + ")";
                    }
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
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Otkaži", e -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

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
        actions.setSpacing(true);

        HorizontalLayout row2 = new HorizontalLayout(due, priority, assignedTo);
        row2.setWidthFull();
        row2.setFlexGrow(1, assignedTo);

        VerticalLayout detailsCard = new VerticalLayout(
                buildSectionHeader(isEdit ? "Detalji taska" : "Kreiranje taska",
                        isEdit ? "Pregled i izmjena osnovnih informacija." : "Unesi osnovne informacije za novi task."),
                title,
                desc,
                row2,
                actions
        );
        detailsCard.setPadding(false);
        detailsCard.setSpacing(true);
        detailsCard.setWidthFull();
        applyCardStyle(detailsCard);

        if (!isEdit) {
            add(detailsCard);
            return;
        }

        VerticalLayout attachmentsCard = buildAttachmentsSection(existing.getId(), canWrite);
        applyCardStyle(attachmentsCard);

        VerticalLayout commentsCard = buildCommentsSection(existing.getId(), canWrite);
        applyCardStyle(commentsCard);

        VerticalLayout activityCard = buildActivitySection(existing.getId());
        applyCardStyle(activityCard);

        VerticalLayout root = new VerticalLayout(detailsCard, attachmentsCard, commentsCard, activityCard);
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        Scroller scroller = new Scroller(root);
        scroller.setWidthFull();
        scroller.setHeightFull();

        add(scroller);

        registerRealtime(existing.getId());
    }

    private com.vaadin.flow.component.Component buildSectionHeader(String title, String subtitle) {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(false);

        H4 h = new H4(title);
        h.getStyle().set("margin", "0");

        Paragraph p = new Paragraph(subtitle);
        p.getStyle()
                .set("margin", "4px 0 0 0")
                .set("color", "var(--lumo-secondary-text-color)");

        wrap.add(h, p);
        return wrap;
    }

    private void registerRealtime(Long cardId) {
        realtimeRegistration = CardRealtimeBus.register(cardId, changeType ->
                getUI().ifPresent(ui -> ui.access(this::refreshAllSections))
        );

        addDetachListener(e -> {
            if (realtimeRegistration != null) {
                realtimeRegistration.remove();
                realtimeRegistration = null;
            }
        });
    }

    private void refreshAllSections() {
        refreshActivitySection();

        if (attachmentsFilesWrap != null && currentActivityCardId != null) {
            refreshAttachmentsList(attachmentsFilesWrap, currentActivityCardId, attachmentsCanWrite);
        }

        if (reloadComments != null) {
            reloadComments.run();
        }
    }

    private VerticalLayout buildActivitySection(Long cardId) {
        this.currentActivityCardId = cardId;

        activityGrid = new Grid<>(CardActivity.class, false);
        activityGrid.setWidthFull();
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

        refreshActivitySection();

        VerticalLayout wrap = new VerticalLayout(
                buildSectionHeader("Activity", "Istorija izmjena i aktivnosti nad taskom."),
                activityGrid
        );
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();

        return wrap;
    }

    private void refreshActivitySection() {
        if (activityGrid == null || currentActivityCardId == null) {
            return;
        }

        List<CardActivity> acts = services.cardActivityService.listForCard(currentActivityCardId);
        activityGrid.setItems(acts);
    }

    private VerticalLayout buildCommentsSection(Long cardId, boolean canWrite) {
        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        commentsList = new MessageList();
        commentsList.setWidthFull();
        commentsList.getStyle()
                .set("max-height", "240px")
                .set("overflow", "auto")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "8px")
                .set("box-sizing", "border-box")
                .set("background", "white");

        Span hint = new Span(
                canWrite ? "Napiši komentar i pritisni Enter." : "Nemaš prava da dodaješ komentare."
        );
        hint.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        reloadComments = () -> {
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

                commentsList.setItems(items);
            } catch (Exception ex) {
                Notification.show("Ne mogu učitati komentare: " + ex.getMessage(),
                        4000, Notification.Position.MIDDLE);
            }
        };

        reloadComments.run();

        MessageInput input = new MessageInput();
        input.setWidthFull();
        input.setEnabled(canWrite);

        input.addSubmitListener(e -> {
            try {
                services.cardCommentService.addComment(cardId, actorUserId, e.getValue());
                reloadComments.run();
                refreshActivitySection();
            } catch (Exception ex) {
                Notification.show("Greška: " + ex.getMessage(),
                        4000, Notification.Position.MIDDLE);
            }
        });

        root.add(
                buildSectionHeader("Komentari", "Diskusija i komunikacija vezana za task."),
                commentsList,
                hint,
                input
        );
        return root;
    }

    private VerticalLayout buildAttachmentsSection(Long cardId, boolean canWrite) {
        this.attachmentsCanWrite = canWrite;

        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        Span hint = new Span(
                canWrite
                        ? "Dodaj fajl uz task."
                        : "Možeš pregledati priloge, ali nemaš pravo dodavanja ili brisanja."
        );
        hint.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        attachmentsFilesWrap = new Div();
        attachmentsFilesWrap.setWidthFull();
        attachmentsFilesWrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("box-sizing", "border-box")
                .set("overflow", "hidden")
                .set("background", "white");

        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            try {
                User actor = requireActorUser();
                try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
                    services.cardAttachmentService.upload(
                            cardId,
                            actor,
                            metadata.fileName(),
                            metadata.contentType(),
                            in
                    );
                }

                getUI().ifPresent(ui -> ui.access(() -> {
                    Notification.show("Fajl uspješno dodan.");
                    refreshAttachmentsList(attachmentsFilesWrap, cardId, canWrite);
                    refreshActivitySection();
                }));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }));

        upload.setWidthFull();
        upload.setDropLabel(new Span("Prevuci fajl ovdje ili klikni za odabir."));
        upload.setMaxFiles(10);
        upload.setEnabled(canWrite);
        upload.addClassName("attachments-upload");

        refreshAttachmentsList(attachmentsFilesWrap, cardId, canWrite);

        root.add(
                buildSectionHeader("Prilozi", "Dokumenti i ostali fajlovi vezani za task."),
                hint,
                upload,
                attachmentsFilesWrap
        );
        return root;
    }

    private void refreshAttachmentsList(Div filesWrap, Long cardId, boolean canWrite) {
        filesWrap.removeAll();

        try {
            User actor = requireActorUser();
            List<CardAttachment> attachments = services.cardAttachmentService.findByCard(cardId, actor);

            if (attachments.isEmpty()) {
                Span empty = new Span("Nema dodanih fajlova.");
                empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
                filesWrap.add(empty);
                return;
            }

            for (CardAttachment a : attachments) {
                HorizontalLayout row = new HorizontalLayout();
                row.setWidthFull();
                row.setPadding(false);
                row.setSpacing(true);
                row.setAlignItems(Alignment.CENTER);
                row.setJustifyContentMode(JustifyContentMode.BETWEEN);

                row.getStyle()
                        .set("padding", "8px 0")
                        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                        .set("box-sizing", "border-box");

                String contentType = (a.getContentType() == null || a.getContentType().isBlank())
                        ? "nepoznat tip"
                        : a.getContentType();

                Span info = new Span(
                        a.getOriginalFilename() + " (" + humanReadableSize(a.getSizeBytes()) + ", " + contentType + ")"
                );
                info.getStyle()
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("word-break", "break-word");

                HorizontalLayout actions = new HorizontalLayout();
                actions.setPadding(false);
                actions.setSpacing(true);
                actions.setAlignItems(Alignment.CENTER);
                actions.getStyle().set("flex-shrink", "0");

                Anchor downloadLink = buildDownloadLink(a);
                actions.add(downloadLink);

                if (canWrite) {
                    Button deleteBtn = new Button("Obriši");
                    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

                    deleteBtn.addClickListener(e -> {
                        try {
                            User actor2 = requireActorUser();
                            services.cardAttachmentService.delete(a.getId(), actor2);

                            Notification.show("Prilog obrisan.");
                            refreshAttachmentsList(filesWrap, cardId, canWrite);
                            refreshActivitySection();
                        } catch (Exception ex) {
                            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                        }
                    });

                    actions.add(deleteBtn);
                }

                row.add(info, actions);
                row.expand(info);
                filesWrap.add(row);
            }

        } catch (Exception ex) {
            Notification.show("Ne mogu učitati priloge: " + ex.getMessage(),
                    4000, Notification.Position.MIDDLE);
        }
    }

    private User requireActorUser() {
        return services.userService.findById(actorUserId)
                .orElseThrow(() -> new IllegalStateException("Prijavljeni korisnik nije pronađen."));
    }

    private static void applyCardStyle(VerticalLayout layout) {
        layout.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "16px")
                .set("box-sizing", "border-box")
                .set("overflow", "hidden")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)");
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String formatInstant(java.time.Instant ins) {
        if (ins == null) return "—";
        LocalDateTime dt = LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
        return DT_FMT.format(dt);
    }

    private static String humanReadableSize(Long sizeBytes) {
        if (sizeBytes == null) return "0 B";

        double size = sizeBytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return ((long) size) + " " + units[unitIndex];
        }

        return String.format(Locale.US, "%.1f %s", size, units[unitIndex]);
    }

    private Anchor buildDownloadLink(CardAttachment attachment) {
        Anchor download = new Anchor((DownloadHandler) event -> {
            User actor = requireActorUser();
            var downloadable = services.cardAttachmentService.getDownloadable(attachment.getId(), actor);

            event.setFileName(attachment.getOriginalFilename());

            if (attachment.getContentType() != null && !attachment.getContentType().isBlank()) {
                event.setContentType(attachment.getContentType());
            }

            try (var in = java.nio.file.Files.newInputStream(downloadable.path());
                 var out = event.getOutputStream()) {
                in.transferTo(out);
            }
        }, "");

        download.getElement().setAttribute("download", true);

        Button downloadBtn = new Button("Preuzmi");
        downloadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        download.add(downloadBtn);

        return download;
    }
}
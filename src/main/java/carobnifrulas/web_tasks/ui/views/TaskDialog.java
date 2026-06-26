package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.CardPresenceBus;
import carobnifrulas.web_tasks.card.CardRealtimeBus;
import carobnifrulas.web_tasks.card.TaskVersionConflictException;
import carobnifrulas.web_tasks.card.activity.CardActivity;
import carobnifrulas.web_tasks.card.checklist.CardChecklistItem;
import carobnifrulas.web_tasks.card.label.CardLabel;
import carobnifrulas.web_tasks.card.attachment.CardAttachment;
import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.user.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskDialog extends Dialog {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ServicesHolder services;
    private final Long boardId;
    private final Long listId;
    private final Long actorUserId;
    private final Card existing;
    private Long currentCardVersion;

    private Div presenceWrap;
    private Registration presenceRegistration;

    private final Runnable onTaskChanged;
    private final boolean hasTaskChangedCallback;
    private boolean taskChanged = false;

    private Grid<CardActivity> activityGrid;
    private Long currentActivityCardId;

    private Div attachmentsFilesWrap;
    private boolean attachmentsCanWrite;

    private Div checklistItemsWrap;
    private boolean checklistCanWrite;
    private Runnable reloadChecklist;

    private Div labelsWrap;
    private boolean labelsCanWrite;
    private Runnable reloadLabels;

    private MessageList commentsList;
    private Runnable reloadComments;

    private Registration realtimeRegistration;

    public static TaskDialog create(ServicesHolder services, Long boardId, Long listId, Long actorUserId) {
        return new TaskDialog(services, boardId, listId, actorUserId, null, null);
    }

    public static TaskDialog edit(ServicesHolder services, Card existing, Long actorUserId) {
        return new TaskDialog(services, existing.getBoardId(), existing.getListId(), actorUserId, existing, null);
    }

    public static TaskDialog edit(ServicesHolder services, Card existing, Long actorUserId, Runnable onTaskChanged) {
        return new TaskDialog(services, existing.getBoardId(), existing.getListId(), actorUserId, existing, onTaskChanged);
    }

    private TaskDialog(ServicesHolder services, Long boardId, Long listId, Long actorUserId, Card existing, Runnable onTaskChanged) {
        this.services = services;
        this.boardId = boardId;
        this.listId = listId;
        this.actorUserId = actorUserId;
        this.existing = existing;
        this.hasTaskChangedCallback = onTaskChanged != null;
        this.onTaskChanged = onTaskChanged == null ? () -> {} : onTaskChanged;

        boolean isEdit = existing != null;
        this.currentCardVersion = isEdit ? normalizeVersion(existing.getVersion()) : null;

        BoardRole myRole = services.boardMemberService.getRole(boardId, actorUserId);
        boolean archived = services.boardService.requireMemberBoard(boardId, actorUserId).getArchivedAt() != null;
        boolean canWrite = myRole != BoardRole.VIEWER && !archived;

        setHeaderTitle(isEdit ? "Uredi task" : "Novi task");
        addDialogCloseButton();
        setWidth("1040px");
        setMaxWidth("96vw");
        setHeight("780px");
        setDraggable(true);
        setResizable(true);

        addOpenedChangeListener(e -> {
            if (!e.isOpened()) {
                unregisterPresence();
                notifyTaskChangedIfNeeded();
            }
        });

        TextField title = new TextField("Naslov");
        title.setWidthFull();

        TextArea desc = new TextArea("Opis");
        desc.setWidthFull();
        desc.setMinHeight("220px");
        desc.setMaxHeight("320px");
        desc.setPlaceholder("Unesi opis taska...");

        DatePicker due = new DatePicker("Rok (opciono)");
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
            due.setValue(existing.getDueAt() == null ? null : existing.getDueAt().toLocalDate());
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

        Button archive = new Button("Arhiviraj", VaadinIcon.ARCHIVE.create());
        archive.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        archive.setVisible(isEdit && canWrite);
        archive.setEnabled(isEdit && canWrite);

        archive.addClickListener(e -> {
            ConfirmDialog cd = new ConfirmDialog();
            cd.setHeader("Arhivirati task?");
            cd.setText("Task neće biti trajno obrisan. Biće premješten u arhivu taskova i može se kasnije vratiti.");
            cd.setCancelable(true);
            cd.setConfirmText("Arhiviraj");
            cd.setConfirmButtonTheme("error primary");

            cd.addConfirmListener(ev -> {
                try {
                    services.cardService.archiveCard(existing.getId(), actorUserId, currentCardVersion);
                    taskChanged = true;
                    close();
                    Notification.show("Task je arhiviran.");

                    if (!hasTaskChangedCallback) {
                        MainView.getMainView().setContent(new BoardView(boardId));
                    }
                } catch (Exception ex) {
                    if (isTaskConflict(ex)) {
                        handleTaskConflict(isEdit ? existing.getId() : null);
                        return;
                    }
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });

            cd.open();
        });

        Button cancel = new Button("Otkaži", e -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        save.setEnabled(canWrite);

        save.addClickListener(e -> {
            try {
                LocalDateTime dueVal = endOfDueDate(due.getValue());
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
                    Card saved = services.cardService.updateCard(
                            existing.getId(),
                            actorUserId,
                            title.getValue(),
                            desc.getValue(),
                            dueVal,
                            pr,
                            assigneeId,
                            currentCardVersion
                    );
                    currentCardVersion = normalizeVersion(saved.getVersion());
                }

                taskChanged = true;

                close();
                Notification.show("Sačuvano.");

                if (!hasTaskChangedCallback) {
                    MainView.getMainView().setContent(new BoardView(boardId));
                }

            } catch (Exception ex) {
                if (isTaskConflict(ex)) {
                    handleTaskConflict(isEdit ? existing.getId() : null);
                    return;
                }
                Notification.show(ex.getMessage());
            }
        });

        HorizontalLayout actions = new HorizontalLayout(save, archive, cancel);
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

        VerticalLayout presenceCard = buildPresenceSection(existing.getId(), canWrite);
        applyCardStyle(presenceCard);

        VerticalLayout labelsCard = buildLabelsSection(existing.getId(), canWrite);
        applyCardStyle(labelsCard);

        VerticalLayout checklistCard = buildChecklistSection(existing.getId(), canWrite);
        applyCardStyle(checklistCard);

        VerticalLayout attachmentsCard = buildAttachmentsSection(existing.getId(), canWrite);
        applyCardStyle(attachmentsCard);

        VerticalLayout commentsCard = buildCommentsSection(existing.getId(), canWrite);
        applyCardStyle(commentsCard);

        VerticalLayout activityCard = buildActivitySection(existing.getId());
        applyCardStyle(activityCard);

        VerticalLayout root = new VerticalLayout(presenceCard, detailsCard, labelsCard, checklistCard, attachmentsCard, commentsCard, activityCard);
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        Scroller scroller = new Scroller(root);
        scroller.setWidthFull();
        scroller.setHeightFull();

        add(scroller);

        registerRealtime(existing.getId());
    }

    private void notifyTaskChangedIfNeeded() {
        if (!taskChanged) {
            return;
        }

        taskChanged = false;
        onTaskChanged.run();
    }

    private void refreshCurrentCardVersion(Long cardId) {
        if (cardId == null) {
            currentCardVersion = null;
            return;
        }

        currentCardVersion = normalizeVersion(services.cardService.requireById(cardId).getVersion());
    }

    private void handleTaskConflict(Long cardId) {
        Notification.show(
                "Task je u međuvremenu promijenjen od strane drugog korisnika. Osvježavam prikaz taska.",
                6000,
                Notification.Position.MIDDLE
        );

        if (cardId != null) {
            try {
                refreshCurrentCardVersion(cardId);
                refreshAllSections();
            } catch (Exception ignored) {
                // Ako je task obrisan ili više nije dostupan, samo zatvaramo dialog i osvježavamo board.
            }
        }

        taskChanged = true;
        close();
    }

    private static boolean isTaskConflict(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof TaskVersionConflictException) {
                return true;
            }

            String className = t.getClass().getName();
            if (className.contains("OptimisticLock") || className.contains("StaleObjectState")) {
                return true;
            }

            t = t.getCause();
        }
        return false;
    }

    private static Long normalizeVersion(Long version) {
        return version == null ? 0L : version;
    }

    /**
     * UI prima samo datum roka. U bazi i dalje čuvamo LocalDateTime,
     * pa datum pretvaramo u kraj tog dana. Tako task nije overdue tokom
     * samog dana roka, nego tek narednog dana.
     */
    private static LocalDateTime endOfDueDate(LocalDate date) {
        return date == null ? null : date.atTime(23, 59);
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
            unregisterPresence();
        });
    }

    private void refreshAllSections() {
        refreshActivitySection();

        if (attachmentsFilesWrap != null && currentActivityCardId != null) {
            refreshAttachmentsList(attachmentsFilesWrap, currentActivityCardId, attachmentsCanWrite);
        }

        if (reloadChecklist != null) {
            reloadChecklist.run();
        }

        if (reloadLabels != null) {
            reloadLabels.run();
        }

        if (reloadComments != null) {
            reloadComments.run();
        }
    }

    private VerticalLayout buildPresenceSection(Long cardId, boolean canWrite) {
        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        Span hint = new Span(
                canWrite
                        ? "Ovdje vidiš ko trenutno ima otvoren isti task. Ovo pomaže da se izbjegnu paralelne izmjene bez dogovora."
                        : "Ovdje vidiš ko trenutno pregleda isti task. VIEWER korisnik je označen kao pregled."
        );
        hint.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        presenceWrap = new Div();
        presenceWrap.setWidthFull();
        presenceWrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("box-sizing", "border-box")
                .set("background", "white");

        root.add(
                buildSectionHeader("Prisustvo na tasku", "Korisnici koji trenutno imaju otvoren ovaj task."),
                hint,
                presenceWrap
        );

        registerPresence(cardId, canWrite);
        renderPresence(CardPresenceBus.snapshot(cardId));

        return root;
    }

    private void registerPresence(Long cardId, boolean canWrite) {
        if (cardId == null || presenceRegistration != null) {
            return;
        }

        User actor = requireActorUser();
        String displayName = actor.getFullName();
        if (displayName == null || displayName.isBlank()) {
            displayName = actor.getEmail();
        }

        CardPresenceBus.PresenceMode mode = canWrite
                ? CardPresenceBus.PresenceMode.EDITING
                : CardPresenceBus.PresenceMode.VIEWING;

        presenceRegistration = CardPresenceBus.register(
                cardId,
                actor.getId(),
                displayName,
                actor.getEmail(),
                mode,
                users -> getUI().ifPresent(ui -> ui.access(() -> renderPresence(users)))
        );
    }

    private void unregisterPresence() {
        if (presenceRegistration != null) {
            presenceRegistration.remove();
            presenceRegistration = null;
        }
    }

    private void renderPresence(List<CardPresenceBus.PresenceUser> users) {
        if (presenceWrap == null) {
            return;
        }

        presenceWrap.removeAll();

        List<CardPresenceBus.PresenceUser> visibleUsers = users == null ? List.of() : users;

        long otherUsers = visibleUsers.stream()
                .filter(u -> u.getUserId() == null || !u.getUserId().equals(actorUserId))
                .count();

        Span summary = new Span(
                otherUsers == 0
                        ? "Samo ti trenutno imaš otvoren ovaj task."
                        : "Još " + otherUsers + " korisnik(a) trenutno ima otvoren ovaj task."
        );
        summary.getStyle()
                .set("display", "block")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("margin-bottom", "8px");

        HorizontalLayout chips = new HorizontalLayout();
        chips.setPadding(false);
        chips.setSpacing(true);
        chips.setWidthFull();
        chips.getStyle()
                .set("flex-wrap", "wrap")
                .set("gap", "8px");

        for (CardPresenceBus.PresenceUser user : visibleUsers) {
            chips.add(buildPresenceChip(user));
        }

        presenceWrap.add(summary, chips);
    }

    private Span buildPresenceChip(CardPresenceBus.PresenceUser user) {
        String name = user.getDisplayName();
        if (name == null || name.isBlank()) {
            name = user.getEmail();
        }
        if (name == null || name.isBlank()) {
            name = "Nepoznat korisnik";
        }

        boolean self = user.getUserId() != null && user.getUserId().equals(actorUserId);
        boolean editing = user.getMode() == CardPresenceBus.PresenceMode.EDITING;

        String text = name + (self ? " (ti)" : "") + " · " + (editing ? "može da uređuje" : "samo pregleda");

        Span chip = new Span(text);
        chip.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("background", editing ? "var(--lumo-warning-color-10pct)" : "var(--lumo-primary-color-10pct)")
                .set("color", editing ? "var(--lumo-warning-text-color)" : "var(--lumo-primary-text-color)");

        return chip;
    }

    private VerticalLayout buildActivitySection(Long cardId) {
        this.currentActivityCardId = cardId;

        activityGrid = new Grid<>(CardActivity.class, false);
        activityGrid.setWidthFull();
        activityGrid.setHeight("280px");

        activityGrid.addColumn(a -> formatDateTime(a.getCreatedAt()))
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

    private VerticalLayout buildLabelsSection(Long cardId, boolean canWrite) {
        this.labelsCanWrite = canWrite;

        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        Span hint = new Span(
                canWrite
                        ? "Dodaj ili označi labele za lakšu organizaciju taskova."
                        : "Možeš pregledati labele, ali nemaš pravo dodavanja ili izmjena."
        );
        hint.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        labelsWrap = new Div();
        labelsWrap.setWidthFull();
        labelsWrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("box-sizing", "border-box")
                .set("background", "white");

        root.add(buildSectionHeader("Labele", "Kategorizacija taska bojama i oznakama."), hint);

        if (canWrite) {
            TextField newLabel = new TextField();
            newLabel.setPlaceholder("Nova labela...");
            newLabel.setWidthFull();
            newLabel.setClearButtonVisible(true);

            Select<String> color = new Select<>();
            color.setLabel("Boja");
            color.setItems("BLUE", "GREEN", "YELLOW", "RED", "PURPLE", "GRAY");
            color.setValue("BLUE");
            color.setWidth("180px");
            color.setItemLabelGenerator(c -> switch (c) {
                case "GREEN" -> "Zelena";
                case "YELLOW" -> "Žuta";
                case "RED" -> "Crvena";
                case "PURPLE" -> "Ljubičasta";
                case "GRAY" -> "Siva";
                default -> "Plava";
            });

            Button add = new Button("Dodaj labelu", e -> {
                try {
                    services.cardLabelService.createAndAssignLabel(
                            cardId,
                            actorUserId,
                            newLabel.getValue(),
                            color.getValue(),
                            currentCardVersion
                    );
                    refreshCurrentCardVersion(cardId);
                    taskChanged = true;
                    newLabel.clear();
                    color.setValue("BLUE");
                    refreshLabelsList(cardId, labelsCanWrite);
                    refreshActivitySection();
                    Notification.show("Labela je dodana.");
                } catch (Exception ex) {
                    if (isTaskConflict(ex)) {
                        handleTaskConflict(cardId);
                        return;
                    }
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });
            add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            HorizontalLayout addRow = new HorizontalLayout(newLabel, color, add);
            addRow.setWidthFull();
            addRow.setPadding(false);
            addRow.setSpacing(true);
            addRow.setDefaultVerticalComponentAlignment(Alignment.END);
            addRow.setFlexGrow(1, newLabel);

            root.add(addRow);
        }

        reloadLabels = () -> refreshLabelsList(cardId, labelsCanWrite);
        reloadLabels.run();

        root.add(labelsWrap);
        return root;
    }

    private void refreshLabelsList(Long cardId, boolean canWrite) {
        if (labelsWrap == null) {
            return;
        }

        labelsWrap.removeAll();

        try {
            List<CardLabel> boardLabels = services.cardLabelService.listLabelsForBoard(boardId, actorUserId);
            List<CardLabel> assignedLabels = services.cardLabelService.listLabelsForCard(cardId, actorUserId);

            Set<Long> assignedIds = assignedLabels.stream()
                    .map(CardLabel::getId)
                    .collect(Collectors.toSet());

            if (boardLabels.isEmpty()) {
                Span empty = new Span(canWrite
                        ? "Nema labela na ovom boardu. Dodaj prvu labelu iznad."
                        : "Nema labela za prikaz.");
                empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
                labelsWrap.add(empty);
                return;
            }

            VerticalLayout list = new VerticalLayout();
            list.setPadding(false);
            list.setSpacing(true);
            list.setWidthFull();

            int visibleCount = 0;

            for (CardLabel label : boardLabels) {
                boolean assigned = assignedIds.contains(label.getId());

                if (!canWrite && !assigned) {
                    continue;
                }

                visibleCount++;

                Checkbox selected = new Checkbox();
                selected.setValue(assigned);
                selected.setEnabled(canWrite);

                selected.addValueChangeListener(e -> {
                    try {
                        if (Boolean.TRUE.equals(e.getValue())) {
                            services.cardLabelService.assignLabel(cardId, actorUserId, label.getId(), currentCardVersion);
                        } else {
                            services.cardLabelService.removeLabel(cardId, actorUserId, label.getId(), currentCardVersion);
                        }
                        refreshCurrentCardVersion(cardId);
                        taskChanged = true;
                        refreshLabelsList(cardId, canWrite);
                        refreshActivitySection();
                    } catch (Exception ex) {
                        if (isTaskConflict(ex)) {
                            handleTaskConflict(cardId);
                            return;
                        }
                        Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                        refreshLabelsList(cardId, canWrite);
                    }
                });

                HorizontalLayout row = new HorizontalLayout(selected, buildLabelChip(label));
                row.setPadding(false);
                row.setSpacing(true);
                row.setAlignItems(Alignment.CENTER);
                row.getStyle()
                        .set("padding", "4px 0")
                        .set("box-sizing", "border-box");

                list.add(row);
            }

            if (visibleCount == 0) {
                Span empty = new Span("Task nema dodijeljene labele.");
                empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
                labelsWrap.add(empty);
            } else {
                labelsWrap.add(list);
            }
        } catch (Exception ex) {
            Notification.show("Ne mogu učitati labele: " + ex.getMessage(),
                    4000, Notification.Position.MIDDLE);
        }
    }

    private Span buildLabelChip(CardLabel label) {
        Span chip = new Span(label.getName());
        chip.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("padding", "3px 10px")
                .set("border-radius", "999px")
                .set("background", labelBackground(label.getColor()))
                .set("color", labelTextColor(label.getColor()))
                .set("border", "1px solid " + labelBorderColor(label.getColor()));
        return chip;
    }

    private static String labelBackground(String color) {
        return switch (color == null ? "BLUE" : color) {
            case "GREEN" -> "var(--lumo-success-color-10pct)";
            case "YELLOW" -> "var(--lumo-warning-color-10pct)";
            case "RED" -> "var(--lumo-error-color-10pct)";
            case "PURPLE" -> "var(--lumo-primary-color-10pct)";
            case "GRAY" -> "var(--lumo-contrast-10pct)";
            default -> "var(--lumo-primary-color-10pct)";
        };
    }

    private static String labelTextColor(String color) {
        return switch (color == null ? "BLUE" : color) {
            case "GREEN" -> "var(--lumo-success-text-color)";
            case "YELLOW" -> "var(--lumo-warning-text-color)";
            case "RED" -> "var(--lumo-error-text-color)";
            case "GRAY" -> "var(--lumo-secondary-text-color)";
            default -> "var(--lumo-primary-text-color)";
        };
    }

    private static String labelBorderColor(String color) {
        return switch (color == null ? "BLUE" : color) {
            case "GREEN" -> "var(--lumo-success-color-30pct)";
            case "YELLOW" -> "var(--lumo-warning-color-30pct)";
            case "RED" -> "var(--lumo-error-color-30pct)";
            case "GRAY" -> "var(--lumo-contrast-20pct)";
            default -> "var(--lumo-primary-color-30pct)";
        };
    }


    private VerticalLayout buildChecklistSection(Long cardId, boolean canWrite) {
        this.checklistCanWrite = canWrite;

        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        Span hint = new Span(
                canWrite
                        ? "Dodaj manje korake koje treba završiti unutar ovog taska."
                        : "Možeš pregledati checklist, ali nemaš pravo dodavanja ili izmjena."
        );
        hint.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        TextField newItem = new TextField();
        newItem.setPlaceholder("Nova checklist stavka...");
        newItem.setWidthFull();
        newItem.setClearButtonVisible(true);
        newItem.setEnabled(canWrite);

        Button add = new Button("Dodaj", e -> {
            try {
                services.cardChecklistService.addItem(cardId, actorUserId, newItem.getValue(), currentCardVersion);
                refreshCurrentCardVersion(cardId);
                taskChanged = true;
                newItem.clear();
                refreshChecklistList(cardId, checklistCanWrite);
                refreshActivitySection();
                Notification.show("Checklist stavka je dodana.");
            } catch (Exception ex) {
                if (isTaskConflict(ex)) {
                    handleTaskConflict(cardId);
                    return;
                }
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.setEnabled(canWrite);

        HorizontalLayout addRow = new HorizontalLayout(newItem, add);
        addRow.setWidthFull();
        addRow.setPadding(false);
        addRow.setSpacing(true);
        addRow.setDefaultVerticalComponentAlignment(Alignment.END);
        addRow.setFlexGrow(1, newItem);

        checklistItemsWrap = new Div();
        checklistItemsWrap.setWidthFull();
        checklistItemsWrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("box-sizing", "border-box")
                .set("overflow", "hidden")
                .set("background", "white");

        reloadChecklist = () -> refreshChecklistList(cardId, checklistCanWrite);
        reloadChecklist.run();

        root.add(
                buildSectionHeader("Checklist", "Manji koraci i podzadaci unutar taska."),
                hint,
                addRow,
                checklistItemsWrap
        );

        return root;
    }

    private void refreshChecklistList(Long cardId, boolean canWrite) {
        if (checklistItemsWrap == null) {
            return;
        }

        checklistItemsWrap.removeAll();

        try {
            List<CardChecklistItem> items = services.cardChecklistService.listForCard(cardId, actorUserId);

            if (items.isEmpty()) {
                Span empty = new Span("Nema checklist stavki.");
                empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
                checklistItemsWrap.add(empty);
                return;
            }

            long doneCount = items.stream().filter(CardChecklistItem::isDone).count();

            Span progress = new Span("Završeno: " + doneCount + " / " + items.size());
            progress.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("font-weight", "700")
                    .set("display", "block")
                    .set("margin-bottom", "8px");
            checklistItemsWrap.add(progress);

            for (CardChecklistItem item : items) {
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

                Checkbox done = new Checkbox();
                done.setValue(item.isDone());
                done.setEnabled(canWrite);

                Span title = new Span(item.getTitle());
                title.getStyle()
                        .set("font-size", "var(--lumo-font-size-s)")
                        .set("word-break", "break-word");

                if (item.isDone()) {
                    title.getStyle()
                            .set("text-decoration", "line-through")
                            .set("color", "var(--lumo-secondary-text-color)");
                }

                done.addValueChangeListener(e -> {
                    try {
                        services.cardChecklistService.setDone(item.getId(), actorUserId, Boolean.TRUE.equals(e.getValue()), currentCardVersion);
                        refreshCurrentCardVersion(cardId);
                        taskChanged = true;
                        refreshChecklistList(cardId, canWrite);
                        refreshActivitySection();
                    } catch (Exception ex) {
                        if (isTaskConflict(ex)) {
                            handleTaskConflict(cardId);
                            return;
                        }
                        Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                        refreshChecklistList(cardId, canWrite);
                    }
                });

                HorizontalLayout left = new HorizontalLayout(done, title);
                left.setPadding(false);
                left.setSpacing(true);
                left.setAlignItems(Alignment.CENTER);
                left.setWidthFull();
                left.expand(title);

                HorizontalLayout actions = new HorizontalLayout();
                actions.setPadding(false);
                actions.setSpacing(true);
                actions.setAlignItems(Alignment.CENTER);
                actions.getStyle().set("flex-shrink", "0");

                if (canWrite) {
                    Button delete = new Button("Obriši");
                    delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
                    delete.addClickListener(e -> {
                        try {
                            services.cardChecklistService.deleteItem(item.getId(), actorUserId, currentCardVersion);
                            refreshCurrentCardVersion(cardId);
                            taskChanged = true;
                            refreshChecklistList(cardId, true);
                            refreshActivitySection();
                            Notification.show("Checklist stavka je obrisana.");
                        } catch (Exception ex) {
                            if (isTaskConflict(ex)) {
                                handleTaskConflict(cardId);
                                return;
                            }
                            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                        }
                    });
                    actions.add(delete);
                }

                row.add(left, actions);
                row.expand(left);
                checklistItemsWrap.add(row);
            }
        } catch (Exception ex) {
            Notification.show("Ne mogu učitati checklist: " + ex.getMessage(),
                    4000, Notification.Position.MIDDLE);
        }
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

        com.vaadin.flow.component.Component uploadArea;

        if (canWrite) {
            Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
                try {
                    if (!canWrite) {
                        throw new IllegalStateException("Nemaš pravo dodavanja priloga.");
                    }

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
            upload.setEnabled(true);
            upload.addClassName("attachments-upload");

            uploadArea = upload;

        } else {
            Div disabledUpload = new Div();
            disabledUpload.setWidthFull();
            disabledUpload.getStyle()
                    .set("border", "1px dashed var(--lumo-contrast-20pct)")
                    .set("border-radius", "12px")
                    .set("padding", "20px 16px")
                    .set("box-sizing", "border-box")
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("opacity", "0.75")
                    .set("pointer-events", "none");

            Button disabledUploadBtn = new Button("Upload Files...");
            disabledUploadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            disabledUploadBtn.setEnabled(false);

            Span disabledText = new Span("Upload nije dozvoljen za VIEWER korisnika.");
            disabledText.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-s)");

            HorizontalLayout disabledRow = new HorizontalLayout(disabledUploadBtn, disabledText);
            disabledRow.setPadding(false);
            disabledRow.setSpacing(true);
            disabledRow.setDefaultVerticalComponentAlignment(Alignment.CENTER);

            disabledUpload.add(disabledRow);
            uploadArea = disabledUpload;
        }

        refreshAttachmentsList(attachmentsFilesWrap, cardId, canWrite);

        root.add(
                buildSectionHeader("Prilozi", "Dokumenti i ostali fajlovi vezani za task."),
                hint,
                uploadArea,
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
                    deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

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

    private static String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "—";
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

    private void addDialogCloseButton() {
        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> close());

        closeButton.addThemeVariants(
                ButtonVariant.LUMO_TERTIARY_INLINE,
                ButtonVariant.LUMO_ICON
        );

        closeButton.setAriaLabel("Zatvori");
        closeButton.getStyle()
                .set("margin-left", "auto")
                .set("cursor", "pointer");

        getHeader().add(closeButton);
    }
}
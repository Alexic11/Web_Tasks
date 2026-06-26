package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.board.BoardRealtimeBus;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.card.checklist.CardChecklistService;
import carobnifrulas.web_tasks.card.label.CardLabel;
import carobnifrulas.web_tasks.list.ListEntity;
import carobnifrulas.web_tasks.ui.MainView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class BoardView extends View {

    public enum BackTarget {
        BOARDS,
        HISTORY
    }

    private final Long boardId;
    private final AtomicLong draggedCardId = new AtomicLong(-1L);
    private final FilterState filterState;
    private final BackTarget backTarget;

    private static final class FilterState {
        Long assigneeId;
        Integer priority;
        Long labelId;
        boolean overdueOnly;
        String titleQuery;

        FilterState copy() {
            FilterState c = new FilterState();
            c.assigneeId = this.assigneeId;
            c.priority = this.priority;
            c.labelId = this.labelId;
            c.overdueOnly = this.overdueOnly;
            c.titleQuery = this.titleQuery;
            return c;
        }
    }

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public BoardView(Long boardId) {
        this(boardId, new FilterState(), BackTarget.BOARDS);
    }

    public BoardView(Long boardId, BackTarget backTarget) {
        this(boardId, new FilterState(), backTarget);
    }

    private BoardView(Long boardId, FilterState state) {
        this(boardId, state, BackTarget.BOARDS);
    }

    private BoardView(Long boardId, FilterState state, BackTarget backTarget) {
        this.boardId = boardId;
        this.filterState = state == null ? new FilterState() : state;
        this.backTarget = backTarget == null ? BackTarget.BOARDS : backTarget;
    }

    @Override
    public void setElements() {
        var board = services.boardService.requireMemberBoard(boardId, loggedUser.getId());

        boolean archived = board.getArchivedAt() != null;

        BoardRole myRole = services.boardMemberService.getRole(boardId, loggedUser.getId());
        boolean canManageMembers = (myRole == BoardRole.OWNER || myRole == BoardRole.ADMIN);

        boolean isGlobalAdmin = carobnifrulas.web_tasks.security.model.SecurityUtils.isGlobalAdmin(loggedUser);

        boolean canWrite = (myRole != BoardRole.VIEWER) && !archived;
        boolean canArchive = !archived && (isGlobalAdmin || myRole == BoardRole.OWNER || myRole == BoardRole.ADMIN);

        Map<Long, String> assigneeLabel =
                services.boardMemberService.listAssignees(boardId).stream()
                        .collect(Collectors.toMap(
                                BoardMemberRepository.AssigneeRow::getUserId,
                                r -> {
                                    String fn = r.getFullName() == null ? "" : r.getFullName().trim();
                                    if (!fn.isBlank()) {
                                        return fn + " (" + r.getEmail() + ")";
                                    }
                                    return r.getEmail();
                                },
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));

        List<CardLabel> boardLabels = services.cardLabelService.listLabelsForBoard(boardId, loggedUser.getId());

        add(buildHeaderSection(board.getName(), myRole, archived, canManageMembers, canArchive));

        if (archived) {
            Paragraph p = new Paragraph("Ovaj board je zatvoren (History režim) — samo pregled.");
            p.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("margin", "0");
            add(p);
        } else if (!canWrite) {
            Paragraph p = new Paragraph("VIEWER režim: možeš samo pregledati board.");
            p.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("margin", "0");
            add(p);
        }

        FlexLayout columns = new FlexLayout();
        columns.setWidthFull();
        columns.setFlexWrap(FlexLayout.FlexWrap.NOWRAP);
        columns.getStyle()
                .set("display", "flex")
                .set("gap", "16px")
                .set("align-items", "flex-start");

        Span count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        Runnable refreshColumns = () -> {
            try {
                columns.removeAll();

                List<ListEntity> lists = services.listService.findByBoard(boardId);

                Map<Long, List<Card>> cardsByList = new LinkedHashMap<>();
                for (ListEntity l : lists) {
                    cardsByList.put(l.getId(), services.cardService.findByList(l.getId()));
                }

                int totalCount = cardsByList.values().stream().mapToInt(List::size).sum();

                List<Long> cardIds = cardsByList.values().stream()
                        .flatMap(List::stream)
                        .map(Card::getId)
                        .toList();

                Map<Long, CardChecklistService.ChecklistStats> checklistStats =
                        services.cardChecklistService.statsForCards(cardIds);

                Map<Long, List<CardLabel>> labelsByCard =
                        services.cardLabelService.labelsByCard(cardIds, loggedUser.getId());

                int matchCount = 0;
                for (int i = 0; i < lists.size(); i++) {
                    Long listId = lists.get(i).getId();
                    for (Card c : cardsByList.getOrDefault(listId, List.of())) {
                        if (matchesFilters(c, i, lists.size(), labelsByCard)) {
                            matchCount++;
                        }
                    }
                }

                count.setText("Prikaz: " + matchCount + " / " + totalCount);

                for (int i = 0; i < lists.size(); i++) {
                    ListEntity list = lists.get(i);
                    columns.add(buildColumn(list, lists, i, canWrite, assigneeLabel, cardsByList, checklistStats, labelsByCard));
                }
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        };

        add(buildFilterBar(assigneeLabel, boardLabels, count, refreshColumns));

        Registration boardRealtimeRegistration = BoardRealtimeBus.register(boardId, changeType ->
                getUI().ifPresent(ui -> ui.access(refreshColumns::run))
        );

        addDetachListener(e -> boardRealtimeRegistration.remove());

        refreshColumns.run();

        Scroller scroller = new Scroller(columns);
        scroller.setWidthFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.HORIZONTAL);
        scroller.getStyle().set("padding-bottom", "12px");

        add(scroller);
    }

    private com.vaadin.flow.component.Component buildHeaderSection(String boardName,
                                                                   BoardRole myRole,
                                                                   boolean archived,
                                                                   boolean canManageMembers,
                                                                   boolean canArchive) {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "18px")
                .set("padding", "18px")
                .set("background", "linear-gradient(to right, var(--lumo-primary-color-10pct), white)");

        Button back = new Button("Nazad", e -> {
            if (backTarget == BackTarget.HISTORY) {
                MainView.getMainView().setContent(new ArchivedBoardsView());
            } else {
                MainView.getMainView().setContent(new BoardsView());
            }
        });
        back.setIcon(VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        H2 title = new H2(boardName);
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph("Pregled boarda, taskova i timske saradnje.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout leftText = new VerticalLayout(title, subtitle);
        leftText.setPadding(false);
        leftText.setSpacing(false);

        HorizontalLayout left = new HorizontalLayout(back, leftText);
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);

        Span roleBadge = buildRoleBadge(myRole.name());

        HorizontalLayout right = new HorizontalLayout();
        right.setSpacing(true);
        right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        if (archived) {
            Span archivedBadge = new Span("ARCHIVED");
            archivedBadge.getStyle()
                    .set("padding", "4px 10px")
                    .set("border-radius", "999px")
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("font-weight", "700");
            right.add(archivedBadge);
        }

        Button membersBtn = new Button("Members", VaadinIcon.USERS.create());
        membersBtn.addClickListener(e -> {
            BoardMembersDialog dialog = new BoardMembersDialog(
                    boardId,
                    loggedUser.getId(),
                    services,
                    () -> MainView.getMainView().setContent(
                            new BoardView(boardId, filterState.copy(), backTarget)
                    )
            );

            dialog.open();
        });
        membersBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        membersBtn.setVisible(canManageMembers && !archived);

        Button archivedTasksBtn = new Button("Arhivirani taskovi", VaadinIcon.ARCHIVE.create());
        archivedTasksBtn.addClickListener(e -> {
            ArchivedCardsDialog dialog = new ArchivedCardsDialog(
                    boardId,
                    loggedUser.getId(),
                    services,
                    () -> MainView.getMainView().setContent(new BoardView(boardId, filterState.copy(), backTarget))
            );
            dialog.open();
        });
        archivedTasksBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        archivedTasksBtn.setVisible(!archived);

        Button closeBoard = new Button("Zatvori", VaadinIcon.LOCK.create());
        closeBoard.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeBoard.setVisible(canArchive);

        closeBoard.addClickListener(e -> {
            long openCnt;
            try {
                openCnt = services.cardService.countOpenTasks(boardId);
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
                return;
            }

            if (openCnt > 0) {
                ConfirmDialog info = new ConfirmDialog();
                info.setHeader("Ne možeš zatvoriti board");
                info.setText("Board '" + boardName + "' ima još otvorenih taskova: " + openCnt +
                        ". Premjesti sve taskove u Done pa pokušaj ponovo.");
                info.setConfirmText("OK");
                info.setConfirmButtonTheme("primary");
                info.setCancelable(false);
                info.open();
                return;
            }

            ConfirmDialog cd = new ConfirmDialog();
            cd.setHeader("Zatvori board?");
            cd.setText("Jesi li siguran da želiš zatvoriti board '" + boardName + "' ? Board će preći u History.");
            cd.setCancelable(true);
            cd.setConfirmText("Zatvori");
            cd.setConfirmButtonTheme("error primary");

            cd.addConfirmListener(ev -> {
                try {
                    services.boardService.archiveBoard(boardId, loggedUser.getId());
                    Notification.show("Board '" + boardName + "' je zatvoren.");
                    MainView.getMainView().setContent(new BoardsView());
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });

            cd.open();
        });

        right.add(roleBadge, archivedTasksBtn, membersBtn, closeBoard);

        HorizontalLayout top = new HorizontalLayout(left, right);
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        wrap.add(top);
        return wrap;
    }

    private com.vaadin.flow.component.Component buildColumn(ListEntity list,
                                                            List<ListEntity> allLists,
                                                            int idx,
                                                            boolean canWrite,
                                                            Map<Long, String> assigneeLabel,
                                                            Map<Long, List<Card>> cardsByList,
                                                            Map<Long, CardChecklistService.ChecklistStats> checklistStats,
                                                            Map<Long, List<CardLabel>> labelsByCard) {

        VerticalLayout col = new VerticalLayout();
        col.setPadding(false);
        col.setSpacing(true);
        col.setWidth("100%");
        col.getStyle()
                .set("flex", "1 1 360px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "14px")
                .set("padding-bottom", "100px")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)")
                .set("min-width", "320px")
                .set("box-sizing", "border-box")
                .set("transition", "all 0.2s ease");

        H3 h = new H3(list.getTitle());
        h.getStyle().set("margin", "0");

        List<Card> listCards = cardsByList.getOrDefault(list.getId(), List.of());
        long visibleCount = listCards.stream()
                .filter(c -> matchesFilters(c, idx, allLists.size(), labelsByCard))
                .count();

        Span listCount = new Span(String.valueOf(visibleCount));
        listCount.getStyle()
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700");

        HorizontalLayout leftHeader = new HorizontalLayout(h, listCount);
        leftHeader.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        leftHeader.setSpacing(true);

        Button addTask = new Button("+ Novi task",
                e -> TaskDialog.create(services, boardId, list.getId(), loggedUser.getId()).open());
        addTask.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTask.setVisible(canWrite);

        HorizontalLayout header = new HorizontalLayout(leftHeader, addTask);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        col.add(header);

        if (canWrite) {
            DropTarget<VerticalLayout> dropOnList = DropTarget.create(col);
            dropOnList.setDropEffect(DropEffect.MOVE);

            col.getElement().addEventListener("dragenter", e ->
                    col.getStyle().set("background", "var(--lumo-primary-color-10pct)")
            );

            col.getElement().addEventListener("dragleave", e ->
                    col.getStyle().set("background", "white")
            );

            dropOnList.addDropListener(e -> {
                col.getStyle().set("background", "white");

                Long movingId = draggedCardId.get();
                if (movingId == null || movingId <= 0) {
                    return;
                }

                try {
                    int endIndex = services.cardService.findByList(list.getId()).size();
                    services.cardService.reorderWithinList(movingId, list.getId(), endIndex, loggedUser.getId());
                    MainView.getMainView().setContent(new BoardView(boardId, filterState.copy(), backTarget));
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });
        }

        List<Card> cards = cardsByList.getOrDefault(list.getId(), List.of());
        for (Card c : cards) {
            if (!matchesFilters(c, idx, allLists.size(), labelsByCard)) {
                continue;
            }

            col.add(renderCard(c, list.getId(), allLists, idx, canWrite, assigneeLabel, checklistStats, labelsByCard));
        }

        return col;
    }

    private com.vaadin.flow.component.Component renderCard(Card c,
                                                           Long columnListId,
                                                           List<ListEntity> allLists,
                                                           int idx,
                                                           boolean canWrite,
                                                           Map<Long, String> assigneeLabel,
                                                           Map<Long, CardChecklistService.ChecklistStats> checklistStats,
                                                           Map<Long, List<CardLabel>> labelsByCard) {

        VerticalLayout box = new VerticalLayout();
        box.setPadding(false);
        box.setSpacing(true);
        box.getStyle()
                .set("cursor", canWrite ? "grab" : "pointer")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "14px")
                .set("background", "white")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.04)")
                .set("transition", "all 0.2s ease");

        if (c.getDueAt() != null && c.getDueAt().toLocalDate().isBefore(LocalDate.now())) {
            box.getStyle().set("background", "linear-gradient(to bottom right, var(--lumo-error-color-10pct), white)");
        }

        if (canWrite) {
            DragSource<com.vaadin.flow.component.Component> drag = DragSource.create(box);
            drag.setDragData(c.getId());
            drag.setEffectAllowed(EffectAllowed.MOVE);
            drag.addDragStartListener(e -> draggedCardId.set(c.getId()));
            drag.addDragEndListener(e -> draggedCardId.set(-1L));

            DropTarget<com.vaadin.flow.component.Component> drop = DropTarget.create(box);
            drop.setDropEffect(DropEffect.MOVE);
            drop.addDropListener(e -> {
                Long movingId = draggedCardId.get();

                if (movingId == null || movingId <= 0) {
                    return;
                }

                if (movingId.equals(c.getId())) {
                    return;
                }

                try {
                    List<Card> inList = services.cardService.findByList(columnListId);
                    int targetIdx = 0;

                    for (int i = 0; i < inList.size(); i++) {
                        if (inList.get(i).getId().equals(c.getId())) {
                            targetIdx = i;
                            break;
                        }
                    }

                    services.cardService.reorderWithinList(movingId, columnListId, targetIdx, loggedUser.getId());
                    MainView.getMainView().setContent(new BoardView(boardId, filterState.copy(), backTarget));
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });
        }

        box.addClickListener(ev -> TaskDialog.edit(
                services,
                c,
                loggedUser.getId(),
                () -> MainView.getMainView().setContent(new BoardView(boardId, filterState.copy(), backTarget))
        ).open());

        Span title = new Span(c.getTitle());
        title.getStyle()
                .set("font-weight", "700")
                .set("font-size", "var(--lumo-font-size-m)");

        String assignedTxt = "-";
        if (c.getAssignedTo() != null) {
            assignedTxt = assigneeLabel.getOrDefault(c.getAssignedTo(), String.valueOf(c.getAssignedTo()));
        }

        Span assignee = new Span("Assigned: " + assignedTxt);
        assignee.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        com.vaadin.flow.component.Component labels = buildLabelsRow(labelsByCard.getOrDefault(c.getId(), List.of()));

        Span priority = buildPriorityBadge(c.getPriority());
        Span due = buildDueLabel(c.getDueAt());
        Span checklist = buildChecklistBadge(checklistStats == null ? null : checklistStats.get(c.getId()));

        HorizontalLayout meta = new HorizontalLayout(priority, due, checklist);
        meta.setSpacing(true);
        meta.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setVisible(canWrite);

        Button take = new Button("Preuzmi", e -> {
            services.cardService.assignToMe(c.getId(), loggedUser.getId());
            MainView.getMainView().setContent(new BoardView(boardId, filterState.copy(), backTarget));
        });
        take.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button release = new Button("Pusti", e -> {
            services.cardService.unassign(c.getId(), loggedUser.getId());
            MainView.getMainView().setContent(new BoardView(boardId, filterState.copy(), backTarget));
        });
        release.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        boolean iAmAssignee = c.getAssignedTo() != null && c.getAssignedTo().equals(loggedUser.getId());

        take.setEnabled(c.getAssignedTo() == null);
        release.setEnabled(iAmAssignee);

        Button left = new Button(VaadinIcon.ARROW_LEFT.create(), e -> {
            if (idx == 0) {
                return;
            }

            try {
                services.cardService.moveToList(
                        c.getId(),
                        allLists.get(idx - 1).getId(),
                        loggedUser.getId()
                );

                MainView.getMainView().setContent(new BoardView(boardId, filterState.copy(), backTarget));
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        Button right = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> {
            if (idx >= allLists.size() - 1) {
                return;
            }

            try {
                services.cardService.moveToList(
                        c.getId(),
                        allLists.get(idx + 1).getId(),
                        loggedUser.getId()
                );

                MainView.getMainView().setContent(new BoardView(boardId, filterState.copy(), backTarget));
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        left.setEnabled(idx > 0);
        right.setEnabled(idx < allLists.size() - 1);

        left.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        right.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        actions.add(take, release, left, right);

        box.add(title, labels, assignee, meta, actions);
        return box;
    }

    private com.vaadin.flow.component.Component buildLabelsRow(List<CardLabel> labels) {
        FlexLayout row = new FlexLayout();
        row.setWidthFull();
        row.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        row.getStyle()
                .set("gap", "4px")
                .set("margin", "0");

        if (labels == null || labels.isEmpty()) {
            Span empty = new Span("Labele: —");
            empty.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-secondary-text-color)");
            row.add(empty);
            return row;
        }

        for (CardLabel label : labels) {
            row.add(buildLabelChip(label));
        }

        return row;
    }

    private Span buildLabelChip(CardLabel label) {
        Span chip = new Span(label.getName());
        chip.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "700")
                .set("padding", "2px 8px")
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

    private Span buildChecklistBadge(CardChecklistService.ChecklistStats stats) {
        if (stats == null || !stats.hasItems()) {
            Span empty = new Span("Checklist: —");
            empty.getStyle()
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("padding", "2px 10px")
                    .set("border-radius", "999px")
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("color", "var(--lumo-secondary-text-color)");
            return empty;
        }

        Span badge = new Span("Checklist: " + stats.done() + "/" + stats.total() + " (" + stats.percent() + "%)");
        badge.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "2px 10px")
                .set("border-radius", "999px")
                .set("font-weight", "600")
                .set("background", stats.done() == stats.total()
                        ? "var(--lumo-success-color-10pct)"
                        : "var(--lumo-primary-color-10pct)")
                .set("color", stats.done() == stats.total()
                        ? "var(--lumo-success-text-color)"
                        : "var(--lumo-primary-text-color)");

        return badge;
    }

    private Span buildPriorityBadge(Integer p) {
        int pr = (p == null) ? 1 : p;

        Span s = new Span("P" + pr);
        s.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "2px 10px")
                .set("border-radius", "999px")
                .set("font-weight", "600")
                .set("border", "1px solid var(--lumo-contrast-20pct)");

        switch (pr) {
            case 5 -> s.getStyle()
                    .set("background", "var(--lumo-error-color-10pct)")
                    .set("color", "var(--lumo-error-text-color)");
            case 4 -> s.getStyle()
                    .set("background", "var(--lumo-warning-color-10pct)")
                    .set("color", "var(--lumo-warning-text-color)");
            case 3 -> s.getStyle()
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("color", "var(--lumo-primary-text-color)");
            case 2 -> s.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("color", "var(--lumo-body-text-color)");
            default -> s.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");
        }

        return s;
    }

    private Span buildDueLabel(LocalDateTime dueAt) {
        String dueTxt = (dueAt == null)
                ? "Rok: —"
                : "Rok: " + DT_FMT.format(dueAt);

        Span due = new Span(dueTxt);
        due.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "2px 10px")
                .set("border-radius", "999px")
                .set("background", "var(--lumo-contrast-5pct)");

        if (dueAt != null && dueAt.toLocalDate().isBefore(LocalDate.now())) {
            due.getStyle()
                    .set("color", "var(--lumo-error-text-color)")
                    .set("font-weight", "700")
                    .set("background", "var(--lumo-error-color-10pct)");
        } else {
            due.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");
        }

        return due;
    }

    private com.vaadin.flow.component.Component buildFilterBar(Map<Long, String> assigneeLabel,
                                                               List<CardLabel> boardLabels,
                                                               Span count,
                                                               Runnable refreshColumns) {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        Select<Long> assignee = new Select<>();
        assignee.setLabel("Assignee");
        assignee.setWidth("320px");
        assignee.setEmptySelectionAllowed(true);
        assignee.setEmptySelectionCaption("Svi");

        List<Long> assItems = new ArrayList<>();
        assItems.add(-1L);
        assItems.addAll(assigneeLabel.keySet());

        assignee.setItems(assItems);

        assignee.setItemLabelGenerator(id -> {
            if (id == null) {
                return "Svi";
            }

            if (id == -1L) {
                return "— Unassigned —";
            }

            return assigneeLabel.getOrDefault(id, String.valueOf(id));
        });

        if (filterState.assigneeId != null && assItems.contains(filterState.assigneeId)) {
            assignee.setValue(filterState.assigneeId);
        } else {
            filterState.assigneeId = null;
            assignee.clear();
        }

        assignee.addValueChangeListener(e -> {
            filterState.assigneeId = e.getValue();
            refreshColumns.run();
        });

        Select<Integer> pr = new Select<>();
        pr.setLabel("Prioritet");
        pr.setWidth("200px");
        pr.setEmptySelectionAllowed(true);
        pr.setEmptySelectionCaption("Svi");
        pr.setItems(1, 2, 3, 4, 5);
        pr.setItemLabelGenerator(p -> p == null ? "Svi" : "P" + p);
        pr.setValue(filterState.priority);

        pr.addValueChangeListener(e -> {
            filterState.priority = e.getValue();
            refreshColumns.run();
        });

        Select<Long> label = new Select<>();
        label.setLabel("Labela");
        label.setWidth("220px");
        label.setEmptySelectionAllowed(true);
        label.setEmptySelectionCaption("Sve");

        List<Long> labelItems = new ArrayList<>();
        if (boardLabels != null) {
            labelItems.addAll(boardLabels.stream().map(CardLabel::getId).toList());
        }
        label.setItems(labelItems);
        label.setItemLabelGenerator(id -> {
            if (id == null) {
                return "Sve";
            }
            if (boardLabels == null) {
                return String.valueOf(id);
            }
            return boardLabels.stream()
                    .filter(l -> l.getId().equals(id))
                    .findFirst()
                    .map(CardLabel::getName)
                    .orElse(String.valueOf(id));
        });

        if (filterState.labelId != null && labelItems.contains(filterState.labelId)) {
            label.setValue(filterState.labelId);
        } else {
            filterState.labelId = null;
            label.clear();
        }

        label.addValueChangeListener(e -> {
            filterState.labelId = e.getValue();
            refreshColumns.run();
        });

        Checkbox overdue = new Checkbox("Overdue");
        overdue.setValue(filterState.overdueOnly);
        overdue.addValueChangeListener(e -> {
            filterState.overdueOnly = Boolean.TRUE.equals(e.getValue());
            refreshColumns.run();
        });

        TextField search = new TextField();
        search.setLabel("Search title");
        search.setWidthFull();
        search.setClearButtonVisible(true);
        search.setValue(filterState.titleQuery == null ? "" : filterState.titleQuery);
        search.setValueChangeMode(ValueChangeMode.TIMEOUT);
        search.setValueChangeTimeout(300);

        search.addValueChangeListener(e -> {
            filterState.titleQuery = e.getValue();
            refreshColumns.run();
        });

        Button reset = new Button("Reset", e -> {
            filterState.assigneeId = null;
            filterState.priority = null;
            filterState.labelId = null;
            filterState.overdueOnly = false;
            filterState.titleQuery = "";

            assignee.clear();
            pr.clear();
            label.clear();
            overdue.setValue(false);
            search.clear();

            refreshColumns.run();
        });
        reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(assignee, pr, label, overdue, search, reset, count);
        bar.setFlexGrow(1, search);

        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "12px")
                .set("margin-top", "8px")
                .set("background", "white");

        return bar;
    }

    private boolean matchesFilters(Card c, int listIdx, int totalLists, Map<Long, List<CardLabel>> labelsByCard) {
        if (filterState.assigneeId != null) {
            if (filterState.assigneeId == -1L) {
                if (c.getAssignedTo() != null) {
                    return false;
                }
            } else {
                if (c.getAssignedTo() == null || !filterState.assigneeId.equals(c.getAssignedTo())) {
                    return false;
                }
            }
        }

        if (filterState.priority != null) {
            int p = c.getPriority() == null ? 1 : c.getPriority();

            if (!filterState.priority.equals(p)) {
                return false;
            }
        }

        if (filterState.labelId != null) {
            List<CardLabel> labels = labelsByCard == null ? List.of() : labelsByCard.getOrDefault(c.getId(), List.of());
            boolean hasLabel = labels.stream().anyMatch(l -> filterState.labelId.equals(l.getId()));
            if (!hasLabel) {
                return false;
            }
        }

        if (filterState.overdueOnly) {
            if (c.getDueAt() == null) {
                return false;
            }

            if (!c.getDueAt().toLocalDate().isBefore(LocalDate.now())) {
                return false;
            }

            if (listIdx == totalLists - 1) {
                return false;
            }
        }

        String q = filterState.titleQuery == null ? "" : filterState.titleQuery.trim();

        if (!q.isEmpty()) {
            String t = c.getTitle() == null ? "" : c.getTitle();

            if (!t.toLowerCase().contains(q.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    private Span buildRoleBadge(String role) {
        Span badge = new Span(role);
        badge.getStyle()
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("border", "1px solid var(--lumo-contrast-10pct)");

        switch (role) {
            case "OWNER" -> badge.getStyle()
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("color", "var(--lumo-primary-text-color)");
            case "ADMIN" -> badge.getStyle()
                    .set("background", "var(--lumo-warning-color-10pct)")
                    .set("color", "var(--lumo-warning-text-color)");
            case "MEMBER" -> badge.getStyle()
                    .set("background", "var(--lumo-success-color-10pct)")
                    .set("color", "var(--lumo-success-text-color)");
            case "VIEWER" -> badge.getStyle()
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("color", "var(--lumo-secondary-text-color)");
            default -> badge.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("color", "var(--lumo-secondary-text-color)");
        }

        return badge;
    }
}
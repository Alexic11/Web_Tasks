package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.list.ListEntity;
import carobnifrulas.web_tasks.ui.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
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
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class BoardView extends View {

    private final Long boardId;
    private final AtomicLong draggedCardId = new AtomicLong(-1L);

    private final FilterState filterState;

    private static final class FilterState {
        Long assigneeId;     // null = svi, -1 = unassigned, >0 = konkretan user
        Integer priority;    // null = svi
        boolean overdueOnly;
        String titleQuery;   // null/"" = sve

        FilterState copy() {
            FilterState c = new FilterState();
            c.assigneeId = this.assigneeId;
            c.priority = this.priority;
            c.overdueOnly = this.overdueOnly;
            c.titleQuery = this.titleQuery;
            return c;
        }
    }

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public BoardView(Long boardId) {
        this(boardId, new FilterState());
    }

    private BoardView(Long boardId, FilterState state) {
        this.boardId = boardId;
        this.filterState = state == null ? new FilterState() : state;
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
                                    if (!fn.isBlank()) return fn + " (" + r.getEmail() + ")";
                                    return r.getEmail();
                                }
                        ));

        // ===== TOP BAR =====
        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout left = new HorizontalLayout();
        left.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Button back = new Button("Nazad",
                e -> MainView.getMainView().setContent(services.menu.getDefaultView()));
        back.setIcon(VaadinIcon.ARROW_LEFT.create());

        H2 title = new H2(board.getName());
        left.add(back, title);

        HorizontalLayout right = new HorizontalLayout();
        right.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Span roleBadge = new Span(myRole.name());
        roleBadge.getStyle()
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("font-size", "var(--lumo-font-size-s)");

        if (archived) {
            Span archivedBadge = new Span("ARCHIVED");
            archivedBadge.getStyle()
                    .set("padding", "4px 10px")
                    .set("border-radius", "999px")
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("font-size", "var(--lumo-font-size-s)");
            right.add(archivedBadge);
        }

        Button membersBtn = new Button("Members",
                VaadinIcon.USERS.create(),
                e -> new BoardMembersDialog(boardId, loggedUser.getId(), services).open());
        membersBtn.setVisible(canManageMembers && !archived);

        Button closeBoard = new Button("Zatvori", VaadinIcon.LOCK.create());
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
                info.setText("Board '" + board.getName() + "' ima još otvorenih taskova: " + openCnt +
                        ". Premjesti sve taskove u Done pa pokušaj ponovo.");
                info.setConfirmText("OK");
                info.setConfirmButtonTheme("primary");
                info.setCancelable(false);
                info.open();
                return;
            }

            ConfirmDialog cd = new ConfirmDialog();
            cd.setHeader("Zatvori board?");
            cd.setText("Jesi li siguran da želiš zatvoriti board '" + board.getName() + "' ? Board će preći u History.");
            cd.setCancelable(true);

            cd.setConfirmText("Zatvori");
            cd.setConfirmButtonTheme("error primary");

            cd.addConfirmListener(ev -> {
                try {
                    services.boardService.archiveBoard(boardId, loggedUser.getId());
                    Notification.show("Board '" + board.getName() + "' je zatvoren.");
                    MainView.getMainView().setContent(new BoardsView());
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });

            cd.open();
        });

        closeBoard.setVisible(canArchive);

        right.add(roleBadge, membersBtn, closeBoard);
        top.add(left, right);
        add(top);

        if (archived) {
            Paragraph p = new Paragraph("Ovaj board je zatvoren (History režim) — samo pregled.");
            p.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(p);
        } else if (!canWrite) {
            Paragraph p = new Paragraph("VIEWER režim: možeš samo pregledati board.");
            p.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(p);
        }

        // ===== COLUMNS UI =====
        HorizontalLayout columns = new HorizontalLayout();
        columns.setWidthFull();
        columns.setSpacing(true);

        // ✅ span za count koji ćemo update-ovati bez rebuild-a view-a
        Span count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        // ✅ refresh kolona (i count-a) bez MainView.setContent(...)
        Runnable refreshColumns = () -> {
            try {
                columns.removeAll();

                List<ListEntity> lists = services.listService.findByBoard(boardId);

                Map<Long, List<Card>> cardsByList = new LinkedHashMap<>();
                for (ListEntity l : lists) {
                    cardsByList.put(l.getId(), services.cardService.findByList(l.getId()));
                }

                int totalCount = cardsByList.values().stream().mapToInt(List::size).sum();

                int matchCount = 0;
                for (int i = 0; i < lists.size(); i++) {
                    Long listId = lists.get(i).getId();
                    for (Card c : cardsByList.getOrDefault(listId, List.of())) {
                        if (matchesFilters(c, i, lists.size())) matchCount++;
                    }
                }

                count.setText("Prikaz: " + matchCount + " / " + totalCount);

                for (int i = 0; i < lists.size(); i++) {
                    ListEntity list = lists.get(i);
                    columns.add(buildColumn(list, lists, i, canWrite, assigneeLabel, cardsByList));
                }
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        };

        // ===== FILTER BAR =====
        Component filterBar = buildFilterBar(assigneeLabel, count, refreshColumns);
        add(filterBar);

        // inicijalno punjenje
        refreshColumns.run();

        add(columns);
    }

    private Component buildColumn(ListEntity list,
                                  List<ListEntity> allLists,
                                  int idx,
                                  boolean canWrite,
                                  Map<Long, String> assigneeLabel,
                                  Map<Long, List<Card>> cardsByList) {

        VerticalLayout col = new VerticalLayout();
        col.setPadding(true);
        col.setSpacing(true);
        col.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "12px")
                .set("min-width", "340px");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H3 h = new H3(list.getTitle());

        Button addTask = new Button("+ Novi task",
                e -> TaskDialog.create(services, boardId, list.getId(), loggedUser.getId()).open());
        addTask.setVisible(canWrite);

        header.add(h, addTask);
        col.add(header);

        // ✅ Drop na samu listu => ide na kraj liste (samo ako može pisati)
        if (canWrite) {
            DropTarget<VerticalLayout> dropOnList = DropTarget.create(col);
            dropOnList.setDropEffect(DropEffect.MOVE);
            dropOnList.addDropListener(e -> {
                Long movingId = draggedCardId.get();
                if (movingId == null || movingId <= 0) return;

                try {
                    int endIndex = services.cardService.findByList(list.getId()).size();
                    services.cardService.reorderWithinList(movingId, list.getId(), endIndex, loggedUser.getId());
                    MainView.getMainView().setContent(new BoardView(boardId, filterState.copy()));
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });
        }

        List<Card> cards = cardsByList.getOrDefault(list.getId(), List.of());
        for (Card c : cards) {
            if (!matchesFilters(c, idx, allLists.size())) continue;
            col.add(renderCard(c, list.getId(), allLists, idx, canWrite, assigneeLabel));
        }

        return col;
    }

    private Component renderCard(Card c,
                                 Long columnListId,
                                 List<ListEntity> allLists,
                                 int idx,
                                 boolean canWrite,
                                 Map<Long, String> assigneeLabel) {

        VerticalLayout box = new VerticalLayout();
        box.setPadding(true);
        box.setSpacing(false);
        box.getStyle()
                .set("cursor", canWrite ? "grab" : "pointer")
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "12px");

        if (canWrite) {
            DragSource<Component> drag = DragSource.create(box);
            drag.setDragData(c.getId());
            drag.setEffectAllowed(EffectAllowed.MOVE);
            drag.addDragStartListener(e -> draggedCardId.set(c.getId()));
            drag.addDragEndListener(e -> draggedCardId.set(-1L));

            DropTarget<Component> drop = DropTarget.create(box);
            drop.setDropEffect(DropEffect.MOVE);
            drop.addDropListener(e -> {
                Long movingId = draggedCardId.get();
                if (movingId == null || movingId <= 0) return;
                if (movingId.equals(c.getId())) return;

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
                    MainView.getMainView().setContent(new BoardView(boardId, filterState.copy()));
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });
        }

        box.addClickListener(ev -> TaskDialog.edit(services, c, loggedUser.getId()).open());

        Span title = new Span(c.getTitle());
        title.getStyle().set("font-weight", "700");

        String assignedTxt = "-";
        if (c.getAssignedTo() != null) {
            assignedTxt = assigneeLabel.getOrDefault(c.getAssignedTo(), String.valueOf(c.getAssignedTo()));
        }

        Span assignee = new Span("Assigned: " + assignedTxt);
        assignee.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Span priority = buildPriorityBadge(c.getPriority());
        Span due = buildDueLabel(c.getDueAt());

        HorizontalLayout meta = new HorizontalLayout(priority, due);
        meta.setSpacing(true);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setVisible(canWrite);

        Button take = new Button("Preuzmi", e -> {
            services.cardService.assignToMe(c.getId(), loggedUser.getId());
            MainView.getMainView().setContent(new BoardView(boardId, filterState.copy()));
        });

        Button release = new Button("Pusti", e -> {
            services.cardService.unassign(c.getId(), loggedUser.getId());
            MainView.getMainView().setContent(new BoardView(boardId, filterState.copy()));
        });

        boolean iAmAssignee = c.getAssignedTo() != null && c.getAssignedTo().equals(loggedUser.getId());
        take.setEnabled(c.getAssignedTo() == null);
        release.setEnabled(iAmAssignee);

        Button left = new Button(VaadinIcon.ARROW_LEFT.create(), e -> {
            if (idx == 0) return;

            try {
                services.cardService.moveToList(
                        c.getId(),
                        allLists.get(idx - 1).getId(),
                        loggedUser.getId()
                );
                MainView.getMainView().setContent(new BoardView(boardId, filterState.copy()));
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        Button right = new Button(VaadinIcon.ARROW_RIGHT.create(), e -> {
            if (idx >= allLists.size() - 1) return;

            try {
                services.cardService.moveToList(
                        c.getId(),
                        allLists.get(idx + 1).getId(),
                        loggedUser.getId()
                );
                MainView.getMainView().setContent(new BoardView(boardId, filterState.copy()));
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        left.setEnabled(idx > 0);
        right.setEnabled(idx < allLists.size() - 1);

        actions.add(take, release, left, right);

        box.add(title, assignee, meta, actions);
        return box;
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
                    .set("background", "var(--lumo-contrast-5pct)");
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
        due.getStyle().set("font-size", "var(--lumo-font-size-s)");

        if (dueAt != null && dueAt.isBefore(LocalDateTime.now())) {
            due.getStyle()
                    .set("color", "var(--lumo-error-text-color)")
                    .set("font-weight", "600");
        } else {
            due.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)");
        }

        return due;
    }

    // ✅ UPDATED: priority label "Svi" kad je null + search refresh bez rebuild-a view-a
    private Component buildFilterBar(Map<Long, String> assigneeLabel, Span count, Runnable refreshColumns) {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        // --- Assignee filter (NE DIRAMO LOGIKU "Svi") ---
        Select<Long> assignee = new Select<>();
        assignee.setLabel("Assignee");
        assignee.setWidth("320px");
        assignee.setEmptySelectionAllowed(true);
        assignee.setEmptySelectionCaption("Svi");

        List<Long> assItems = new ArrayList<>();
        assItems.add(-1L); // unassigned
        assItems.addAll(assigneeLabel.keySet());
        assignee.setItems(assItems);

        assignee.setItemLabelGenerator(id -> {
            if (id == null) return "Svi";
            if (id == -1L) return "— Unassigned —";
            return assigneeLabel.getOrDefault(id, String.valueOf(id));
        });

        assignee.setValue(filterState.assigneeId);

        assignee.addValueChangeListener(e -> {
            filterState.assigneeId = e.getValue();
            refreshColumns.run(); // ✅ bez gubljenja fokusa
        });

        // --- Priority filter (fix Pnull) ---
        Select<Integer> pr = new Select<>();
        pr.setLabel("Prioritet");
        pr.setWidth("200px");
        pr.setEmptySelectionAllowed(true);
        pr.setEmptySelectionCaption("Svi");
        pr.setItems(1, 2, 3, 4, 5);

        // ✅ kad je null, label je "Svi" (da ne bude "Pnull")
        pr.setItemLabelGenerator(p -> p == null ? "Svi" : "P" + p);

        pr.setValue(filterState.priority);

        pr.addValueChangeListener(e -> {
            filterState.priority = e.getValue();
            refreshColumns.run(); // ✅ bez rebuild-a view-a
        });

        // --- Overdue only ---
        Checkbox overdue = new Checkbox("Overdue");
        overdue.setValue(filterState.overdueOnly);
        overdue.addValueChangeListener(e -> {
            filterState.overdueOnly = Boolean.TRUE.equals(e.getValue());
            refreshColumns.run();
        });

        // --- Search title (live debounce, ali bez izbacivanja iz inputa) ---
        TextField search = new TextField();
        search.setLabel("Search title");
        search.setWidthFull();
        search.setClearButtonVisible(true);
        search.setValue(filterState.titleQuery == null ? "" : filterState.titleQuery);

        search.setValueChangeMode(ValueChangeMode.TIMEOUT);
        search.setValueChangeTimeout(300);

        search.addValueChangeListener(e -> {
            filterState.titleQuery = e.getValue();
            refreshColumns.run(); // ✅ ostaje fokus u search-u
        });

        // --- Reset button ---
        Button reset = new Button("Reset", e -> {
            filterState.assigneeId = null;
            filterState.priority = null;
            filterState.overdueOnly = false;
            filterState.titleQuery = "";
            refreshColumns.run();
        });

        bar.add(assignee, pr, overdue, search, reset, count);
        bar.setFlexGrow(1, search);

        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("margin-top", "8px");

        return bar;
    }

    private boolean matchesFilters(Card c, int listIdx, int totalLists) {
        // assignee
        if (filterState.assigneeId != null) {
            if (filterState.assigneeId == -1L) {
                if (c.getAssignedTo() != null) return false;
            } else {
                if (c.getAssignedTo() == null || !filterState.assigneeId.equals(c.getAssignedTo())) return false;
            }
        }

        // priority
        if (filterState.priority != null) {
            int p = c.getPriority() == null ? 1 : c.getPriority();
            if (!filterState.priority.equals(p)) return false;
        }

        // overdue
        if (filterState.overdueOnly) {
            if (c.getDueAt() == null) return false;
            if (!c.getDueAt().isBefore(LocalDateTime.now())) return false;

            // opcionalno: sakrij overdue u zadnjoj listi (Done)
            if (listIdx == totalLists - 1) return false;
        }

        // search by title
        String q = filterState.titleQuery == null ? "" : filterState.titleQuery.trim();
        if (!q.isEmpty()) {
            String t = c.getTitle() == null ? "" : c.getTitle();
            if (!t.toLowerCase().contains(q.toLowerCase())) return false;
        }

        return true;
    }
}
package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.services.ServicesHolder;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ArchivedCardsDialog extends Dialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Long boardId;
    private final Long actorUserId;
    private final ServicesHolder services;
    private final Runnable onChanged;

    private final Grid<CardRepository.ArchivedCardRow> grid =
            new Grid<>(CardRepository.ArchivedCardRow.class, false);

    private List<CardRepository.ArchivedCardRow> allRows = List.of();
    private String titleQuery = "";

    private Span count;
    private final boolean canReopen;

    public ArchivedCardsDialog(Long boardId,
                               Long actorUserId,
                               ServicesHolder services,
                               Runnable onChanged) {
        this.boardId = boardId;
        this.actorUserId = actorUserId;
        this.services = services;
        this.onChanged = onChanged == null ? () -> {} : onChanged;

        var board = services.boardService.requireMemberBoard(boardId, actorUserId);
        boolean boardArchived = board.getArchivedAt() != null;
        BoardRole role = services.boardMemberService.getRole(boardId, actorUserId);
        this.canReopen = !boardArchived && role != BoardRole.VIEWER;

        setHeaderTitle("Arhivirani taskovi");
        setWidth("1080px");
        setMaxWidth("96vw");
        setHeight("720px");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        VerticalLayout root = new VerticalLayout(
                buildIntroSection(),
                buildFilterBar(),
                buildGridSection()
        );
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        add(root);

        refresh();
        applyFiltersAndRender();
    }

    private com.vaadin.flow.component.Component buildIntroSection() {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(false);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "16px")
                .set("background", "linear-gradient(to right, var(--lumo-contrast-5pct), white)");

        H4 title = new H4("Task arhiva");
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph(
                canReopen
                        ? "Ovdje su taskovi koji su arhivirani na ovom boardu. Možeš ih vratiti ako su greškom arhivirani."
                        : "Ovdje možeš pregledati arhivirane taskove ovog boarda. Nemaš pravo vraćanja taskova."
        );
        subtitle.getStyle()
                .set("margin", "4px 0 0 0")
                .set("color", "var(--lumo-secondary-text-color)");

        wrap.add(title, subtitle);
        return wrap;
    }

    private com.vaadin.flow.component.Component buildFilterBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setSpacing(true);
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);

        TextField search = new TextField("Search task");
        search.setWidthFull();
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.TIMEOUT);
        search.setValueChangeTimeout(300);
        search.addValueChangeListener(e -> {
            titleQuery = e.getValue() == null ? "" : e.getValue();
            applyFiltersAndRender();
        });

        Button reset = new Button("Reset", e -> {
            titleQuery = "";
            search.clear();
            applyFiltersAndRender();
        });
        reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        bar.add(search, reset, count);
        bar.setFlexGrow(1, search);
        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "12px")
                .set("background", "white");

        return bar;
    }

    private com.vaadin.flow.component.Component buildGridSection() {
        grid.removeAllColumns();
        grid.setWidthFull();
        grid.setAllRowsVisible(true);

        grid.addColumn(CardRepository.ArchivedCardRow::getTitle)
                .setHeader("Task")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(CardRepository.ArchivedCardRow::getListTitle)
                .setHeader("Lista")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> assigneeLabel(r.getAssigneeName(), r.getAssigneeEmail()))
                .setHeader("Assignee")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(r -> buildPriorityBadge(r.getPriority()))
                .setHeader("Prioritet")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> formatDate(r.getDueAt()))
                .setHeader("Rok")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(r -> formatDateTime(r.getArchivedAt()))
                .setHeader("Arhiviran")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(r -> {
            Button reopen = new Button("Vrati", VaadinIcon.UNLOCK.create());
            reopen.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            reopen.setEnabled(canReopen);
            reopen.setVisible(canReopen);

            reopen.addClickListener(e -> openReopenConfirm(r));
            return reopen;
        }).setHeader("Akcija").setAutoWidth(true).setFlexGrow(0);

        VerticalLayout wrap = new VerticalLayout(grid);
        wrap.setPadding(false);
        wrap.setSpacing(false);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "12px")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)");

        return wrap;
    }

    private void openReopenConfirm(CardRepository.ArchivedCardRow row) {
        ConfirmDialog cd = new ConfirmDialog();
        cd.setHeader("Vratiti task iz arhive?");
        cd.setText("Task '" + row.getTitle() + "' će se vratiti u listu '" + row.getListTitle() + "'.");
        cd.setCancelable(true);
        cd.setConfirmText("Vrati task");
        cd.setConfirmButtonTheme("primary");

        cd.addConfirmListener(ev -> {
            try {
                services.cardService.reopenCard(row.getCardId(), actorUserId);
                Notification.show("Task je vraćen iz arhive.");
                refresh();
                applyFiltersAndRender();
                onChanged.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        cd.open();
    }

    private void refresh() {
        List<CardRepository.ArchivedCardRow> rows = services.cardService.listArchivedCardsForBoard(boardId, actorUserId);
        allRows = rows == null ? List.of() : rows;
    }

    private void applyFiltersAndRender() {
        String q = titleQuery == null ? "" : titleQuery.trim().toLowerCase();

        List<CardRepository.ArchivedCardRow> filtered = new ArrayList<>();
        for (CardRepository.ArchivedCardRow row : allRows) {
            if (!q.isEmpty()) {
                String text = (safe(row.getTitle()) + " " + safe(row.getListTitle()) + " " +
                        safe(row.getAssigneeName()) + " " + safe(row.getAssigneeEmail())).toLowerCase();
                if (!text.contains(q)) {
                    continue;
                }
            }
            filtered.add(row);
        }

        grid.setItems(filtered);
        count.setText("Prikaz: " + filtered.size() + " / " + allRows.size());
    }

    private Span buildPriorityBadge(Integer p) {
        int pr = p == null ? 1 : p;
        Span badge = new Span("P" + pr);
        badge.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "2px 10px")
                .set("border-radius", "999px")
                .set("font-weight", "600")
                .set("border", "1px solid var(--lumo-contrast-20pct)");
        return badge;
    }

    private static String assigneeLabel(String name, String email) {
        String n = safe(name).trim();
        String e = safe(email).trim();
        if (!n.isBlank() && !e.isBlank()) {
            return n + " (" + e + ")";
        }
        if (!n.isBlank()) return n;
        if (!e.isBlank()) return e;
        return "—";
    }

    private static String formatDate(LocalDateTime dt) {
        return dt == null ? "—" : DATE_FMT.format(dt.toLocalDate());
    }

    private static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "—" : DT_FMT.format(dt);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

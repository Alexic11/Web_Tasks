package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.board.BoardRole;
import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.user.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
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

    /*
     * Callback koji BoardView šalje ovom dialogu.
     * Koristimo ga da javimo BoardView-u da treba refresh filtera,
     * ali tek kada se dialog zatvori.
     */
    private final Runnable onMembersChanged;

    /*
     * Flag koji govori da se u dialogu stvarno desila izmjena:
     * dodavanje člana, promjena role ili uklanjanje člana.
     */
    private boolean changed = false;

    private ComboBox<User> userBox;

    private final Grid<BoardMemberRepository.MemberRow> grid =
            new Grid<>(BoardMemberRepository.MemberRow.class, false);

    /*
     * Stari konstruktor ostaje zbog kompatibilnosti.
     * Ako negdje drugo u aplikaciji pozivaš ovaj dialog bez callbacka,
     * neće se ništa slomiti.
     */
    public BoardMembersDialog(Long boardId, Long actorUserId, ServicesHolder services) {
        this(boardId, actorUserId, services, null);
    }

    /*
     * Novi konstruktor koji prima callback.
     */
    public BoardMembersDialog(Long boardId,
                              Long actorUserId,
                              ServicesHolder services,
                              Runnable onMembersChanged) {
        this.boardId = boardId;
        this.actorUserId = actorUserId;
        this.services = services;
        this.onMembersChanged = onMembersChanged == null ? () -> {} : onMembersChanged;

        setHeaderTitle("Members");
        setWidth("960px");
        setMaxWidth("96vw");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        /*
         * Kada korisnik zatvori dialog, ako je bilo izmjena,
         * pozovi BoardView da se ponovo učita.
         *
         * Ovo je bolje nego da refreshujemo BoardView odmah nakon svakog dodavanja,
         * jer bi se dialog zatvorio/nestao poslije prvog dodanog člana.
         */
        addOpenedChangeListener(e -> {
            if (!e.isOpened() && changed) {
                onMembersChanged.run();
            }
        });

        VerticalLayout root = new VerticalLayout(
                buildIntroSection(),
                buildTopSection(),
                buildGridSection()
        );
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        add(root);

        refresh();
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
                .set("background", "linear-gradient(to right, var(--lumo-primary-color-10pct), white)");

        H4 title = new H4("Upravljanje članovima boarda");
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph("Dodaj nove članove, promijeni njihove role ili ukloni pristup boardu.");
        subtitle.getStyle()
                .set("margin", "4px 0 0 0")
                .set("color", "var(--lumo-secondary-text-color)");

        wrap.add(title, subtitle);
        return wrap;
    }

    private com.vaadin.flow.component.Component buildTopSection() {
        userBox = new ComboBox<>("Korisnik");
        userBox.setPlaceholder("Odaberi korisnika...");
        userBox.setWidth("420px");
        userBox.setClearButtonVisible(true);
        userBox.setItemLabelGenerator(u -> u.getFullName() + " (" + u.getEmail() + ")");
        userBox.setItems(services.boardMemberService.listUsersNotInBoard(boardId));

        Select<BoardRole> role = new Select<>();
        role.setLabel("Rola");
        role.setItems(BoardRole.ADMIN, BoardRole.MEMBER, BoardRole.VIEWER);
        role.setValue(BoardRole.MEMBER);
        role.setWidth("190px");

        Button add = new Button("Dodaj člana", e -> {
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

                changed = true;

                userBox.clear();
                role.setValue(BoardRole.MEMBER);
                refresh();

                Notification.show("Član je uspješno dodan.");
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout row = new HorizontalLayout(userBox, role, add);
        row.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        row.setWidthFull();
        row.setSpacing(true);

        VerticalLayout wrap = new VerticalLayout(new H4("Dodaj člana"), row);
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "16px")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)");

        return wrap;
    }

    private com.vaadin.flow.component.Component buildGridSection() {
        grid.setWidthFull();

        grid.addColumn(BoardMemberRepository.MemberRow::getEmail)
                .setHeader("Email")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(BoardMemberRepository.MemberRow::getFullName)
                .setHeader("Ime")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(this::buildRoleBadge)
                .setHeader("Rola")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(this::buildActiveBadge)
                .setHeader("Status")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(r -> {
            BoardRole current = BoardRole.valueOf(r.getRole());

            if (current == BoardRole.OWNER) {
                Span s = new Span("—");
                s.getStyle().set("color", "var(--lumo-secondary-text-color)");
                return s;
            }

            Select<BoardRole> sel = new Select<>();
            sel.setItems(BoardRole.ADMIN, BoardRole.MEMBER, BoardRole.VIEWER);
            sel.setValue(current);
            sel.setWidth("170px");

            sel.addValueChangeListener(ev -> {
                if (ev.getValue() == null) {
                    return;
                }

                try {
                    services.boardMemberService.changeRole(
                            boardId,
                            actorUserId,
                            r.getUserId(),
                            ev.getValue()
                    );

                    changed = true;

                    Notification.show("Rola je sačuvana.");
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
                    services.boardMemberService.removeMember(
                            boardId,
                            actorUserId,
                            r.getUserId()
                    );

                    changed = true;

                    Notification.show("Član je uklonjen.");
                    refresh();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage());
                }
            });
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR);

            return remove;
        }).setHeader("Ukloni").setAutoWidth(true);

        grid.setAllRowsVisible(true);

        VerticalLayout wrap = new VerticalLayout(new H4("Članovi boarda"), grid);
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "16px")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)");

        return wrap;
    }

    private com.vaadin.flow.component.Component buildRoleBadge(BoardMemberRepository.MemberRow row) {
        String role = row.getRole() == null ? "—" : row.getRole();

        Span badge = new Span(role);
        badge.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
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

    private com.vaadin.flow.component.Component buildActiveBadge(BoardMemberRepository.MemberRow row) {
        boolean active = !Boolean.FALSE.equals(row.getActive());

        Span badge = new Span(active ? "AKTIVAN" : "NEAKTIVAN");
        badge.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("background", active
                        ? "var(--lumo-success-color-10pct)"
                        : "var(--lumo-contrast-10pct)")
                .set("color", active
                        ? "var(--lumo-success-text-color)"
                        : "var(--lumo-secondary-text-color)");

        return badge;
    }

    private void refresh() {
        grid.setItems(services.boardMemberService.listMemberRows(boardId));

        if (userBox != null) {
            userBox.setItems(services.boardMemberService.listUsersNotInBoard(boardId));
        }
    }
}
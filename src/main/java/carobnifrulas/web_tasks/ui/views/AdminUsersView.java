package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import carobnifrulas.web_tasks.user.User;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;

@org.springframework.stereotype.Component
public class AdminUsersView extends View implements MenuTab {

    private final Grid<User> grid = new Grid<>(User.class, false);

    private List<User> allUsers = List.of();
    private Span count;
    private HorizontalLayout summaryRow;

    private final FilterState filterState = new FilterState();

    private static final class FilterState {
        String idQuery;
        String emailQuery;
        String nameQuery;
        String mustChange; // null = svi, "DA", "NE"
        String status;     // null = svi, "AKTIVAN", "NEAKTIVAN"

        void reset() {
            idQuery = "";
            emailQuery = "";
            nameQuery = "";
            mustChange = null;
            status = null;
        }
    }

    @Override
    public void setElements() {
        add(buildHeaderSection());

        if (!"admin@local".equalsIgnoreCase(loggedUser.getEmail())) {
            VerticalLayout denied = new VerticalLayout();
            denied.setPadding(false);
            denied.setSpacing(true);
            denied.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
            denied.setWidthFull();
            denied.getStyle()
                    .set("padding", "36px")
                    .set("border", "1px dashed var(--lumo-contrast-20pct)")
                    .set("border-radius", "16px")
                    .set("color", "var(--lumo-secondary-text-color)");

            Icon lock = VaadinIcon.LOCK.create();
            lock.setSize("28px");

            Span text = new Span("Nemaš pristup ovoj stranici.");
            denied.add(lock, text);

            add(denied);
            return;
        }

        count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        summaryRow = new HorizontalLayout();
        summaryRow.setWidthFull();
        summaryRow.setSpacing(true);
        summaryRow.getStyle().set("margin-top", "6px");

        add(summaryRow);
        add(buildFilterBar());

        configureGrid();

        VerticalLayout gridWrap = new VerticalLayout(grid);
        gridWrap.setPadding(false);
        gridWrap.setSpacing(false);
        gridWrap.setWidthFull();
        gridWrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "12px")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)");

        add(gridWrap);

        refreshAllUsers();
        applyFiltersAndRender();
    }

    private Component buildHeaderSection() {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "18px")
                .set("padding", "18px")
                .set("background", "linear-gradient(to right, var(--lumo-primary-color-10pct), white)");

        H2 title = new H2("Admin - Users");
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph("Pregled korisnika, aktivacija/deaktivacija naloga, reset lozinki i kreiranje novih naloga.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)");

        Button addUser = new Button("Dodaj korisnika", e -> openAddDialog());
        addUser.setIcon(VaadinIcon.PLUS.create());
        addUser.addThemeVariants(LUMO_PRIMARY);

        HorizontalLayout top = new HorizontalLayout();
        top.setWidthFull();
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        VerticalLayout left = new VerticalLayout(title, subtitle);
        left.setPadding(false);
        left.setSpacing(false);

        top.add(left, addUser);
        wrap.add(top);

        return wrap;
    }

    private Component buildFilterBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        TextField idSearch = new TextField();
        idSearch.setLabel("Search ID");
        idSearch.setWidth("130px");
        idSearch.setClearButtonVisible(true);
        idSearch.setValue(filterState.idQuery == null ? "" : filterState.idQuery);
        idSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        idSearch.setValueChangeTimeout(300);

        TextField emailSearch = new TextField();
        emailSearch.setLabel("Search email");
        emailSearch.setWidth("260px");
        emailSearch.setClearButtonVisible(true);
        emailSearch.setValue(filterState.emailQuery == null ? "" : filterState.emailQuery);
        emailSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        emailSearch.setValueChangeTimeout(300);

        TextField nameSearch = new TextField();
        nameSearch.setLabel("Search ime");
        nameSearch.setWidth("240px");
        nameSearch.setClearButtonVisible(true);
        nameSearch.setValue(filterState.nameQuery == null ? "" : filterState.nameQuery);
        nameSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        nameSearch.setValueChangeTimeout(300);

        Select<String> must = new Select<>();
        must.setLabel("Mora promj. lozinku");
        must.setWidth("190px");
        must.setEmptySelectionAllowed(true);
        must.setEmptySelectionCaption("Svi");
        must.setItems("DA", "NE");
        must.setValue(filterState.mustChange);

        Select<String> status = new Select<>();
        status.setLabel("Status");
        status.setWidth("170px");
        status.setEmptySelectionAllowed(true);
        status.setEmptySelectionCaption("Svi");
        status.setItems("AKTIVAN", "NEAKTIVAN");
        status.setValue(filterState.status);

        Button reset = new Button("Reset");
        reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        bar.add(idSearch, emailSearch, nameSearch, must, status, reset, count);
        bar.setFlexGrow(1, emailSearch);

        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "12px")
                .set("margin-top", "8px")
                .set("background", "white");

        idSearch.addValueChangeListener(e -> {
            filterState.idQuery = e.getValue();
            applyFiltersAndRender();
        });

        emailSearch.addValueChangeListener(e -> {
            filterState.emailQuery = e.getValue();
            applyFiltersAndRender();
        });

        nameSearch.addValueChangeListener(e -> {
            filterState.nameQuery = e.getValue();
            applyFiltersAndRender();
        });

        must.addValueChangeListener(e -> {
            filterState.mustChange = e.getValue();
            applyFiltersAndRender();
        });

        status.addValueChangeListener(e -> {
            filterState.status = e.getValue();
            applyFiltersAndRender();
        });

        reset.addClickListener(e -> {
            filterState.reset();
            idSearch.setValue("");
            emailSearch.setValue("");
            nameSearch.setValue("");
            must.clear();
            status.clear();
            applyFiltersAndRender();
        });

        return bar;
    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.setWidthFull();

        grid.addColumn(User::getId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addColumn(User::getEmail)
                .setHeader("Email")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(User::getFullName)
                .setHeader("Ime")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addComponentColumn(this::buildStatusBadge)
                .setHeader("Status")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(this::buildMustChangeBadge)
                .setHeader("Mora promj. lozinku")
                .setAutoWidth(true)
                .setFlexGrow(0);

        grid.addComponentColumn(u -> {
            Button reset = new Button("Reset PW", e -> openResetDialog(u));
            reset.setIcon(VaadinIcon.REFRESH.create());
            reset.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            return reset;
        }).setHeader("Reset").setAutoWidth(true);

        grid.addComponentColumn(this::buildActiveToggleButton)
                .setHeader("Aktivacija")
                .setAutoWidth(true);

        grid.setAllRowsVisible(true);
    }

    private Component buildActiveToggleButton(User u) {
        boolean active = u.isActive();

        Button btn = new Button(active ? "Deaktiviraj" : "Aktiviraj");
        btn.setIcon(active ? VaadinIcon.BAN.create() : VaadinIcon.CHECK_CIRCLE.create());

        if (active) {
            btn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        } else {
            btn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        }

        if (u.getEmail() != null && "admin@local".equalsIgnoreCase(u.getEmail())) {
            btn.setEnabled(false);
            btn.setTooltipText("Ne možeš deaktivirati admin nalog.");
        }

        if (loggedUser != null && loggedUser.getId() != null && loggedUser.getId().equals(u.getId())) {
            btn.setEnabled(false);
            btn.setTooltipText("Ne možeš deaktivirati svoj nalog dok si ulogovan.");
        }

        btn.addClickListener(e -> {
            if (active) {
                openDeactivateDialog(u);
            } else {
                openActivateDialog(u);
            }
        });

        return btn;
    }

    private Component buildStatusBadge(User u) {
        boolean active = u.isActive();

        Span badge = new Span(active ? "AKTIVAN" : "NEAKTIVAN");
        badge.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("padding", "3px 10px")
                .set("border-radius", "999px")
                .set("background", active
                        ? "var(--lumo-success-color-10pct)"
                        : "var(--lumo-contrast-10pct)")
                .set("color", active
                        ? "var(--lumo-success-text-color)"
                        : "var(--lumo-secondary-text-color)");

        return badge;
    }

    private Component buildMustChangeBadge(User u) {
        boolean must = u.isMustChangePassword();

        Span badge = new Span(must ? "DA" : "NE");
        badge.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "700")
                .set("padding", "3px 10px")
                .set("border-radius", "999px")
                .set("background", must
                        ? "var(--lumo-warning-color-10pct)"
                        : "var(--lumo-success-color-10pct)")
                .set("color", must
                        ? "var(--lumo-warning-text-color)"
                        : "var(--lumo-success-text-color)");

        return badge;
    }

    private void refreshAllUsers() {
        allUsers = services.userService.findAllUsers();
        if (allUsers == null) {
            allUsers = List.of();
        }
    }

    private void applyFiltersAndRender() {
        String idq = filterState.idQuery == null ? "" : filterState.idQuery.trim();
        String eq = filterState.emailQuery == null ? "" : filterState.emailQuery.trim().toLowerCase();
        String nq = filterState.nameQuery == null ? "" : filterState.nameQuery.trim().toLowerCase();
        String mustVal = filterState.mustChange;
        String statusVal = filterState.status;

        List<User> filtered = new ArrayList<>();

        for (User u : allUsers) {
            if (!idq.isEmpty()) {
                String idStr = u.getId() == null ? "" : String.valueOf(u.getId());
                if (!idStr.contains(idq)) continue;
            }

            if (!eq.isEmpty()) {
                String em = u.getEmail() == null ? "" : u.getEmail().toLowerCase();
                if (!em.contains(eq)) continue;
            }

            if (!nq.isEmpty()) {
                String fn = u.getFullName() == null ? "" : u.getFullName().toLowerCase();
                if (!fn.contains(nq)) continue;
            }

            if (mustVal != null) {
                boolean m = u.isMustChangePassword();
                if ("DA".equals(mustVal) && !m) continue;
                if ("NE".equals(mustVal) && m) continue;
            }

            if (statusVal != null) {
                boolean active = u.isActive();
                if ("AKTIVAN".equals(statusVal) && !active) continue;
                if ("NEAKTIVAN".equals(statusVal) && active) continue;
            }

            filtered.add(u);
        }

        grid.setItems(filtered);
        count.setText("Prikaz: " + filtered.size() + " / " + allUsers.size());
        renderSummary(filtered);
    }

    private void renderSummary(List<User> filtered) {
        summaryRow.removeAll();

        int total = filtered.size();
        int active = 0;
        int inactive = 0;
        int mustChange = 0;
        int admins = 0;

        for (User u : filtered) {
            if (u.isActive()) {
                active++;
            } else {
                inactive++;
            }

            if (u.isMustChangePassword()) {
                mustChange++;
            }

            if (u.getEmail() != null && "admin@local".equalsIgnoreCase(u.getEmail())) {
                admins++;
            }
        }

        summaryRow.add(
                buildSummaryCard("Ukupno", String.valueOf(total), "var(--lumo-primary-color-10pct)"),
                buildSummaryCard("Aktivni", String.valueOf(active), "var(--lumo-success-color-10pct)"),
                buildSummaryCard("Neaktivni", String.valueOf(inactive), "var(--lumo-contrast-10pct)"),
                buildSummaryCard("Moraju promijeniti lozinku", String.valueOf(mustChange), "var(--lumo-warning-color-10pct)"),
                buildSummaryCard("Admin nalozi", String.valueOf(admins), "var(--lumo-contrast-10pct)")
        );
    }

    private Component buildSummaryCard(String label, String value, String background) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.setWidth("220px");

        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "14px")
                .set("background", background)
                .set("box-sizing", "border-box");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "6px");

        card.add(valueSpan, labelSpan);
        return card;
    }

    private void openDeactivateDialog(User u) {
        ConfirmDialog cd = new ConfirmDialog();
        cd.setHeader("Deaktivirati korisnika?");
        cd.setText("Korisnik više neće moći da se prijavi i neće se nuditi za nova dodavanja/dodjele. Historija ostaje sačuvana: " + u.getEmail());
        cd.setCancelable(true);
        cd.setCancelText("Otkaži");
        cd.setConfirmText("Deaktiviraj");
        cd.setConfirmButtonTheme("error primary");

        cd.addConfirmListener(ev -> {
            try {
                services.userService.deactivateUser(u.getId(), loggedUser.getId());
                refreshAllUsers();
                applyFiltersAndRender();
                Notification.show("Korisnik deaktiviran.");
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        cd.open();
    }

    private void openActivateDialog(User u) {
        ConfirmDialog cd = new ConfirmDialog();
        cd.setHeader("Aktivirati korisnika?");
        cd.setText("Korisnik će ponovo moći da se prijavi i da bude dodan/dodijeljen: " + u.getEmail());
        cd.setCancelable(true);
        cd.setCancelText("Otkaži");
        cd.setConfirmText("Aktiviraj");
        cd.setConfirmButtonTheme("success primary");

        cd.addConfirmListener(ev -> {
            try {
                services.userService.activateUser(u.getId());
                refreshAllUsers();
                applyFiltersAndRender();
                Notification.show("Korisnik aktiviran.");
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        cd.open();
    }

    private void openAddDialog() {
        Dialog d = new Dialog();
        d.setHeaderTitle("Novi korisnik");

        TextField email = new TextField("Email");
        TextField fullName = new TextField("Puno ime");
        email.setWidthFull();
        fullName.setWidthFull();

        Button save = new Button("Kreiraj", e -> {
            String em = email.getValue() == null ? "" : email.getValue().trim();
            String fn = fullName.getValue() == null ? "" : fullName.getValue().trim();

            if (em.isBlank() || !em.contains("@")) {
                Notification.show("Unesi validan email.");
                return;
            }
            if (fn.isBlank()) {
                Notification.show("Unesi ime.");
                return;
            }

            try {
                var res = services.userService.createUserWithTempPassword(em, fn);
                d.close();

                refreshAllUsers();
                applyFiltersAndRender();

                showTempPasswordDialog(res.user().getEmail(), res.tempPassword());
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Otkaži", e -> d.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout actions = new HorizontalLayout(save, cancel);

        VerticalLayout layout = new VerticalLayout(email, fullName, actions);
        layout.setWidth("420px");
        layout.setPadding(false);
        layout.setSpacing(true);

        d.add(layout);
        d.open();
    }

    private void openResetDialog(User u) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Reset lozinke: " + u.getEmail());

        Button doReset = new Button("Resetuj", e -> {
            String temp = services.userService.resetPassword(u.getId());
            d.close();

            refreshAllUsers();
            applyFiltersAndRender();

            showTempPasswordDialog(u.getEmail(), temp);
        });
        doReset.addThemeVariants(LUMO_PRIMARY);

        Button cancel = new Button("Otkaži", e -> d.close());
        cancel.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(
                new Paragraph("Korisnik će morati promijeniti lozinku pri sljedećem login-u."),
                new HorizontalLayout(doReset, cancel)
        );
        layout.setPadding(false);
        layout.setSpacing(true);

        d.add(layout);
        d.open();
    }

    private void showTempPasswordDialog(String email, String tempPassword) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Privremena lozinka");

        Paragraph info = new Paragraph(
                "Korisnik: " + email + "\nPrivremena lozinka: " + tempPassword
        );
        info.getStyle()
                .set("white-space", "pre-wrap")
                .set("font-family", "monospace");

        Button ok = new Button("OK", e -> d.close());
        ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout layout = new VerticalLayout(info, ok);
        layout.setPadding(false);
        layout.setSpacing(true);

        d.add(layout);
        d.open();
    }

    @Override
    public String getTabName() {
        return "Admin";
    }

    @Override
    public VaadinIcon getTabIcon() {
        return VaadinIcon.TOOLS;
    }

    @Override
    public DomEventListener onTabClick() {
        return e -> MainView.getMainView().setContent(this);
    }
}

package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import carobnifrulas.web_tasks.user.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;

@Component
public class AdminUsersView extends View implements MenuTab {

    private final Grid<User> grid = new Grid<>(User.class, false);

    // cache za filtering
    private List<User> allUsers = List.of();

    // state
    private final FilterState filterState = new FilterState();

    private static final class FilterState {
        String idQuery;
        String emailQuery;
        String nameQuery;
        String mustChange; // null = svi, "DA", "NE"

        void reset() {
            idQuery = "";
            emailQuery = "";
            nameQuery = "";
            mustChange = null;
        }
    }

    @Override
    public void setElements() {
        add(new H2("Admin - Users"));

        // sigurnosna provjera (MVP): admin@local
        if (!"admin@local".equalsIgnoreCase(loggedUser.getEmail())) {
            add(new Paragraph("Nemaš pristup ovoj stranici."));
            return;
        }

        Button addUser = new Button("Dodaj korisnika", e -> openAddDialog());
        addUser.setIcon(VaadinIcon.PLUS.create());
        addUser.addThemeVariants(LUMO_PRIMARY);

        configureGrid();

        // ===== FILTER BAR =====
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        bar.setSpacing(true);

        TextField idSearch = new TextField();
        idSearch.setLabel("Search ID");
        idSearch.setWidth("160px");
        idSearch.setClearButtonVisible(true);
        idSearch.setValue(filterState.idQuery == null ? "" : filterState.idQuery);
        idSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        idSearch.setValueChangeTimeout(300);

        TextField emailSearch = new TextField();
        emailSearch.setLabel("Search email");
        emailSearch.setWidth("280px");
        emailSearch.setClearButtonVisible(true);
        emailSearch.setValue(filterState.emailQuery == null ? "" : filterState.emailQuery);
        emailSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        emailSearch.setValueChangeTimeout(300);

        TextField nameSearch = new TextField();
        nameSearch.setLabel("Search ime");
        nameSearch.setWidth("260px");
        nameSearch.setClearButtonVisible(true);
        nameSearch.setValue(filterState.nameQuery == null ? "" : filterState.nameQuery);
        nameSearch.setValueChangeMode(ValueChangeMode.TIMEOUT);
        nameSearch.setValueChangeTimeout(300);

        Select<String> must = new Select<>();
        must.setLabel("Mora promj. lozinku");
        must.setWidth("220px");
        must.setEmptySelectionAllowed(true);
        must.setEmptySelectionCaption("Svi");
        must.setItems("DA", "NE");
        must.setValue(filterState.mustChange);

        Button reset = new Button("Reset");

        Span count = new Span();
        count.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");

        bar.add(idSearch, emailSearch, nameSearch, must, reset, count);
        bar.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "10px")
                .set("margin-top", "8px");

        // ===== LOAD + APPLY FILTERS =====
        refreshAllUsers();

        Runnable applyFilters = () -> {
            String idq = filterState.idQuery == null ? "" : filterState.idQuery.trim();
            String eq = filterState.emailQuery == null ? "" : filterState.emailQuery.trim().toLowerCase();
            String nq = filterState.nameQuery == null ? "" : filterState.nameQuery.trim().toLowerCase();
            String mustVal = filterState.mustChange;

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

                filtered.add(u);
            }

            grid.setItems(filtered);
            count.setText("Prikaz: " + filtered.size() + " / " + allUsers.size());
        };

        idSearch.addValueChangeListener(e -> {
            filterState.idQuery = e.getValue();
            applyFilters.run();
        });

        emailSearch.addValueChangeListener(e -> {
            filterState.emailQuery = e.getValue();
            applyFilters.run();
        });

        nameSearch.addValueChangeListener(e -> {
            filterState.nameQuery = e.getValue();
            applyFilters.run();
        });

        must.addValueChangeListener(e -> {
            filterState.mustChange = e.getValue();
            applyFilters.run();
        });

        reset.addClickListener(e -> {
            filterState.reset();
            idSearch.setValue("");
            emailSearch.setValue("");
            nameSearch.setValue("");
            must.clear();
            applyFilters.run();
        });

        applyFilters.run();

        add(addUser, bar, grid);
    }

    private void configureGrid() {

        grid.removeAllColumns(); // spriječi dupliranje kolona kad se view refresha
        grid.setWidthFull();

        grid.addColumn(User::getId).setHeader("ID").setAutoWidth(true);

        grid.addColumn(User::getEmail)
                .setHeader("Email")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(User::getFullName)
                .setHeader("Ime")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(u -> u.isMustChangePassword() ? "DA" : "NE")
                .setHeader("Mora promj. lozinku")
                .setAutoWidth(true);

        grid.addComponentColumn(u -> {
            Button reset = new Button("Reset PW", e -> openResetDialog(u));
            reset.setIcon(VaadinIcon.REFRESH.create());
            return reset;
        }).setHeader("Reset").setAutoWidth(true);

        // ✅ NOVO: Delete kolona
        grid.addComponentColumn(u -> {
            Button del = new Button("Obriši");
            del.setIcon(VaadinIcon.TRASH.create());
            del.getStyle().set("color", "crimson");

            if (u.getEmail() != null && "admin@local".equalsIgnoreCase(u.getEmail())) {
                del.setEnabled(false);
                del.setTooltipText("Ne možeš obrisati admin nalog.");
            }

            del.addClickListener(e -> {
                ConfirmDialog cd = new ConfirmDialog();
                cd.setHeader("Potvrda brisanja");
                cd.setText("Da li ste sigurni da želite obrisati korisnika: " + u.getEmail() + " ?");
                cd.setCancelable(true);
                cd.setCancelText("Otkaži");
                cd.setConfirmText("Obriši");
                cd.setConfirmButtonTheme("error primary");

                cd.addConfirmListener(ev -> {
                    try {
                        services.userService.deleteUser(u.getId());
                        refreshAllUsers();
                        MainView.getMainView().setContent(this);
                        Notification.show("Korisnik obrisan.");
                    } catch (Exception ex) {
                        Notification.show(ex.getMessage());
                    }
                });

                cd.open();
            });

            return del;
        }).setHeader("Brisanje").setAutoWidth(true);

        grid.setAllRowsVisible(true);
    }

    private void refreshAllUsers() {
        allUsers = services.userService.findAllUsers();
        if (allUsers == null) allUsers = List.of();
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
                MainView.getMainView().setContent(this);

                showTempPasswordDialog(res.user().getEmail(), res.tempPassword());
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        Button cancel = new Button("Otkaži", e -> d.close());

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        VerticalLayout layout = new VerticalLayout(email, fullName, actions);
        layout.setWidth("420px");

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
            MainView.getMainView().setContent(this);

            showTempPasswordDialog(u.getEmail(), temp);
        });
        doReset.addThemeVariants(LUMO_PRIMARY);

        Button cancel = new Button("Otkaži", e -> d.close());

        d.add(new VerticalLayout(
                new Paragraph("Korisnik će morati promijeniti lozinku pri sljedećem login-u."),
                new HorizontalLayout(doReset, cancel)
        ));
        d.open();
    }

    private void showTempPasswordDialog(String email, String tempPassword) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Privremena lozinka");

        Paragraph info = new Paragraph(
                "Korisnik: " + email + "\nPrivremena lozinka: " + tempPassword
        );

        info.getStyle().set("white-space", "pre-wrap").set("font-family", "monospace");

        Button ok = new Button("OK", e -> d.close());
        d.add(new VerticalLayout(info, ok));
        d.open();
    }

    @Override public String getTabName() { return "Admin"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.TOOLS; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}
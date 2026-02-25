package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import carobnifrulas.web_tasks.user.User;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.DomEventListener;
import org.springframework.stereotype.Component;

import static com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY;

@Component
public class AdminUsersView extends View implements MenuTab {

    private final Grid<User> grid = new Grid<>(User.class, false);

    @Override
    public void setElements() {
        add(new H2("Admin - Users"));

        // sigurnosna provjera (MVP): admin@local
        if (!"admin@local".equalsIgnoreCase(loggedUser.getEmail())) {
            add(new com.vaadin.flow.component.html.Paragraph("Nemaš pristup ovoj stranici."));
            return;
        }

        Button addUser = new Button("Dodaj korisnika", e -> openAddDialog());
        addUser.setIcon(VaadinIcon.PLUS.create());
        addUser.addThemeVariants(LUMO_PRIMARY);

        configureGrid();
        refresh();

        add(addUser, grid);
    }

    private void configureGrid() {
        grid.setWidthFull();

        grid.addColumn(User::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(User::getEmail).setHeader("Email").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(User::getFullName).setHeader("Ime").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(u -> u.isMustChangePassword() ? "DA" : "NE")
                .setHeader("Mora promj. lozinku").setAutoWidth(true);

        grid.addComponentColumn(u -> {
            Button reset = new Button("Reset PW", e -> openResetDialog(u));
            reset.setIcon(VaadinIcon.REFRESH.create());
            return reset;
        }).setHeader("Akcija").setAutoWidth(true);

        grid.setAllRowsVisible(true);
    }

    private void refresh() {
        grid.setItems(services.userService.findAllUsers());
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
                refresh();
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
            refresh();
            showTempPasswordDialog(u.getEmail(), temp);
        });
        doReset.addThemeVariants(LUMO_PRIMARY);

        Button cancel = new Button("Otkaži", e -> d.close());

        d.add(new VerticalLayout(
                new com.vaadin.flow.component.html.Paragraph("Korisnik će morati promijeniti lozinku pri sljedećem login-u."),
                new HorizontalLayout(doReset, cancel)
        ));
        d.open();
    }

    private void showTempPasswordDialog(String email, String tempPassword) {
        Dialog d = new Dialog();
        d.setHeaderTitle("Privremena lozinka");

        var info = new com.vaadin.flow.component.html.Paragraph(
                "Korisnik: " + email + "\nPrivremena lozinka: " + tempPassword
        );

        // Prikaži kao mono font radi copy/paste
        info.getStyle().set("white-space", "pre-wrap").set("font-family", "monospace");

        Button ok = new Button("OK", e -> d.close());
        d.add(new VerticalLayout(info, ok));
        d.open();
    }

    // MenuTab
    @Override public String getTabName() { return "Admin"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.TOOLS; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }
}
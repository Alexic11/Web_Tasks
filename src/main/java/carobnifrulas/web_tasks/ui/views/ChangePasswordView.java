package carobnifrulas.web_tasks.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;

public class ChangePasswordView extends View {

    @Override
    public void setElements() {
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        add(buildHeaderSection());
        add(buildFormCard());
    }

    private com.vaadin.flow.component.Component buildHeaderSection() {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();
        wrap.setMaxWidth("760px");

        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "18px")
                .set("padding", "18px")
                .set("background", "linear-gradient(to right, var(--lumo-primary-color-10pct), white)");

        H2 title = new H2("Promjena lozinke");
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph("Radi sigurnosti naloga, potrebno je da postaviš novu lozinku prije nastavka rada.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)");

        wrap.add(title, subtitle);
        return wrap;
    }

    private com.vaadin.flow.component.Component buildFormCard() {
        PasswordField p1 = new PasswordField("Nova lozinka");
        PasswordField p2 = new PasswordField("Ponovi novu lozinku");

        p1.setWidthFull();
        p2.setWidthFull();

        p1.setPlaceholder("Unesi novu lozinku");
        p2.setPlaceholder("Ponovi novu lozinku");

        Button save = new Button("Sačuvaj", e -> {
            String a = p1.getValue();
            String b = p2.getValue();

            if (a == null || a.length() < 6) {
                Notification.show("Lozinka mora imati bar 6 karaktera.");
                return;
            }

            if (!a.equals(b)) {
                Notification.show("Lozinke se ne poklapaju.");
                return;
            }

            services.userService.changePassword(loggedUser.getId(), a);
            Notification.show("Lozinka je promijenjena.");

            var refreshed = services.userService.requireByEmail(loggedUser.getEmail());
            carobnifrulas.web_tasks.ui.MainView.getMainView().setLoggedUser(refreshed);
            carobnifrulas.web_tasks.ui.MainView.getMainView().setContent(services.menu.getDefaultView());
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setWidthFull();

        Paragraph hint = new Paragraph("Preporuka: koristi kombinaciju slova, brojeva i specijalnih znakova.");
        hint.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout form = new VerticalLayout(p1, p2, hint, save);
        form.setPadding(false);
        form.setSpacing(true);
        form.setWidthFull();
        form.setMaxWidth("460px");

        VerticalLayout card = new VerticalLayout(form);
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.setMaxWidth("760px");
        card.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "18px")
                .set("padding", "28px")
                .set("background", "white")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.05)");

        return card;
    }
}
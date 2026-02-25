package carobnifrulas.web_tasks.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;

public class ChangePasswordView extends View {

    @Override
    public void setElements() {
        add(new H2("Promjena lozinke"));

        PasswordField p1 = new PasswordField("Nova lozinka");
        PasswordField p2 = new PasswordField("Ponovi novu lozinku");
        p1.setWidth("350px");
        p2.setWidth("350px");

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

            // refresh loggedUser u MainView (da mustChangePassword postane false)
            var refreshed = services.userService.requireByEmail(loggedUser.getEmail());
            carobnifrulas.web_tasks.ui.MainView.getMainView().setLoggedUser(refreshed);

            carobnifrulas.web_tasks.ui.MainView.getMainView().setContent(services.menu.getDefaultView());
        });

        add(new VerticalLayout(p1, p2, save));
    }
}
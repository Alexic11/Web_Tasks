package carobnifrulas.web_tasks.ui;

import com.vaadin.flow.component.login.*;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;

@PageTitle("Web Tasks | Login")
@Route("login")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginOverlay login = new LoginOverlay();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Ako je već ulogovan, idi na root
        if (VaadinSession.getCurrent().getAttribute("SPRING_SECURITY_CONTEXT") != null) {
            getUI().ifPresent(ui -> ui.navigate(""));
            return;
        }

        var i18n = LoginI18n.createDefault();
        var form = new LoginI18n.Form();
        form.setTitle("Prijava");
        form.setUsername("Email");
        form.setPassword("Lozinka");
        form.setSubmit("Prijavi se");
        form.setForgotPassword("");
        i18n.setForm(form);

        login.setI18n(i18n);
        login.setAction("login"); // Spring Security endpoint
        login.setTitle("Web Tasks");
        login.setDescription("Upravljanje zadacima po boardovima");
        login.setForgotPasswordButtonVisible(false);
        login.setOpened(true);

        add(login);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean hasError = event.getLocation().getQueryParameters().getParameters().containsKey("error");
        login.setError(hasError);
    }
}
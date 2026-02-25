package carobnifrulas.web_tasks.ui;

import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.user.User;
import carobnifrulas.web_tasks.security.model.SecurityUtils;
import carobnifrulas.web_tasks.ui.menu.Menu;
import carobnifrulas.web_tasks.ui.views.View;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContextImpl;
import com.vaadin.flow.spring.security.AuthenticationContext;

@PageTitle("Web Tasks")
@Route("")
@PermitAll
@PreserveOnRefresh
public class MainView extends AppLayout {

    private final Menu menu;

    @Getter
    private final ServicesHolder servicesHolder;

    @Setter
    @Getter
    private User loggedUser;

    private View currentView;

    private Tabs primaryTabs;
    private HorizontalLayout prTabs;

    private VerticalLayout viewHeader;
    private Button logout;

    private final AuthenticationContext authContext;

    @Getter
    private int screenHeight;

    public MainView(ServicesHolder servicesHolder, Menu menu, AuthenticationContext authContext) {
        this.servicesHolder = servicesHolder;
        this.menu = menu;
        this.authContext = authContext;

        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            screenHeight = details.getBodyClientHeight();

            // (dosledno zip-u) izvuci SPRING_SECURITY_CONTEXT
            SecurityContextImpl securityContext =
                    (SecurityContextImpl) VaadinSession.getCurrent().getSession().getAttribute("SPRING_SECURITY_CONTEXT");

            String email = SecurityUtils.getUsername(securityContext)
                    .orElseThrow(() -> new IllegalStateException("No authenticated user"));

            loggedUser = servicesHolder.userService.requireByEmail(email);

            VaadinSession.getCurrent().setAttribute("main", this);

            // Header (navbar)
            DrawerToggle toggle = new DrawerToggle();
            Div title = new Div();
            title.setText("Web Tasks");
            title.getStyle().set("font-weight", "700").set("font-size", "var(--lumo-font-size-l)");

            HorizontalLayout header = new HorizontalLayout(toggle, title);
            header.setWidthFull();
            header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            header.getStyle().set("padding", "0 var(--lumo-space-m)");
            header.setSpacing(true);

            viewHeader = new VerticalLayout(header);
            viewHeader.setPadding(false);
            viewHeader.setSpacing(false);

            // Drawer: username + linija + tabs + logout
            Div username = new Div("Korisnik: " + loggedUser.getEmail());
            username.getStyle()
                    .set("font-size", "var(--lumo-font-size-m)")
                    .set("margin", "0 var(--lumo-space-m)");

            Div linija = new Div();
            linija.getStyle().set("width", "100%").set("border-top", "2px solid dodgerblue");

            primaryTabs = menu.getVerticalTabs();
            prTabs = new HorizontalLayout(primaryTabs);

            setLogoutButton();
            logout.getStyle().set("margin-top", "auto");

            VerticalLayout drawer = new VerticalLayout(username, linija, prTabs, logout);
            drawer.setPadding(false);
            drawer.setSpacing(false);
            drawer.setSizeFull();
            drawer.getStyle().set("padding", "var(--lumo-space-m)");

            addToDrawer(drawer);
            addToNavbar(viewHeader);

            setPrimarySection(Section.DRAWER);

            // Default view
            if (loggedUser.isMustChangePassword()) {
                setContent(new carobnifrulas.web_tasks.ui.views.ChangePasswordView());
            } else {
                setContent(menu.getDefaultView());
            }
        });
    }

    public static MainView getMainView() {
        return (MainView) VaadinSession.getCurrent().getAttribute("main");
    }

    public void setPrimaryTabs(Tabs tabs) {
        prTabs.replace(this.primaryTabs, tabs);
        this.primaryTabs = tabs;
    }

    public void setContent(View view) {
        if (currentView != null) currentView.destroy();
        currentView = view;

        view.prepare();

        if (getUI().isPresent()) {
            getUI().get().access(() -> super.setContent(view));
        } else {
            super.setContent(view);
        }
    }

    private void setLogoutButton() {
        logout = new Button("Odjavi se");
        logout.setWidthFull();
        logout.getStyle().set("border", "1px solid dodgerblue");
        logout.setIcon(new Icon(VaadinIcon.SIGN_OUT));
        logout.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        logout.addClickListener(e -> authContext.logout());
    }

}
package carobnifrulas.web_tasks.ui;

import carobnifrulas.web_tasks.card.Card;
import carobnifrulas.web_tasks.notification.NotificationEntity;
import carobnifrulas.web_tasks.notification.UserNotificationBus;
import carobnifrulas.web_tasks.security.model.SecurityUtils;
import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.ui.menu.Menu;
import carobnifrulas.web_tasks.ui.views.TaskDialog;
import carobnifrulas.web_tasks.ui.views.View;
import carobnifrulas.web_tasks.user.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContextImpl;
import com.vaadin.flow.spring.security.AuthenticationContext;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Web Tasks")
@Route("")
@PermitAll
@PreserveOnRefresh
public class MainView extends AppLayout {

    private static final DateTimeFormatter NOTIF_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

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

    private Span notificationBadge;
    private Button notificationButton;
    private Registration notificationRegistration;

    public MainView(ServicesHolder servicesHolder, Menu menu, AuthenticationContext authContext) {
        this.servicesHolder = servicesHolder;
        this.menu = menu;
        this.authContext = authContext;

        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            screenHeight = details.getBodyClientHeight();

            SecurityContextImpl securityContext =
                    (SecurityContextImpl) VaadinSession.getCurrent().getSession().getAttribute("SPRING_SECURITY_CONTEXT");

            String email = SecurityUtils.getUsername(securityContext)
                    .orElseThrow(() -> new IllegalStateException("No authenticated user"));

            loggedUser = servicesHolder.userService.requireByEmail(email);

            VaadinSession.getCurrent().setAttribute("main", this);

            DrawerToggle toggle = new DrawerToggle();

            Div title = new Div("Web Tasks");
            title.getStyle()
                    .set("font-weight", "700")
                    .set("font-size", "var(--lumo-font-size-l)");

            notificationBadge = new Span();
            notificationBadge.getStyle()
                    .set("position", "absolute")
                    .set("top", "-4px")
                    .set("right", "-4px")
                    .set("background", "var(--lumo-error-color)")
                    .set("color", "white")
                    .set("border-radius", "999px")
                    .set("min-width", "18px")
                    .set("height", "18px")
                    .set("padding", "0 5px")
                    .set("font-size", "11px")
                    .set("font-weight", "700")
                    .set("line-height", "18px")
                    .set("text-align", "center")
                    .set("box-shadow", "0 0 0 2px white")
                    .set("display", "none");

            notificationButton = new Button(new Icon(VaadinIcon.BELL));
            notificationButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            notificationButton.getStyle()
                    .set("border-radius", "999px")
                    .set("width", "42px")
                    .set("height", "42px")
                    .set("min-width", "42px")
                    .set("padding", "0")
                    .set("background", "var(--lumo-contrast-5pct)");
            notificationButton.addClickListener(e -> openNotificationsDialog());

            Div notificationWrap = new Div(notificationButton, notificationBadge);
            notificationWrap.getStyle()
                    .set("position", "relative")
                    .set("margin-left", "auto");

            HorizontalLayout header = new HorizontalLayout(toggle, title, notificationWrap);
            header.setWidthFull();
            header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            header.setSpacing(true);
            header.getStyle()
                    .set("padding", "10px 16px")
                    .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                    .set("background", "white");

            viewHeader = new VerticalLayout(header);
            viewHeader.setPadding(false);
            viewHeader.setSpacing(false);

            Div username = new Div("Korisnik: " + loggedUser.getEmail());
            username.getStyle()
                    .set("font-size", "var(--lumo-font-size-m)")
                    .set("margin", "0 var(--lumo-space-m)");

            Div linija = new Div();
            linija.getStyle()
                    .set("width", "100%")
                    .set("border-top", "2px solid dodgerblue");

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

            setPrimarySection(AppLayout.Section.DRAWER);

            refreshNotificationBadge();
            registerNotificationListener();

            if (loggedUser.isMustChangePassword()) {
                setContent(new carobnifrulas.web_tasks.ui.views.ChangePasswordView());
            } else {
                setContent(menu.getDefaultView());
            }
        });
    }

    private void registerNotificationListener() {
        notificationRegistration = UserNotificationBus.register(
                loggedUser.getId(),
                () -> getUI().ifPresent(ui -> ui.access(this::refreshNotificationBadge))
        );

        addDetachListener(e -> {
            if (notificationRegistration != null) {
                notificationRegistration.remove();
                notificationRegistration = null;
            }
        });
    }

    private void refreshNotificationBadge() {
        long unread = servicesHolder.notificationService.countUnread(loggedUser.getId());

        if (unread > 0) {
            notificationBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
            notificationBadge.getStyle().set("display", "inline-block");
        } else {
            notificationBadge.setText("");
            notificationBadge.getStyle().set("display", "none");
        }
    }

    private void openNotificationsDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Notifikacije");
        dialog.setWidth("620px");
        dialog.setMaxWidth("96vw");

        VerticalLayout body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(true);
        body.setWidthFull();

        List<NotificationEntity> items =
                servicesHolder.notificationService.listLatest(loggedUser.getId(), 20);

        if (items.isEmpty()) {
            VerticalLayout emptyState = new VerticalLayout();
            emptyState.setPadding(false);
            emptyState.setSpacing(false);
            emptyState.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
            emptyState.getStyle()
                    .set("padding", "36px 12px")
                    .set("color", "var(--lumo-secondary-text-color)");

            Icon bell = VaadinIcon.BELL.create();
            bell.setSize("28px");

            Span txt = new Span("Nema notifikacija.");
            txt.getStyle().set("margin-top", "8px");

            emptyState.add(bell, txt);
            body.add(emptyState);
        } else {
            VerticalLayout list = new VerticalLayout();
            list.setPadding(false);
            list.setSpacing(true);
            list.setWidthFull();

            for (NotificationEntity n : items) {
                list.add(buildNotificationCard(dialog, n));
            }

            Scroller scroller = new Scroller(list);
            scroller.setWidthFull();
            scroller.setHeight("430px");

            body.add(scroller);
        }

        Button markAll = new Button("Označi sve kao pročitano");
        markAll.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        markAll.addClickListener(e -> {
            servicesHolder.notificationService.markAllAsRead(loggedUser.getId());
            refreshNotificationBadge();
            dialog.close();
        });

        Button closeBtn = new Button("Zatvori", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout footer = new HorizontalLayout(markAll, closeBtn);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        footer.getStyle().set("margin-top", "4px");

        VerticalLayout wrap = new VerticalLayout(body, footer);
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();

        dialog.add(wrap);
        dialog.open();
    }

    private com.vaadin.flow.component.Component buildNotificationCard(Dialog parentDialog, NotificationEntity n) {
        String when = n.getCreatedAt() == null
                ? ""
                : NOTIF_FMT.format(n.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime());

        H4 title = new H4(n.getTitle() == null ? "" : n.getTitle());
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("line-height", "1.25");

        Span status = new Span(n.isRead() ? "Pročitano" : "Novo");
        status.getStyle()
                .set("font-size", "11px")
                .set("font-weight", "700")
                .set("padding", "3px 8px")
                .set("border-radius", "999px")
                .set("white-space", "nowrap")
                .set("background", n.isRead()
                        ? "var(--lumo-contrast-10pct)"
                        : "var(--lumo-primary-color-10pct)")
                .set("color", n.isRead()
                        ? "var(--lumo-secondary-text-color)"
                        : "var(--lumo-primary-text-color)");

        HorizontalLayout top = new HorizontalLayout(title, status);
        top.setWidthFull();
        top.setSpacing(true);
        top.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.START);
        top.expand(title);

        Span message = new Span(n.getMessage() == null ? "" : n.getMessage());
        message.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-body-text-color)")
                .set("line-height", "1.45")
                .set("white-space", "normal")
                .set("word-break", "break-word");

        Span time = new Span(when);
        time.getStyle()
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout inner = new VerticalLayout(top, message, time);
        inner.setPadding(false);
        inner.setSpacing(true);
        inner.setWidthFull();

        Div card = new Div(inner);
        card.setWidthFull();
        card.getStyle()
                .set("padding", "14px 16px")
                .set("border-radius", "14px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("background", n.isRead()
                        ? "white"
                        : "linear-gradient(to right, var(--lumo-primary-color-10pct), white)")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.04)")
                .set("box-sizing", "border-box")
                .set("cursor", "pointer")
                .set("transition", "all 0.2s ease");

        card.getElement().addEventListener("mouseenter", e ->
                card.getStyle().set("box-shadow", "0 4px 12px rgba(0,0,0,0.08)")
        );
        card.getElement().addEventListener("mouseleave", e ->
                card.getStyle().set("box-shadow", "0 1px 4px rgba(0,0,0,0.04)")
        );

        card.addClickListener(e -> {
            try {
                servicesHolder.notificationService.markAsRead(n.getId(), loggedUser.getId());
                refreshNotificationBadge();
                parentDialog.close();
                openCardFromNotification(n.getBoardId(), n.getCardId());
            } catch (Exception ex) {
                com.vaadin.flow.component.notification.Notification.show(ex.getMessage());
            }
        });

        return card;
    }

    public void openCardFromNotification(Long boardId, Long cardId) {
        if (boardId == null || cardId == null) {
            return;
        }

        Card card = servicesHolder.cardService.requireById(cardId);
        setContent(new carobnifrulas.web_tasks.ui.views.BoardView(boardId));

        getUI().ifPresent(ui -> ui.access(() ->
                TaskDialog.edit(servicesHolder, card, loggedUser.getId()).open()
        ));
    }

    public static MainView getMainView() {
        return (MainView) VaadinSession.getCurrent().getAttribute("main");
    }

    public void setPrimaryTabs(Tabs tabs) {
        prTabs.replace(this.primaryTabs, tabs);
        this.primaryTabs = tabs;
    }

    public void setContent(View view) {
        if (currentView != null) {
            currentView.destroy();
        }
        currentView = view;

        view.prepare();
        super.setContent(view);
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
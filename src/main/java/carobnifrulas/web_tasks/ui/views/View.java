package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.services.ServicesHolder;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.user.User;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class View extends VerticalLayout {

    protected ServicesHolder services;
    protected User loggedUser;

    public void prepare() {
        setWidthFull();
        setPadding(true);
        setSpacing(true);

        services = MainView.getMainView().getServicesHolder();
        loggedUser = MainView.getMainView().getLoggedUser();

        removeAll();
        setElements();
    }

    public abstract void setElements();

    public void destroy() {
        removeAll();
    }
}
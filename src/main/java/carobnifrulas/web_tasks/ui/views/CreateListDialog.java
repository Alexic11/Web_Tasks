package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.services.ServicesHolder;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

public class CreateListDialog extends Dialog {

    public CreateListDialog(ServicesHolder services,
                            Long boardId,
                            Long actorUserId,
                            Runnable onSaved) {

        setHeaderTitle("Nova lista");
        setWidth("520px");

        TextField title = new TextField("Naziv");
        title.setWidthFull();
        title.setPlaceholder("npr. Backlog");

        Button save = new Button("Sačuvaj", e -> {
            try {
                services.listService.createList(boardId, actorUserId, title.getValue());
                close();
                Notification.show("Lista dodata.");
                if (onSaved != null) onSaved.run();
            } catch (Exception ex) {
                Notification.show(ex.getMessage());
            }
        });

        Button cancel = new Button("Otkaži", e -> close());

        add(new VerticalLayout(title, new HorizontalLayout(save, cancel)));
    }
}
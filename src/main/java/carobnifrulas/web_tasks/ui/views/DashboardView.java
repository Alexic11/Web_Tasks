package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.dashboard.DashboardService;
import carobnifrulas.web_tasks.dashboard.dto.BoardStatsDto;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.flow.spring.annotation.UIScope;

@UIScope
@org.springframework.stereotype.Component
public class DashboardView extends View implements MenuTab {

    private final DashboardService service;

    private Tabs tabs;
    private Tab tabActive;
    private Tab tabArchived;
    private VerticalLayout content;

    public DashboardView(DashboardService service) {
        this.service = service;
    }

    @Override public String getTabName() { return "Dashboard"; }
    @Override public VaadinIcon getTabIcon() { return VaadinIcon.CHART; }
    @Override public DomEventListener onTabClick() { return e -> MainView.getMainView().setContent(this); }

    @Override
    public void prepare() {
        removeAll();
        setElements();
    }

    @Override
    public void setElements() {
        addClassName("dashboard-view");
        removeAll();

        add(new H2("Dashboard"));

        tabActive = new Tab("Aktivni");
        tabArchived = new Tab("Završeni");

        tabs = new Tabs(tabActive, tabArchived);
        tabs.setWidthFull();

        content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        tabs.addSelectedChangeListener(e -> {
            boolean archived = e.getSelectedTab() == tabArchived;
            renderTab(archived);
        });

        add(tabs, content);

        // default: Aktivni
        tabs.setSelectedTab(tabActive);
        renderTab(false);
    }

    private void renderTab(boolean archived) {
        content.removeAll();

        var stats = loadForTab(archived);

        if (stats.isEmpty()) {
            content.add(new Span(archived
                    ? "Nema gotovih dashboardova."
                    : "Nema aktivnih dashboardova."));
            return;
        }

        // responsive “grid”
        HorizontalLayout grid = new HorizontalLayout();
        grid.addClassName("dashboard-grid");
        grid.setWidthFull();
        grid.setWrap(true);
        grid.setSpacing(true);

        for (BoardStatsDto dto : stats) {
            grid.add(buildBoardCard(dto));
        }

        content.add(grid);
    }

    private java.util.List<BoardStatsDto> loadForTab(boolean archived) {
        var user = MainView.getMainView().getLoggedUser();
        boolean isAdmin = user != null && "admin@local".equalsIgnoreCase(user.getEmail());

        if (isAdmin) {
            return archived ? service.getArchivedForAdmin() : service.getActiveForAdmin();
        }
        // owner-only stats
        return archived ? service.getArchivedForOwner(user.getId()) : service.getActiveForOwner(user.getId());
    }

    // ------- isti UI kao kod tebe -------

    private Component buildBoardCard(BoardStatsDto dto) {

        VerticalLayout card = new VerticalLayout();
        card.addClassName("dashboard-card");
        card.setPadding(false);
        card.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("dashboard-card-header");
        header.setWidthFull();
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);

        Span boardName = new Span(dto.getBoardName());
        boardName.addClassName("dashboard-card-title");

        Span overdueBadge = new Span("Overdue: " + dto.getOverdueTasks());
        overdueBadge.addClassName("badge");
        overdueBadge.addClassName(dto.getOverdueTasks() > 0 ? "badge-danger" : "badge-ok");

        header.add(boardName);
        header.add(overdueBadge);
        header.expand(boardName);

        double progress = dto.getProgressPercent();
        Span progressText = new Span((int) Math.round(progress) + "% done");
        progressText.addClassName("dashboard-progress-text");

        ProgressBar bar = new ProgressBar();
        bar.setWidthFull();
        bar.setValue(progress / 100.0);
        bar.addClassName("dashboard-progress");

        HorizontalLayout metrics = new HorizontalLayout();
        metrics.addClassName("dashboard-metrics");
        metrics.setWidthFull();

        metrics.add(metric("Total", dto.getTotalTasks()));
        metrics.add(metric("Active", dto.getActiveTasks()));
        metrics.add(metric("Done", dto.getDoneTasks()));

        HorizontalLayout pr = new HorizontalLayout();
        pr.addClassName("dashboard-priority");
        pr.setWidthFull();

        pr.add(chip("Low", dto.getLowPriority(), "chip-low"));
        pr.add(chip("Med", dto.getMediumPriority(), "chip-med"));
        pr.add(chip("High", dto.getHighPriority(), "chip-high"));

        VerticalLayout body = new VerticalLayout(progressText, bar, metrics, pr);
        body.addClassName("dashboard-card-body");
        body.setPadding(false);
        body.setSpacing(true);

        card.add(header, body);
        return card;
    }

    private Component metric(String label, Long value) {
        VerticalLayout box = new VerticalLayout();
        box.addClassName("metric");
        box.setPadding(false);
        box.setSpacing(false);

        Span l = new Span(label);
        l.addClassName("metric-label");

        Span v = new Span(String.valueOf(value == null ? 0 : value));
        v.addClassName("metric-value");

        box.add(l, v);
        return box;
    }

    private Component chip(String label, Long value, String className) {
        Span s = new Span(label + ": " + (value == null ? 0 : value));
        s.addClassName("chip");
        s.addClassName(className);
        return s;
    }
}
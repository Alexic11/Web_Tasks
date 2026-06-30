package carobnifrulas.web_tasks.ui.views;

import carobnifrulas.web_tasks.dashboard.DashboardService;
import carobnifrulas.web_tasks.security.model.SecurityUtils;
import carobnifrulas.web_tasks.dashboard.dto.BoardStatsDto;
import carobnifrulas.web_tasks.ui.MainView;
import carobnifrulas.web_tasks.ui.menu.MenuTab;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
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
    private HorizontalLayout summaryRow;

    public DashboardView(DashboardService service) {
        this.service = service;
    }

    @Override
    public String getTabName() {
        return "Dashboard";
    }

    @Override
    public VaadinIcon getTabIcon() {
        return VaadinIcon.CHART;
    }

    @Override
    public DomEventListener onTabClick() {
        return e -> MainView.getMainView().setContent(this);
    }

    @Override
    public void setElements() {
        addClassName("dashboard-view");
        removeAll();

        add(buildHeaderSection());

        summaryRow = new HorizontalLayout();
        summaryRow.setWidthFull();
        summaryRow.setSpacing(true);
        summaryRow.getStyle().set("margin-top", "6px");
        add(summaryRow);

        tabActive = new Tab("Aktivni");
        tabArchived = new Tab("Završeni");

        tabs = new Tabs(tabActive, tabArchived);
        tabs.setWidthFull();

        VerticalLayout tabsWrap = new VerticalLayout(tabs);
        tabsWrap.setPadding(false);
        tabsWrap.setSpacing(false);
        tabsWrap.setWidthFull();
        tabsWrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "12px")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)");

        content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();

        tabs.addSelectedChangeListener(e -> {
            boolean archived = e.getSelectedTab() == tabArchived;
            renderTab(archived);
        });

        add(tabsWrap, content);

        tabs.setSelectedTab(tabActive);
        renderTab(false);
    }

    private Component buildHeaderSection() {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);
        wrap.setWidthFull();
        wrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "18px")
                .set("padding", "18px")
                .set("background", "linear-gradient(to right, var(--lumo-primary-color-10pct), white)");

        H2 title = new H2("Dashboard");
        title.getStyle().set("margin", "0");

        Paragraph subtitle = new Paragraph("Pregled statistike boardova, napretka i prioriteta zadataka.");
        subtitle.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-secondary-text-color)");

        wrap.add(title, subtitle);
        return wrap;
    }

    private void renderTab(boolean archived) {
        content.removeAll();

        var stats = loadForTab(archived);
        renderSummary(stats);

        if (stats.isEmpty()) {
            content.add(buildEmptyState(archived));
            return;
        }

        HorizontalLayout grid = new HorizontalLayout();
        grid.addClassName("dashboard-grid");
        grid.setWidthFull();
        grid.setWrap(true);
        grid.setSpacing(true);

        for (BoardStatsDto dto : stats) {
            grid.add(buildBoardCard(dto));
        }

        VerticalLayout gridWrap = new VerticalLayout(grid);
        gridWrap.setPadding(false);
        gridWrap.setSpacing(false);
        gridWrap.setWidthFull();
        gridWrap.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "14px")
                .set("background", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.04)");

        content.add(gridWrap);
    }

    private void renderSummary(java.util.List<BoardStatsDto> stats) {
        summaryRow.removeAll();

        long boards = stats.size();
        long totalTasks = 0;
        long activeTasks = 0;
        long doneTasks = 0;
        long overdue = 0;

        for (BoardStatsDto dto : stats) {
            totalTasks += safe(dto.getTotalTasks());
            activeTasks += safe(dto.getActiveTasks());
            doneTasks += safe(dto.getDoneTasks());
            overdue += safe(dto.getOverdueTasks());
        }

        summaryRow.add(
                buildSummaryCard("Boardovi", String.valueOf(boards), "var(--lumo-primary-color-10pct)"),
                buildSummaryCard("Ukupno taskova", String.valueOf(totalTasks), "var(--lumo-contrast-10pct)"),
                buildSummaryCard("Aktivni", String.valueOf(activeTasks), "var(--lumo-warning-color-10pct)"),
                buildSummaryCard("Done", String.valueOf(doneTasks), "var(--lumo-success-color-10pct)"),
                buildSummaryCard("Overdue", String.valueOf(overdue), "var(--lumo-error-color-10pct)")
        );
    }

    private Component buildSummaryCard(String label, String value, String background) {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(false);
        card.setSpacing(false);
        card.setWidth("210px");

        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("padding", "14px")
                .set("background", background)
                .set("box-sizing", "border-box");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-size", "28px")
                .set("font-weight", "700");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "6px");

        card.add(valueSpan, labelSpan);
        return card;
    }

    private Component buildEmptyState(boolean archived) {
        VerticalLayout empty = new VerticalLayout();
        empty.setPadding(false);
        empty.setSpacing(false);
        empty.setWidthFull();
        empty.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        empty.getStyle()
                .set("padding", "36px")
                .set("border", "1px dashed var(--lumo-contrast-20pct)")
                .set("border-radius", "16px")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("background", "white");

        Span icon = new Span(archived ? "📦" : "📊");
        icon.getStyle().set("font-size", "28px");

        Span text = new Span(archived
                ? "Nema završenih dashboarda."
                : "Nema aktivnih dashboarda.");

        text.getStyle().set("margin-top", "8px");

        empty.add(icon, text);
        return empty;
    }

    private java.util.List<BoardStatsDto> loadForTab(boolean archived) {
        var user = MainView.getMainView().getLoggedUser();
        boolean isAdmin = SecurityUtils.isGlobalAdmin(user);

        if (isAdmin) {
            return archived ? service.getArchivedForAdmin() : service.getActiveForAdmin();
        }

        return archived ? service.getArchivedForOwner(user.getId()) : service.getActiveForOwner(user.getId());
    }

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

    private long safe(Long v) {
        return v == null ? 0 : v;
    }
}
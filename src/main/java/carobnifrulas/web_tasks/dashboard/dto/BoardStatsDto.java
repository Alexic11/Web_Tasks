package carobnifrulas.web_tasks.dashboard.dto;

import lombok.Getter;

@Getter
public class BoardStatsDto {

    private final Long boardId;
    private final String boardName;

    private final Long totalTasks;
    private final Long activeTasks;
    private final Long overdueTasks;
    private final Long doneTasks;

    private final Long lowPriority;
    private final Long mediumPriority;
    private final Long highPriority;

    public BoardStatsDto(
            Long boardId,
            String boardName,
            Long totalTasks,
            Long activeTasks,
            Long overdueTasks,
            Long doneTasks,
            Long lowPriority,
            Long mediumPriority,
            Long highPriority
    ) {
        this.boardId = boardId;
        this.boardName = boardName;

        this.totalTasks = nz(totalTasks);
        this.activeTasks = nz(activeTasks);
        this.overdueTasks = nz(overdueTasks);
        this.doneTasks = nz(doneTasks);

        this.lowPriority = nz(lowPriority);
        this.mediumPriority = nz(mediumPriority);
        this.highPriority = nz(highPriority);
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    public double getProgressPercent() {
        long total = totalTasks == null ? 0L : totalTasks;
        if (total <= 0) return 0.0;

        long done = doneTasks == null ? 0L : doneTasks;
        return (done * 100.0) / total;
    }
}
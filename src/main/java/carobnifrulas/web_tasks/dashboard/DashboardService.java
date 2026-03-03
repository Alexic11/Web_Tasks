package carobnifrulas.web_tasks.dashboard;

import carobnifrulas.web_tasks.dashboard.dto.BoardStatsDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final DashboardRepository repo;

    public DashboardService(DashboardRepository repo) {
        this.repo = repo;
    }

    public List<BoardStatsDto> getActiveForAdmin() {
        return repo.fetchActiveStatsForAdmin();
    }

    public List<BoardStatsDto> getArchivedForAdmin() {
        return repo.fetchArchivedStatsForAdmin();
    }

    public List<BoardStatsDto> getActiveForOwner(long userId) {
        return repo.fetchActiveStatsForOwner(userId);
    }

    public List<BoardStatsDto> getArchivedForOwner(long userId) {
        return repo.fetchArchivedStatsForOwner(userId);
    }
}
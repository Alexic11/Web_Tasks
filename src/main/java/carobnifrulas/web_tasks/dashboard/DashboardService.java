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

    public List<BoardStatsDto> getStatsForAdmin() {
        return repo.fetchStatsForAdmin();
    }

    public List<BoardStatsDto> getStatsForOwner(long userId) {
        return repo.fetchStatsForOwner(userId);
    }

    public boolean hasOwnerBoards(long userId) {
        return repo.hasOwnerBoards(userId);
    }
}
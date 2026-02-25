package carobnifrulas.web_tasks.list;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListService {
    private final ListRepository lists;

    public ListService(ListRepository lists) {
        this.lists = lists;
    }

    public List<ListEntity> findByBoard(Long boardId) {
        return lists.findByBoardIdOrderByPositionAsc(boardId);
    }
}
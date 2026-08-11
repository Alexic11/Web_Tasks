package carobnifrulas.web_tasks.board;

import carobnifrulas.web_tasks.board.BoardMember;
import carobnifrulas.web_tasks.board.BoardMemberId;
import carobnifrulas.web_tasks.board.BoardMemberRepository;
import carobnifrulas.web_tasks.card.CardRepository;
import carobnifrulas.web_tasks.list.ListService;
import carobnifrulas.web_tasks.security.model.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BoardServiceTest {

    private BoardRepository boardRepository;
    private BoardMemberRepository boardMemberRepository;
    private ListService listService;
    private AppUserService userService;
    private CardRepository cardRepository;

    private BoardService service;

    @BeforeEach
    void setUp() {
        boardRepository = mock(BoardRepository.class);
        boardMemberRepository = mock(BoardMemberRepository.class);
        listService = mock(ListService.class);
        userService = mock(AppUserService.class);
        cardRepository = mock(CardRepository.class);

        service = new BoardService(
                boardRepository,
                boardMemberRepository,
                listService,
                userService,
                cardRepository
        );
    }

    @Test
    void createBoardShouldAssignCreatorAsOwner() {
        Board savedBoard = new Board();
        savedBoard.setId(10L);
        savedBoard.setName("Demo Board");

        when(boardRepository.save(any(Board.class)))
                .thenReturn(savedBoard);

        Board result = service.createBoard("Demo Board", 5L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Demo Board", result.getName());

        ArgumentCaptor<BoardMember> captor =
                ArgumentCaptor.forClass(BoardMember.class);

        verify(boardMemberRepository).save(captor.capture());

        BoardMember member = captor.getValue();

        assertEquals(
                new BoardMemberId(10L, 5L),
                member.getId()
        );

        assertEquals("OWNER", member.getRole());

        verify(listService)
                .createDefaultListsIfMissing(10L, 5L);
    }

    @Test
    void createBoardShouldPersistBoard() {
        Board savedBoard = new Board();
        savedBoard.setId(20L);
        savedBoard.setName("Website Redesign");

        when(boardRepository.save(any(Board.class)))
                .thenReturn(savedBoard);

        service.createBoard("Website Redesign", 7L);

        ArgumentCaptor<Board> captor =
                ArgumentCaptor.forClass(Board.class);

        verify(boardRepository).save(captor.capture());

        Board board = captor.getValue();

        assertEquals("Website Redesign", board.getName());
    }
}
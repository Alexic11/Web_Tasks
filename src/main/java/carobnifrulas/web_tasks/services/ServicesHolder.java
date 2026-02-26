package carobnifrulas.web_tasks.services;

import carobnifrulas.web_tasks.board.BoardMemberService;
import carobnifrulas.web_tasks.board.BoardService;
import carobnifrulas.web_tasks.card.CardService;
import carobnifrulas.web_tasks.list.ListService;
import carobnifrulas.web_tasks.security.model.AppUserService;
import carobnifrulas.web_tasks.ui.menu.Menu;
import org.springframework.stereotype.Component;

@Component
public class ServicesHolder {

    public final AppUserService userService;
    public final BoardService boardService;
    public final ListService listService;
    public final CardService cardService;
    public final BoardMemberService boardMemberService;

    // (mali trik) da BoardView može nazad na default view
    public final Menu menu;

    public ServicesHolder(AppUserService userService,
                          BoardService boardService,
                          ListService listService,
                          CardService cardService, BoardMemberService boardMemberService,
                          Menu menu) {
        this.userService = userService;
        this.boardService = boardService;
        this.listService = listService;
        this.cardService = cardService;
        this.boardMemberService = boardMemberService;
        this.menu = menu;
    }
}
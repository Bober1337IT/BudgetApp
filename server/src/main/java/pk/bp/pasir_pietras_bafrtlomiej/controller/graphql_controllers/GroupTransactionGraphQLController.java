package pk.bp.pasir_pietras_bafrtlomiej.controller.graphql_controllers;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.bp.pasir_pietras_bafrtlomiej.dto.GroupTransactionDTO;
import pk.bp.pasir_pietras_bafrtlomiej.model.User;
import pk.bp.pasir_pietras_bafrtlomiej.service.CurrentUserService;
import pk.bp.pasir_pietras_bafrtlomiej.service.GroupTransactionService;

@Controller
public class GroupTransactionGraphQLController {

    private final GroupTransactionService groupTransactionService;
    private final CurrentUserService currentUserService;

    public GroupTransactionGraphQLController(GroupTransactionService groupTransactionService, CurrentUserService currentUserService) {
        this.groupTransactionService = groupTransactionService;
        this.currentUserService = currentUserService;
    }

    @MutationMapping
    public Boolean addGroupTransaction(@Valid @Argument GroupTransactionDTO groupTransactionDTO) {
        User user = currentUserService.getCurrentUser();
        groupTransactionService.addGroupTransaction(groupTransactionDTO, user);
        return true;
    }
}

package pk.bp.pasir_pietras_bafrtlomiej.controller.graphql_controllers;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.bp.pasir_pietras_bafrtlomiej.dto.BalanceDTO;
import pk.bp.pasir_pietras_bafrtlomiej.dto.TransactionDTO;
import pk.bp.pasir_pietras_bafrtlomiej.model.Transaction;
import pk.bp.pasir_pietras_bafrtlomiej.model.User;
import pk.bp.pasir_pietras_bafrtlomiej.service.CurrentUserService;
import pk.bp.pasir_pietras_bafrtlomiej.service.TransactionService;

import java.util.List;

@Controller
public class TransactionGraphQLController {

    private final TransactionService transactionService;
    private final CurrentUserService currentUserService;

    public TransactionGraphQLController(TransactionService transactionService, CurrentUserService currentUserService) {
        this.transactionService = transactionService;
        this.currentUserService = currentUserService;
    }

    @QueryMapping
    public List<Transaction> transactions() {
        return transactionService.getAllTransactions();
    }

    @QueryMapping
    public BalanceDTO userBalance(
            @Argument Double days
    ){
        User user = currentUserService.getCurrentUser();
        return transactionService.calculateBalance(user, days);
    }
    
    @MutationMapping
    public Transaction addTransaction(
            @Valid @Argument TransactionDTO transactionDTO
    ){
        return transactionService.createTransaction(transactionDTO);
    }
    
    @MutationMapping
    public Transaction updateTransaction(
            @Argument Long id,
            @Valid @Argument TransactionDTO transactionDTO
    ){
        return transactionService.updateTransaction(id, transactionDTO);
    }
    
    @MutationMapping
    public Boolean deleteTransaction(
            @Argument Long id
    ){
        try {
            transactionService.deleteTransaction(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

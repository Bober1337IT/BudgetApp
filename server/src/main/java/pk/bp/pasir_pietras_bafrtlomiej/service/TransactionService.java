package pk.bp.pasir_pietras_bafrtlomiej.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pk.bp.pasir_pietras_bafrtlomiej.dto.BalanceDTO;
import pk.bp.pasir_pietras_bafrtlomiej.dto.TransactionDTO;
import pk.bp.pasir_pietras_bafrtlomiej.model.Transaction;
import pk.bp.pasir_pietras_bafrtlomiej.model.TransactionType;
import pk.bp.pasir_pietras_bafrtlomiej.model.User;
import pk.bp.pasir_pietras_bafrtlomiej.repository.TransactionRepository;
import pk.bp.pasir_pietras_bafrtlomiej.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public TransactionService(TransactionRepository transactionRepository, CurrentUserService currentUserService) {
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    public List<Transaction> getAllTransactions() {
        User user = currentUserService.getCurrentUser();
        return transactionRepository.findAllByUser(user);
    }

    public Transaction getTransactionById(Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(currentUserService.getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz uprawnień do tej transakcji");
        }

        return transaction;
    }

    public Transaction updateTransaction(Long id, TransactionDTO transactionDTO) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono tranzakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(currentUserService.getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz uprawnień do tej transakcji");
        }

        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());

        return transactionRepository.save(transaction);
    }

    public Transaction createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());
        transaction.setUser(currentUserService.getCurrentUser());
        transaction.setTimestamp(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono tranzakcji o ID " + id));

        if (!transaction.getUser().getEmail().equals(currentUserService.getCurrentUser().getEmail())) {
            throw new AccessDeniedException("Nie masz uprawnień do tej transakcji");
        }

        transactionRepository.delete(transaction);
    }

    public BalanceDTO calculateBalance(User user, Double days) {
        List<Transaction> userTransactions;

        if (days != null && days > 0) {
            long seconds = (long) (days * 24 * 60 * 60);
            LocalDateTime threshold = LocalDateTime.now().minusSeconds(seconds);
            userTransactions = transactionRepository.findAllByUserAndTimestampGreaterThanEqual(user, threshold);
        } else {
            userTransactions = transactionRepository.findByUser(user);
        }

        double income = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double expense = userTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();

        double balance = income - expense;

        return new BalanceDTO(income, expense, balance);
    }
}

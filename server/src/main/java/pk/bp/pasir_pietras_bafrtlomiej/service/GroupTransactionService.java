package pk.bp.pasir_pietras_bafrtlomiej.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.bp.pasir_pietras_bafrtlomiej.dto.GroupTransactionDTO;
import pk.bp.pasir_pietras_bafrtlomiej.model.Debt;
import pk.bp.pasir_pietras_bafrtlomiej.model.Group;
import pk.bp.pasir_pietras_bafrtlomiej.model.Membership;
import pk.bp.pasir_pietras_bafrtlomiej.model.User;
import pk.bp.pasir_pietras_bafrtlomiej.repository.DebtRepository;
import pk.bp.pasir_pietras_bafrtlomiej.repository.GroupRepository;
import pk.bp.pasir_pietras_bafrtlomiej.repository.MembershipRepository;
import pk.bp.pasir_pietras_bafrtlomiej.websocket.GroupNotificationHandler;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final MembershipService membershipService;
    private final GroupNotificationHandler groupNotificationHandler;

    public GroupTransactionService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            MembershipService membershipService,
            GroupNotificationHandler groupNotificationHandler) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.membershipService = membershipService;
        this.groupNotificationHandler = groupNotificationHandler;
    }

    public void addGroupTransaction(GroupTransactionDTO transactionDTO, User currentUser) {
        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono Grupy"));

        membershipService.assertCurrentUserIsGroupMember(group.getId());

        List<Membership> members = membershipRepository.findByGroupId(group.getId());
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members, currentUser);

        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Grupa nie ma czlonkow, nie mozna dodac transakcji.");
        }

        double amountPerUser = transactionDTO.getAmount() / selectedMembers.size();
        boolean expense = "EXPENSE".equals(transactionDTO.getType());

        for (Membership member : selectedMembers) {
            User otherUser = member.getUser();
            if (!otherUser.getId().equals(currentUser.getId())) {
                Debt debt = new Debt();
                debt.setDebtor(expense ? otherUser : currentUser);
                debt.setCreditor(expense ? currentUser : otherUser);
                debt.setGroup(group);
                debt.setAmount(amountPerUser);
                debt.setTitle(transactionDTO.getTitle());
                debtRepository.save(debt);
            }
        }

        if (expense) {
            notifyExpenseAdded(group, currentUser, selectedMembers,
                    transactionDTO.getTitle(), transactionDTO.getAmount(), amountPerUser);
        }
    }

    private void notifyExpenseAdded(
            Group group,
            User createdBy,
            List<Membership> participants,
            String title,
            double totalAmount,
            double userShare) {
        String message = String.format(
                Locale.ROOT,
                "%s dodał wydatek \"%s\" w grupie %s. Twoja część: %.2f zł.",
                createdBy.getEmail(), title, group.getName(), userShare);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "GROUP_EXPENSE_ADDED");
        payload.put("groupId", group.getId());
        payload.put("groupName", group.getName());
        payload.put("title", title);
        payload.put("amount", totalAmount);
        payload.put("userShare", userShare);
        payload.put("createdByEmail", createdBy.getEmail());
        payload.put("message", message);

        for (Membership member : participants) {
            User recipient = member.getUser();
            if (!recipient.getId().equals(createdBy.getId())) {
                groupNotificationHandler.sendToUser(recipient.getEmail(), payload);
            }
        }
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members,
            User currentUser) {
        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }

        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);
        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();

        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException(
                    "Wszyscy wybrani uzytkownicy musza byc czlonkami grupy.");
        }

        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        if (!currentUserSelected) {
            throw new IllegalStateException(
                    "Aktualny uzytkownik musi byc uczestnikiem transakcji grupowej.");
        }

        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwoch uczestnikow.");
        }

        return selectedMembers;
    }
}

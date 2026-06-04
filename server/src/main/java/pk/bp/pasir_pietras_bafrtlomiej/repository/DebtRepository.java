package pk.bp.pasir_pietras_bafrtlomiej.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.bp.pasir_pietras_bafrtlomiej.model.Debt;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByGroupId(Long groupId);

    void deleteByGroupId(Long groupId);
}

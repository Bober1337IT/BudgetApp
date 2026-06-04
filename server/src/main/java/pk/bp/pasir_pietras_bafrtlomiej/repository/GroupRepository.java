package pk.bp.pasir_pietras_bafrtlomiej.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.bp.pasir_pietras_bafrtlomiej.model.Group;
import pk.bp.pasir_pietras_bafrtlomiej.model.User;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByMemberships_User(User user);
}

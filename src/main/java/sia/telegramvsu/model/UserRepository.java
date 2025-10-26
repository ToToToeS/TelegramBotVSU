package sia.telegramvsu.model;

import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long> {
    User getById(Long id);

    Long id(Long id);

    List<User> findByNewsletter(boolean newsletter);
}

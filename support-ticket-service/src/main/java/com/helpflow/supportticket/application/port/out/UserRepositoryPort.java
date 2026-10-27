package com.helpflow.supportticket.application.port.out;

import com.helpflow.supportticket.application.model.PageQuery;
import com.helpflow.supportticket.application.model.PageResult;
import com.helpflow.supportticket.application.model.UserSearchCriteria;
import com.helpflow.supportticket.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDocument(String document);

    PageResult<User> search(UserSearchCriteria criteria, PageQuery page);
}

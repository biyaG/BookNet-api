package it.unipi.booknetapi.repository.user;

import it.unipi.booknetapi.model.user.Role;
import it.unipi.booknetapi.model.user.User;
import it.unipi.booknetapi.shared.model.PageResult;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryInterface {

    User insert(User user);
    List<User> insertAll(List<User> users);
    boolean updateName(String idUser, String newName);
    boolean updateRole(String idUser, Role newRole);
    boolean updatePassword(String idUser, String newPassword);
    boolean updateImage(String idUser, String newImageUrl);
    boolean delete(String idUser);
    boolean deleteAll(List<String> idUsers);
    Optional<User> findById(String idUser);
    Optional<User> findByUsername(String username);
    PageResult<User> findAll(int page, int size);

}

package it.unipi.booknetapi.service.user;

import it.unipi.booknetapi.command.user.AdminListCommand;
import it.unipi.booknetapi.command.user.ReaderListCommand;
import it.unipi.booknetapi.command.user.ReviewerListCommand;
import it.unipi.booknetapi.command.user.UserGetCommand;
import it.unipi.booknetapi.dto.user.*;
import it.unipi.booknetapi.model.user.*;
import it.unipi.booknetapi.repository.user.UserRepository;
import it.unipi.booknetapi.shared.model.PageResult;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;


    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public ReaderComplexResponse getReaderById(String idUser) {
        User user =  userRepository.findById(idUser)
                .orElse(null);

        if(user == null || user.getRole() == null || user.getRole() != Role.Reader) return null;

        return new ReaderComplexResponse(user);
    }


    public UserResponse getUserById(String idUser) {
        User user =  userRepository.findById(idUser).orElse(null);

        if(user == null) {
            return null;
        }

        switch (user.getRole()) {
            case Admin: return new AdminResponse(user);
            case Reader: new ReaderResponse(user);
            case Reviewer: return new ReviewerResponse(user);
            default: return new UserResponse(user);
        }
    }

    public UserResponse get(UserGetCommand command) {
        User user =  userRepository.findById(command.getId()).orElse(null);

        if(user == null) {
            return null;
        }

        return switch (user.getRole()) {
            case Admin -> new AdminResponse(user);
            case Reader -> new ReaderResponse(user);
            case Reviewer -> new ReviewerResponse(user);
            default -> new UserResponse(user);
        };
    }

    public PageResult<AdminResponse> list(AdminListCommand command) {
        PageResult<Admin> result = this.userRepository.findAllAdmin(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(AdminResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<ReaderResponse> list(ReaderListCommand command) {
        PageResult<Reader> result = this.userRepository.findAllReader(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReaderResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

    public PageResult<ReviewerResponse> list(ReviewerListCommand command) {
        PageResult<Reviewer> result = this.userRepository.findAllReviewer(command.getPagination().getPage(), command.getPagination().getSize());

        return new PageResult<>(
                result.getContent().stream().map(ReviewerResponse::new).toList(),
                result.getTotalElements(),
                result.getCurrentPage(),
                result.getPageSize()
        );
    }

}

package com.ridelink.account.service;

import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UpdateStatusRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.ApiException;
import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;
import com.ridelink.account.model.User;
import com.ridelink.account.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository users;
    @InjectMocks UserService userService;

    @Test
    void getByIdThrowsNotFoundForUnknownUser() {
        UUID id = UUID.randomUUID();
        when(users.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("USER_NOT_FOUND"));
    }

    @Test
    void updateProfileTrimsNameAndSetsPhone() {
        UUID id = UUID.randomUUID();
        User user = new User("Old Name", "a@example.com", "h", null, Role.PASSENGER);
        when(users.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateProfile(id, new UpdateProfileRequest("  New Name  ", "0770000000"));

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.phone()).isEqualTo("0770000000");
    }

    @Test
    void listWithoutRoleReturnsAllUsers() {
        when(users.findAll()).thenReturn(List.of(
                new User("A", "a@x.com", "h", null, Role.PASSENGER),
                new User("B", "b@x.com", "h", null, Role.DRIVER)));

        assertThat(userService.list(null)).hasSize(2);
        verify(users, never()).findByRole(any());
    }

    @Test
    void listWithRoleFiltersByRole() {
        when(users.findByRole(Role.DRIVER)).thenReturn(List.of(new User("B", "b@x.com", "h", null, Role.DRIVER)));

        assertThat(userService.list(Role.DRIVER)).extracting(UserResponse::role).containsExactly(Role.DRIVER);
    }

    @Test
    void updateStatusChangesStatus() {
        UUID id = UUID.randomUUID();
        User user = new User("A", "a@x.com", "h", null, Role.PASSENGER);
        when(users.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userService.updateStatus(id, UUID.randomUUID(), new UpdateStatusRequest(AccountStatus.SUSPENDED));

        assertThat(response.status()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    void adminCannotChangeOwnStatus() {
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> userService.updateStatus(adminId, adminId, new UpdateStatusRequest(AccountStatus.SUSPENDED)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo("SELF_MODIFICATION"));
        verifyNoInteractions(users);
    }
}
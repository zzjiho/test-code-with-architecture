package com.example.demo.service;

import com.example.demo.exception.CertificationCodeNotMatchedException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.UserStatus;
import com.example.demo.model.dto.UserCreateDto;
import com.example.demo.model.dto.UserUpdateDto;
import com.example.demo.repository.UserEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@SqlGroup({
    @Sql(value = "/sql/user-service-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
    @Sql(value = "/sql/delete-all-data.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
})
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void getByEmail은_ACTIVE_상태인_유저를_찾아올수_있다() {
        // given
        String email = "ziho1234567890@gmail.com";

        // when
        UserEntity result = userService.getByEmail(email);

        // then
        assertThat(result.getNickname()).isEqualTo("asdf");
    }

    @Test
    void getByEmail은_PENDING_상태인_유저를_찾아올수_없다() {
        // given
        String email = "ziho1234567891@gmail.com";

        // when
        // then
        assertThatThrownBy(() -> {
            UserEntity result = userService.getByEmail(email);
        }).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById는_ACTIVE_상태인_유저를_찾아올수_있다() {
        // given
        String email = "ziho1234567890@gmail.com";

        // when
        UserEntity result = userService.getById(1);

        // then
        assertThat(result.getNickname()).isEqualTo("asdf");
    }

    @Test
    void getById는_PENDING_상태인_유저를_찾아올수_없다() {
        // given
        String email = "ziho1234567891@gmail.com";

        // when
        // then
        //이 코드를 실행하면 에러가 발생한다.
        assertThatThrownBy(() -> {
            UserEntity result = userService.getById(2);
        }).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void userCreateDto_를_이용하여_유저를_생성할_수_있다() {
        // given
        UserCreateDto userCreateDto = UserCreateDto.builder()
                .email("ziho12345678910.gmail.com")
                .address("seoul")
                .nickname("asdf")
                .build();
        //SimpleMailMessage를 사용하는 send가 호출되어도 아무것도 하지마
        BDDMockito.doNothing().when(javaMailSender).send(ArgumentMatchers.any(SimpleMailMessage.class));

        //when
        UserEntity result = userService.create(userCreateDto);

        //then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void userUpdateDto_를_이용하여_유저를_생성할_수_있다() {
        // given
        UserUpdateDto userUpdateDto = UserUpdateDto.builder()
                .address("zz")
                .nickname("asdfdz")
                .build();

        //when
        userService.update(1, userUpdateDto);

        //then
        UserEntity userEntity = userService.getById(1);
        assertThat(userEntity.getId()).isNotNull();
        assertThat(userEntity.getAddress()).isEqualTo("zz");
        assertThat(userEntity.getNickname()).isEqualTo("asdfdz");
    }

    @Test
    void user를_로그인_시키면_마지막_로그인_시간이_변경된다() {
        // given
        // when
        userService.login(1);

        // then
        UserEntity userEntity = userService.getById(1);
        assertThat(userEntity.getLastLoginAt()).isGreaterThan(0L);
//         assertThat(result.getLastLoginAt()).isEqualTo("T.T"); //FIXME
    }

    @Test
    void PENDING_상태의_사용자는_인증코드로_ACTIVE_시킬_수_있다() {
        // given
        // when
        userService.verifyEmail(2,"aaaaa-aaaa-aaa1");

        // then
        UserEntity userEntity = userService.getById(2);
        assertThat(userEntity.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void PENDING_상태의_사용자는_잘못된_인증코드를_받으면_에러를_던진다() {
        // given
        // when
        // then
        assertThatThrownBy(() -> {
            userService.verifyEmail(2, "aaaaa-aaaa-aaa2");
        }).isInstanceOf(CertificationCodeNotMatchedException.class);
    }





}

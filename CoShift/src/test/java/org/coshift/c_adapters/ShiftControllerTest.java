package org.coshift.c_adapters;

import org.coshift.a_domain.Shift;
import org.coshift.a_domain.person.Person;
import org.coshift.a_domain.person.PersonRole;
import org.coshift.b_application.UseCaseInteractor;
import org.coshift.b_application.ports.PersonRepository;
import org.coshift.c_adapters.security.AuthenticationProviderSpring;
import org.coshift.c_adapters.security.SpringPasswordChecker;
import org.coshift.c_adapters.web.ShiftController;
import org.coshift.d_frameworks.config.SpringConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShiftController.class)
@Import({SpringConfig.class,
         SpringPasswordChecker.class,       // echter Adapter, kein Mock
         AuthenticationProviderSpring.class})
class ShiftControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean
    UseCaseInteractor interactor;

    @MockitoBean PersonRepository  personRepo;

    @MockitoBean org.coshift.b_application.ports.TimeAccountRepository timeAccountRepo;

    @Test
    void authenticatedRequestReturnsShifts() throws Exception {
        List<Shift> shifts = List.of(new Shift(1L, LocalDateTime.now(), 120, 10));
        when(interactor.getAllShifts()).thenReturn(shifts);

        when(interactor.authenticateUser("anton", "secret"))
            .thenReturn(new Person(99L, "anton", "secret", PersonRole.USER));
 
        mvc.perform(get("/api/shifts").with(httpBasic("anton", "secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/shifts"))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"));
    }
}

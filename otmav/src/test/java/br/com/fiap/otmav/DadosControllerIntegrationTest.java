package br.com.fiap.otmav;

import br.com.fiap.otmav.domain.dados.CreateDadosDto;
import br.com.fiap.otmav.domain.dados.Dados;
import br.com.fiap.otmav.domain.dados.DadosRepository;
import br.com.fiap.otmav.domain.dados.UpdateDadosDto;
import br.com.fiap.otmav.domain.funcionario.Funcionario;
import br.com.fiap.otmav.domain.funcionario.FuncionarioRepository;
import br.com.fiap.otmav.domain.motorista.Motorista;
import br.com.fiap.otmav.domain.motorista.MotoristaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN") // MOCK com Auth de ADMIN
public class DadosControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DadosRepository dadosRepository;

    @Autowired
    private FuncionarioRepository funcionarioRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    private final String testCpf1 = "12345678901";
    private final String testCpf2 = "11186233399";

    @BeforeEach
    @Transactional
    void setUp() {
        // ACHA TODOS OS 'DADOS' QUE CONTÉM OS CPF TESTE
        List<Dados> testDados = dadosRepository.findAll().stream()
                .filter(d -> testCpf1.equals(d.getCpf()) || testCpf2.equals(d.getCpf()))
                .toList();

        if (!testDados.isEmpty()) {
            List<Long> dadosIds = testDados.stream().map(Dados::getId).toList();

            List<Funcionario> funcionarios = funcionarioRepository.findAll().stream()
                    .filter(f -> f.getDados() != null && dadosIds.contains(f.getDados().getId()))
                    .toList();
            if (!funcionarios.isEmpty()) {
                funcionarioRepository.deleteAll(funcionarios);
            }

            List<Motorista> motoristas = motoristaRepository.findAll().stream()
                    .filter(m -> m.getDados() != null && dadosIds.contains(m.getDados().getId()))
                    .toList();
            if (!motoristas.isEmpty()) {
                motoristaRepository.deleteAll(motoristas);
            }

            funcionarioRepository.flush();
            motoristaRepository.flush();

            // DELETE OS DADOS, MOTORISTAS E FUNCIONARIOS DEPENDENTES DOS CPFs TESTE
            dadosRepository.deleteAll(testDados);
            dadosRepository.flush();
        }
    }

    private Dados createTestDados(String cpf, String email) {
        Dados dados = new Dados();
        dados.setCpf(cpf);
        dados.setEmail(email);
        dados.setNome("Existing User");
        dados.setSenha("hashedpassword");
        return dadosRepository.saveAndFlush(dados);
    }

    @Test
    @Transactional
    void testDadosCrudLifecycle() throws Exception {
        // 1. CREATE
        CreateDadosDto createDto = new CreateDadosDto(
                testCpf1,
                "11999998888",
                "create@test.com",
                "password123",
                "Test User Create"
        );

        MvcResult createResult = mockMvc.perform(post("/api/dados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cpf", is(testCpf1)))
                .andExpect(jsonPath("$.nome", is("Test User Create")))
                .andExpect(jsonPath("$.email", is("create@test.com")))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        Long createdId = objectMapper.readTree(responseBody).get("id").asLong();

        // 2. READ
        mockMvc.perform(get("/api/dados/" + createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdId.intValue())))
                .andExpect(jsonPath("$.cpf", is(testCpf1)));

        // 3. UPDATE
        UpdateDadosDto updateDto = new UpdateDadosDto(
                "11777776666",
                "update@test.com",
                null,
                "Test User (Updated)"
        );

        mockMvc.perform(put("/api/dados/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdId.intValue())))
                .andExpect(jsonPath("$.nome", is("Test User (Updated)")))
                .andExpect(jsonPath("$.email", is("update@test.com")));

        // 4. DELETE
        mockMvc.perform(delete("/api/dados/" + createdId))
                .andExpect(status().isNoContent());

        // 5. 404 (CONFIRMANDO O DELETE)
        mockMvc.perform(get("/api/dados/" + createdId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void createDados_WhenCpfIsDuplicate_ShouldReturnConflict() throws Exception {
        // ARRANGE
        createTestDados(testCpf2, "existing@test.com");

        // ACT
        CreateDadosDto createDto = new CreateDadosDto(
                testCpf2,
                "11555554444",
                "new_email@test.com",
                "password123",
                "Test User Duplicate"
        );

        // ASSERT: 409 STATUS CODE
        mockMvc.perform(post("/api/dados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void getDados_WhenIdDoesNotExist_ShouldReturnNotFound() throws Exception {
        // ACT E ASSERT: 404 STATUS CODE
        mockMvc.perform(get("/api/dados/999999"))
                .andExpect(status().isNotFound());
    }
}
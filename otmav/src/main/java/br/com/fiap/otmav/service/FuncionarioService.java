package br.com.fiap.otmav.service;

import br.com.fiap.otmav.domain.dados.Dados;
import br.com.fiap.otmav.domain.dados.DadosRepository;
import br.com.fiap.otmav.domain.filial.Filial;
import br.com.fiap.otmav.domain.filial.FilialRepository;
import br.com.fiap.otmav.domain.funcionario.*;
import br.com.fiap.otmav.exception.DuplicateEntryException;
import br.com.fiap.otmav.exception.NotFoundException;
import br.com.fiap.otmav.infra.security.LoginDto;
import br.com.fiap.otmav.infra.security.TokenResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    @Autowired
    private final FuncionarioRepository funcionarioRepository;

    @Autowired
    private final DadosRepository dadosRepository;

    @Autowired
    private final FilialRepository filialRepository;

    @Autowired
    private final DadosService dadosService;

    @Autowired
    private final AuthenticationManager authenticationManager;

    @Autowired
    private final TokenService tokenService;

    @Transactional
    public ReadFuncionarioDto register(RegisterFuncionarioDto dto) {

        var createDadosDto = dto.dados();
        var readDados = dadosService.create(createDadosDto);

        Dados dadosEntity = dadosRepository.findById(readDados.id())
                .orElseThrow(() -> new IllegalStateException("Dados criados porem nao encontrados com id: " + readDados.id()));

        Filial filial = null;
        if (dto.filialId() != null) {
            filial = filialRepository.findById(dto.filialId())
                    .orElseThrow(() -> new NotFoundException("Filial nao encontrada com id: " + dto.filialId()));
        }

        boolean exists = funcionarioRepository.findAll().stream()
                .anyMatch(f -> f.getDados() != null && f.getDados().getId().equals(dadosEntity.getId()));
        if (exists) {
            throw new DuplicateEntryException("Dados ja relacionados com outro funcionario");
        }

        Funcionario func = new Funcionario();
        func.setCargo(dto.cargo());
        func.setDados(dadosEntity);
        func.setFilial(filial);

        Funcionario saved = funcionarioRepository.save(func);
        return new ReadFuncionarioDto(saved);
    }

    // CREATE
    @Transactional
    public ReadFuncionarioDto create(CreateFuncionarioDto dto) {
        Dados dados = dadosRepository.findById(dto.dadosId())
                .orElseThrow(() -> new NotFoundException("Dados nao encontrado com id: " + dto.dadosId()));

        boolean exists = funcionarioRepository.findAll().stream()
                .anyMatch(f -> f.getDados() != null && f.getDados().getId().equals(dto.dadosId()));
        if (exists) {
            throw new DuplicateEntryException("Dados ja relacionados com outro funcionario");
        }

        Filial filial = null;
        if (dto.filialId() != null) {
            filial = filialRepository.findById(dto.filialId())
                    .orElseThrow(() -> new NotFoundException("Filia nao encontrada com id: " + dto.filialId()));
        }

        Funcionario func = new Funcionario();
        func.setCargo(dto.cargo());
        func.setDados(dados);
        func.setFilial(filial);

        Funcionario saved = funcionarioRepository.save(func);
        return new ReadFuncionarioDto(saved);
    }

    // FIND ALL FILTERED
    public Page<ReadFuncionarioDto> findAllFiltered(
            String cargo,
            Long filialId,
            Long dadosId,
            Pageable pageable) {

        return funcionarioRepository.findAllFiltered(cargo, filialId, dadosId, pageable)
                .map(ReadFuncionarioDto::new);
    }

    // FIND BY ID
    public ReadFuncionarioDto findById(Long id) {
        return funcionarioRepository.findById(id)
                .map(ReadFuncionarioDto::new)
                .orElseThrow(() -> new NotFoundException("Funcionario nao encontrado com id: " + id));
    }

    // UPDATE
    @Transactional
    public ReadFuncionarioDto update(Long id, UpdateFuncionarioDto dto) {
        Funcionario func = funcionarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Funcionario nao encontrado com id: " + id));

        if (dto.cargo() != null) func.setCargo(dto.cargo());

        if (dto.dadosId() != null) {
            Dados dados = dadosRepository.findById(dto.dadosId())
                    .orElseThrow(() -> new NotFoundException("Dados nao encontrados com id: " + dto.dadosId()));
            func.setDados(dados);
        }

        if (dto.filialId() != null) {
            Filial filial = filialRepository.findById(dto.filialId())
                    .orElseThrow(() -> new NotFoundException("Filial nao encontrada com id: " + dto.filialId()));
            func.setFilial(filial);
        }

        Funcionario updated = funcionarioRepository.save(func);
        return new ReadFuncionarioDto(updated);
    }

    // DELETE
    @Transactional
    public void delete(Long id) {
        if (!funcionarioRepository.existsById(id)) {
            throw new NotFoundException("Funcionario not found with id: " + id);
        }
        funcionarioRepository.deleteById(id);
    }

    // LOGIN
    public ResponseEntity<TokenResponse> login(LoginDto request) {
        try {
            var authToken = new UsernamePasswordAuthenticationToken(request.email(), request.password());
            Authentication authentication = authenticationManager.authenticate(authToken);

            var principal = authentication.getPrincipal();
            if (principal instanceof br.com.fiap.otmav.infra.security.FuncionarioUserDetails userDetails) {
                var funcionario = userDetails.getFuncionario();
                var token = tokenService.generateToken(funcionario);
                return ResponseEntity.ok(new TokenResponse(token));
            } else {
                var email = request.email();
                var funcionario = funcionarioRepository.findByDadosEmail(email)
                        .orElseThrow(() -> new NotFoundException("Funcionario nao encontrado com o email: " + email));
                var token = tokenService.generateToken(funcionario);
                return ResponseEntity.ok(new TokenResponse(token));
            }
        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new br.com.fiap.otmav.exception.AuthenticationException("Email ou Senha Invalidos");
        }
    }
}

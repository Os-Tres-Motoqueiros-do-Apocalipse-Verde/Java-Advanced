package fiap.com.sensorium.domain.motorista;

import fiap.com.sensorium.domain.dados.Dados;
import fiap.com.sensorium.infra.exception.CustomJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotoristaRepository extends CustomJpaRepository<Motorista, Long> {
    Page<Motorista> findByPlanoContainingIgnoreCase(String plano, Pageable pageable);
}

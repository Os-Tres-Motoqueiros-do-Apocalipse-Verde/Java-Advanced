package br.com.fiap.otmav.service;

import br.com.fiap.otmav.domain.modelo.Modelo;
import br.com.fiap.otmav.domain.modelo.ModeloRepository;
import br.com.fiap.otmav.domain.modelo.ReadModeloDto;
import br.com.fiap.otmav.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeloServiceTest {

    @Mock
    private ModeloRepository modeloRepository;

    @InjectMocks
    private ModeloService modeloService;

    @Test
    void findById_WhenIdExists_ShouldReturnDto() {
        // ARRANGE
        Modelo modelo = new Modelo();
        modelo.setId(1L);
        modelo.setNomeModelo("Test Model");
        modelo.setTanque(15);
        modelo.setTipoCombustivel("Flex");
        modelo.setConsumo(10);

        when(modeloRepository.findById(1L)).thenReturn(Optional.of(modelo));

        // ACT
        ReadModeloDto resultDto = modeloService.findById(1L);

        // ASSERT
        assertNotNull(resultDto);
        assertEquals(1L, resultDto.id());
        assertEquals("Test Model", resultDto.nomeModelo());
    }

    @Test
    void findById_WhenIdDoesNotExist_ShouldThrowNotFoundException() {
        // ARRANGE
        when(modeloRepository.findById(99L)).thenReturn(Optional.empty());

        // ACT
        // ASSERT
        assertThrows(NotFoundException.class, () -> {
            modeloService.findById(99L);
        });
    }
}
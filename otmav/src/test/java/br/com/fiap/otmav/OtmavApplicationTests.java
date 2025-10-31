package br.com.fiap.otmav;

import br.com.fiap.otmav.controller.ModeloController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OtmavApplicationTests {

	@Autowired
	private ModeloController modeloController;

	@Test
	void contextLoads() {
//		SÓ VERIFICANDO SE ALGUM CONTROLLER É CARREGADO
		assertThat(modeloController).isNotNull();
	}

}
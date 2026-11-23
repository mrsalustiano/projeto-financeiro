package br.com.fincontrol.application;

import java.util.List;

public record SeedConfig(String senha, List<PessoaSeed> pessoas) {
}

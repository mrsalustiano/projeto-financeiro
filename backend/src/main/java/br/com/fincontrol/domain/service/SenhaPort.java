package br.com.fincontrol.domain.service;

public interface SenhaPort {

    String hash(String senha);

    boolean confere(String senha, String hash);

    String hashFicticio();
}

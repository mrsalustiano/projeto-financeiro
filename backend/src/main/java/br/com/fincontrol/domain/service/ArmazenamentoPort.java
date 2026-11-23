package br.com.fincontrol.domain.service;

public interface ArmazenamentoPort {

    void guardar(String chave, byte[] conteudo);

    byte[] ler(String chave);

    void remover(String chave);
}

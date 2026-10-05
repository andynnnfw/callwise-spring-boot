package com.andynn.callwise.model;

public enum TipoSenha {
    NORMAL("N"), PRIORITARIA("P");

    private final String prefixo;

    private TipoSenha(String prefixo) {
        this.prefixo = prefixo;
    }

    public String getPrefixo(){
        return prefixo;
    }
}

package br.org.fdte.commons.exceptions;

public class ExcFillData extends Exception {

    /*private String msg = "Faltou preencher campos";

    @Override
    public String getMessage() {
    return msg;
    }*/
    public ExcFillData(String message, Throwable cause) {
        super(message, cause);
    }

    public ExcFillData(String message) {
        super(message);
    }

    public ExcFillData(Throwable cause) {
        super(cause);
    }
}

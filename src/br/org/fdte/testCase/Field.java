package br.org.fdte.testCase;

public class Field {

    String name;
    String value;
    boolean isPositive;

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setPositive(boolean isPositive) {
        this.isPositive = isPositive;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}

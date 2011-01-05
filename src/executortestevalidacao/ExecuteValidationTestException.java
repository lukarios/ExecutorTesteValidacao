
package executortestevalidacao;

public class ExecuteValidationTestException extends Exception {
    public enum ExceptionType { GENERIC_EXCEPTION,
                                DATABASE_ACCESS_ERROR,
                                SUITE_NOT_FOUND };

    public ExceptionType type;
    public String message;

    ExecuteValidationTestException(ExceptionType type, String msg) {
        super();
        this.type = type;
        this.message = msg;
    } // constructor
    
} //ExecuteValidationTestException


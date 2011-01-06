
package executortestevalidacao;

public interface ExecutionCallback {
    
   public void executionEventHandler(ExecutionEvent event) ;
   public void endOfExecution();
   public enum ExecutionEventType {SUITE_STARTED, SUITE_ENDED, SUITE_ABORTED, 
                                    TEST_STARTED, TEST_ENDED, NO_NEGATIVE_TESTS_TO_EXECUTE, NO_POSITIVE_TESTS_TO_EXECUTE, LOOKING_FOR_POSITIVE_CASE,
                                    ATIVATION_STARTED, ATIVATION_ENDED_SUCCESS, ATIVATION_ENDED_FAIL, ATIVATION_ENDED_TIMEOUT };

   public class ExecutionEvent {
       public ExecutionEventType eventType;
       public long timestamp;
       public String objectType;
       public long objectId;
       public String message;
   } // ExecutionEvent

} //ExecutionCallback
 
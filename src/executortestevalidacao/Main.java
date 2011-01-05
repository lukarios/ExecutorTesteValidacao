
package executortestevalidacao;

import java.util.Collection;

public class Main implements ExecutionCallback {

    public void executionEventHandler(ExecutionEvent event) {
        System.out.println("****** Got event " + event.eventType + " " + event.message + " " + event.objectType + " " + event.objectId);
    } // executionEventHandler

    public static void main(String[] args) {
        try {
            ExecutorTesteValidacao ex = new ExecutorTesteValidacao();
            ex.setExecutionCallback( new Main() );

            // demonstrate use of checkForExistingGoldenfile for any test
            // within a suite
            /*
            Collection<String> execs = ex.checkForExistingGoldenfile("suite1");
            if ( execs != null) {
                System.out.println("Suite contem execucoes previas que serao deletadas");
                for (String s : execs) {
                    System.out.println ("Execucao que sera´ removida " + s);
                }
            }
            */

            // demostrate removal of existing GoldenFile execution for
            // any test within a suite
            // Normally not needed because executeValidationSuite does this removal
            // if executing in GoldenFile mode
            /*
               ex.removePreviousExecution(execs);
            */
            // demonstrate execution of a validation suite
            ex.executeValidationSuite("suite1", ExecutorTesteValidacao.ExecutionMode.GOLDEN_FILE, 1, null, false);
        } catch (ExecuteValidationTestException evte) {
            System.out.println (">>> Exception " + evte.type);
            evte.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

} // Main

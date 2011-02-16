package executortestevalidacao;

import java.util.Collection;

import br.org.fdte.persistence.*;

import java.io.OutputStream;

import executortestevalidacao.ExecutorTesteValidacao.ExecutionMode;
import executortestevalidacao.ExecutorTesteValidacao.ExecutionResult;
import executortestevalidacao.ExecutorTesteValidacao.RetrievalResult;

import executortestevalidacao.AtributesAndValues.Atribute;
import executortestevalidacao.AtributesAndValues.Value;
import executortestevalidacao.AtributesAndValues.AtributeIterator;
import org.cabreva.edt.EDTIterativeManager;

/* Estrategia : utilizar um documento de entrada valido e substituir todos
 * os valore negativos de todos os atributos, um a um
 */
class NegativeTestExecution extends ExecutorTesteValidacao {

    private TestResults results = null;
    private long activationId = 0;
    private int repetitions = 1;

    /*lrb 14/02/2011 NegativeTestExecution(String suite,
            ExecutionMode mode,
            int numOfThreads,
            OutputStream logStream,
            boolean abort,
            ExecucaoTesteValidacao currentExecution,
            long activationId) {

    }*/
     NegativeTestExecution(
            ExecutionMode mode,
            int numOfThreads,
            OutputStream logStream,
            boolean abort,
            ExecucaoTesteValidacao currentExecution,
            long activationId,
            EDTIterativeManager edtExec) {
        //lrb 14/02/2011 this.suite = suite;
        //this.suite = suite;
        this.mode = mode;
        this.numOfThreads = numOfThreads;
        this.logStream = logStream;
        this.abort = abort;
        this.currentExecution = currentExecution;
        this.activationId = activationId;
        this.edtExec = edtExec;
    } // constructor

    protected TestResults executeNegativeTests(CaracterizacaoTesteValidacao teste,
            Collection<Especificos> especificos) throws Exception {
        results = new TestResults();
        int casesToTest = teste.getCasosNegativos();
        if (casesToTest == 0) {
            fireEvent(ExecutionCallback.ExecutionEventType.NO_NEGATIVE_TESTS_TO_EXECUTE, "Test", teste.getId(), "");
            return results;
        }

        AtributesAndValues atr = new AtributesAndValues(teste.getDocumentoEntrada());
        //************************************************************************
        // TODO : do we need to add repetitions on negative tests ??
        // if so, just uncomment next line; code has been tested but
        // commented out to lower number of negative tests
        //atr.addRepetitions(repetitions, especificos);
        //************************************************************************
        fireEvent(ExecutionCallback.ExecutionEventType.LOOKING_FOR_POSITIVE_CASE, "Test", teste.getId(), "");
        AtributesAndValues validDoc = buildValidDoc(atr);
        Collection<Atribute> atrs = atr.getAtributeCollection();
        AtributesAndValues retDoc = null;
        for (Atribute t : atrs) {
            AtributeIterator it = atr.getAtributeIterator(t, new Boolean(false));
            Value validAtr = validDoc.get(t);
            Value v = null;
            while (null != (v = it.next())) {
                validDoc.set(t, v);
                long activationTime = System.currentTimeMillis();
                tstCase.setType(positiveToString(false));
                RetrievalResult retRes = executeActivation(teste, validDoc, activationId);
                updateTestResults(retRes.result);
                if (needAbort(retRes.result)) {
                    return results;
                }
                persistActivation(teste, validDoc, retRes.document, activationId, false, retRes.result, activationTime);
                activationId++;
            } // for each value
            validDoc.set(t, validAtr);
        } // for each atribute
        return results;
    } // executeNegativeTests

    private AtributesAndValues buildValidDoc(AtributesAndValues in) {
        // TO DO : check if this doc is really valid by checking rules
        // A new valid doc may be needed if the previous one becomes invalid
        // by checking rules
        return new AtributesAndValues(in, new Boolean(true), true);
    } // buildValidDoc

    private void updateTestResults(ExecutionResult res) {
        if (res.equals(ExecutionResult.FAILURE)) {
            results.negative_negative++;
        }
        if (res.equals(ExecutionResult.SUCCESS)) {
            results.negative_positive++;
        }
        if (res.equals(ExecutionResult.TIMEOUT)) {
            results.timeout++;
        }
    } // updateTestResults

    long getLastActivationId() {
        return activationId;
    } // getLastActivationId
} // NegativeTestExecution


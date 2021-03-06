package executortestevalidacao;

//import br.org.fdte.dao.AtivacaoTesteValidacaoDAO;
import java.util.Collection;

import br.org.fdte.persistence.*;
import br.org.fdte.testCase.TestCase;
import br.org.servicos.ExecucaoTstValidacaoServico;

import java.io.OutputStream;

import executortestevalidacao.ExecutorTesteValidacao.ExecutionMode;
import executortestevalidacao.ExecutorTesteValidacao.ExecutionResult;
import executortestevalidacao.ExecutorTesteValidacao.RetrievalResult;

import org.cabreva.edt.EDTIterativeManager;

public class TestModeExecution extends ExecutorTesteValidacao {

    private long activationId = 0;
    private TestResults results = null;

    TestModeExecution(ExecutionMode mode,
            int numOfThreads,
            OutputStream logStream,
            boolean abort,
            ExecucaoTesteValidacao currentExecution,
            long activationId,
            EDTIterativeManager edtExec,
            TestCase tstCase) {
        this.mode = mode;
        this.numOfThreads = numOfThreads;
        this.logStream = logStream;
        this.abort = abort;
        this.currentExecution = currentExecution;
        this.activationId = activationId;
        this.edtExec = edtExec;
        this.tstCase = tstCase;
    } // constructor

    protected TestResults executeTests(CaracterizacaoTesteValidacao teste, SuiteTesteValidacao suiteTst) throws Exception {
        results = new TestResults();

        //Collection<AtivacaoTesteValidacao> goldenActivations = AtivacaoTesteValidacaoDAO.findGoldenActivations(teste, suiteTst);
        Collection<AtivacaoTesteValidacao> goldenActivations = new ExecucaoTstValidacaoServico().findGoldenActivations(teste, suiteTst);
        for (AtivacaoTesteValidacao goldenActivation : goldenActivations) {
            long activationTime = System.currentTimeMillis();
            AtributesAndValues inputDoc = getInputDoc(goldenActivation);
            //System.out.println("Testing with " + inputDoc.toXmlString());
            AtributesAndValues expectedDoc = getOutputDoc(goldenActivation);
            boolean testType = getTestType(goldenActivation);
            tstCase.setType(positiveToString(testType));
            RetrievalResult resultTeste = executeActivation(teste, inputDoc, activationId);
            ExecutionResult res = null;
            ExecutionResult compareGolden = null;
            if (resultTeste.result.equals(ExecutionResult.TIMEOUT)) {
                res = ExecutionResult.TIMEOUT;
            } else {
                res = resultTeste.result;
                compareGolden = compare(expectedDoc, resultTeste.document);
                /*if (goldenActivation.getResultado().equalsIgnoreCase("S") && resultTeste.result.equals(ExecutionResult.SUCCESS)) {
                    res = compare(expectedDoc, resultTeste.document);
                } else if (goldenActivation.getResultado().equalsIgnoreCase("F") && resultTeste.result.equals(ExecutionResult.FAILURE)) {
                    res = compare(expectedDoc, resultTeste.document);
                } else {
                    res = ExecutionResult.FAILURE;
                }*/
            }
            updateTestResults(res, testType);
            if (needAbort(res)) {
                return results;
            }
            persistActivation(teste, inputDoc, resultTeste.document, activationId, testType, res, activationTime, compareGolden);
            activationId++;
        }
        return results;
    } // executeNegativeTests

    private boolean getTestType(AtivacaoTesteValidacao atv) {
        String positive = atv.getTipo();
        if (positive.equals("P")) {
            return true;
        }
        return false;
    } // getTestType

    private AtributesAndValues getOutputDoc(AtivacaoTesteValidacao atv) throws Exception {
        String doc = new String(atv.getDocumentoSaida());
        return new AtributesAndValues(doc.getBytes());
    } // getOutputDoc

    private AtributesAndValues getInputDoc(AtivacaoTesteValidacao atv) throws Exception {
        String doc = new String(atv.getDocumentoEntrada());
        return new AtributesAndValues(doc.getBytes());
    } // getOutputDoc

    private ExecutionResult compare(AtributesAndValues expected, AtributesAndValues obtained) {
        if (expected == null) {
            return ExecutionResult.FAILURE;
        }
        if (obtained == null) {
            return ExecutionResult.FAILURE;
        }
        if (expected.toXmlString().equals(obtained.toXmlString())) {
            return ExecutionResult.SUCCESS;
        }
        return ExecutionResult.FAILURE;
    } // compare

    private void updateTestResults(ExecutionResult res, boolean type) {
        if (type) {
            if (res.equals(ExecutionResult.FAILURE)) {
                results.positive_negative++;
            }
            if (res.equals(ExecutionResult.SUCCESS)) {
                results.positive_positive++;
            }
        } else {
            if (res.equals(ExecutionResult.FAILURE)) {
                results.negative_negative++;
            }
            if (res.equals(ExecutionResult.SUCCESS)) {
                results.negative_positive++;
            }
        }
        if (res.equals(ExecutionResult.TIMEOUT)) {
            results.timeout++;
        }
    } // updateTestResults

    long getLastActivationId() {
        return activationId;
    } // getLastActivationId
} // TestModeExecution


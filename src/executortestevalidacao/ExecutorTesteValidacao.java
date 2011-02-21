package executortestevalidacao;

import br.org.fdte.commons.exceptions.ExcFillData;
import br.org.fdte.dao.AtivacaoTesteValidacaoDAO;
import br.org.fdte.dao.ExecucaoTesteValidacaoDAO;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Date;

import br.org.fdte.persistence.*;

import java.io.OutputStream;

import executortestevalidacao.ExecutionCallback.ExecutionEvent;
import executortestevalidacao.AtributesAndValues.Atribute;

import br.org.fdte.testCase.Field;
import br.org.fdte.testCase.TestCase;
import br.org.servicos.SuiteServico;
import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;
import org.cabreva.edt.EDTIterativeManager;
import org.jdom.JDOMException;

public class ExecutorTesteValidacao extends Thread {

    public enum ExecutionMode {

        GOLDEN_FILE, SYSTEM_TEST, SYSTEM_EXERCIZE
    };

    public enum ExecutionResult {

        SUCCESS, FAILURE, TIMEOUT
    };
    protected ExecutionCallback executionCallback;
    protected ExecucaoTesteValidacao currentExecution;
    private int idGroup = -1;
    protected static SuiteTesteValidacao suite;
    protected static SuiteServico suiteServico;
    protected ExecutionMode mode;
    protected int numOfThreads;
    protected OutputStream logStream;
    protected boolean abort;
    protected boolean abortNow = false;
    private NegativeTestExecution negativeTestExecution = null;
    private PositiveTestExecution positiveTestExecution = null;
    private TestModeExecution testModeExecution = null;
    private String runSuite;
    private ExecutionMode runMode;
    protected TestCase tstCase = new TestCase();
    protected EDTIterativeManager edtExec;

    public void setRunParameters(String suite, ExecutionMode mode) {
        runSuite = suite;
        runMode = mode;
    }

    @Override
    public void run() {
        try {
            executeValidationSuite(runMode, 1, null, false);
            executionCallback.endOfExecution();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RetrievalResult retrieve(CaracterizacaoTesteValidacao teste) {
        RetrievalResult res = new RetrievalResult();
        res.result = ExecutionResult.SUCCESS;
        res.document = new AtributesAndValues();
        Atribute a = res.document.newAtribute();
        a.name = "atr1";
        res.document.set(a, "valor1");
        return res;
    } // retrieve

    private ExecutionResult submit(CaracterizacaoTesteValidacao teste,
            AtributesAndValues doc) {
        ExecutionResult res = ExecutionResult.SUCCESS;

        List<Field> fields = new ArrayList<Field>();
        for (Atribute atr : doc.getAtributeCollection()) {
            Field field = new Field();
            field.setName(atr.name);
            if (atr.values.iterator().hasNext()) {
                field.setValue(atr.values.iterator().next().value);
            }
            fields.add(field);
        }

        tstCase.setFields(fields);

        try {
            if (currentExecution != null) {
                tstCase.createFileXML();
                this.edtExec.run(tstCase.getFileNameXML());
            }
        } catch (ExcFillData ex) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), ex.getMessage());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), ex.getMessage());
        } catch (JDOMException ex) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), ex.getMessage());
        }

        return res;
    }

    protected RetrievalResult executeActivation(CaracterizacaoTesteValidacao teste,
            AtributesAndValues inputDoc,
            long activationId) {

        fireEvent(ExecutionCallback.ExecutionEventType.ATIVATION_STARTED, "Activation", activationId, "");
        ExecutionResult res = ExecutionResult.SUCCESS;
        inputDoc.dump("Id" + activationId);
        tstCase.setIdActivation(activationId);
        if (currentExecution != null) {
            //currentExecution pode ser nula caso estejamos executando um simples exercicio de sistema
            tstCase.setExecution(currentExecution);
        }
        res = submit(teste, inputDoc);
        RetrievalResult retRes = retrieve(teste);
        res = retRes.result;
        ExecutionCallback.ExecutionEventType etv = ExecutionCallback.ExecutionEventType.ATIVATION_ENDED_SUCCESS;
        if (res.equals(ExecutionResult.FAILURE)) {
            etv = ExecutionCallback.ExecutionEventType.ATIVATION_ENDED_FAIL;
        }
        if (res.equals(ExecutionResult.TIMEOUT)) {
            etv = ExecutionCallback.ExecutionEventType.ATIVATION_ENDED_TIMEOUT;
        }
        fireEvent(etv, "Activation", activationId, "");
        return retRes;
    }

    protected void persistActivation(CaracterizacaoTesteValidacao teste,
            AtributesAndValues validDoc,
            AtributesAndValues retDoc,
            long activationId,
            boolean positiveTeste,
            ExecutionResult executionResult,
            long activationStarted) throws Exception {

        if ((mode.equals(ExecutionMode.GOLDEN_FILE)) || (mode.equals(ExecutionMode.SYSTEM_TEST))) {
            AtivacaoTesteValidacao atv = new AtivacaoTesteValidacao();
            atv.setDocumentoEntrada(validDoc.getBytes());
            atv.setDocumentoSaida(retDoc.getBytes());
            atv.setIdExecucaoTesteValidacao(currentExecution);
            atv.setSequencial((int) activationId);
            atv.setTipo(positiveToString(positiveTeste));
            atv.setResultado(resultToString(executionResult));
            atv.setInicio(new Date(activationStarted));
            atv.setTermino(new Date(System.currentTimeMillis()));
            AtivacaoTesteValidacaoDAO.save(atv);
        }
    }

    private TestResults executeNegativeTests(CaracterizacaoTesteValidacao t,
            Collection<Especificos> especificos) throws Exception {
        negativeTestExecution = new NegativeTestExecution(
                mode,
                numOfThreads,
                logStream,
                abort,
                currentExecution,
                0,
                edtExec,
                tstCase);
        negativeTestExecution.setExecutionCallback(executionCallback);
        TestResults res = negativeTestExecution.executeNegativeTests(t, especificos);
        return res;

    } // executeNegativeTests

    private TestResults executePositiveTests(CaracterizacaoTesteValidacao t,
            Collection<Especificos> especificos) throws Exception {
        long initialId = negativeTestExecution.getLastActivationId();
        positiveTestExecution = new PositiveTestExecution(
                mode,
                numOfThreads,
                logStream,
                abort,
                currentExecution,
                initialId,
                this.edtExec,
                this.tstCase);
        positiveTestExecution.setExecutionCallback(executionCallback);
        TestResults res = positiveTestExecution.executePositiveTests(t, especificos);
        return res;
    } // executeNegativaTests

    private TestResults executeInTestMode(CaracterizacaoTesteValidacao t, SuiteTesteValidacao s) throws Exception {

        int initialId = 0;
        testModeExecution = new TestModeExecution(
                mode,
                numOfThreads,
                logStream,
                abort,
                currentExecution,
                initialId,
                edtExec,
                tstCase);
        testModeExecution.setExecutionCallback(executionCallback);
        TestResults res = testModeExecution.executeTests(t, s);
        return res;
    } // executeNegativaTests

    private ExecutionResult executeValidationTest(CaracterizacaoTesteValidacao t) throws Exception {
        fireEvent(ExecutionCallback.ExecutionEventType.TEST_STARTED, "Test", t.getId(), t.getNome());
        ExecutionResult res = ExecutionResult.SUCCESS;

        SuiteValidacaoTesteValidacao svtvEncontrada = null;
        List<SuiteValidacaoTesteValidacao> lst = suiteServico.getAllSuiteValTesteVal(suite.getNome());
        for (SuiteValidacaoTesteValidacao svtv : lst) {
            if (svtv.getCaracterizacaoTesteValidacao().equals(t)) {
                svtvEncontrada = svtv;
                break;
            }
        }

        tstCase.setWorkflowPath(svtvEncontrada.getWorkflow());
        tstCase.setTestCasePath(svtvEncontrada.getTestCase());

        try {
            if ((mode.equals(ExecutionMode.GOLDEN_FILE)) || (mode.equals(ExecutionMode.SYSTEM_TEST))) {                
                createEdtExec(svtvEncontrada.getWorkflow(), svtvEncontrada.getResult());
                persistTestExecution(t);
            }

            TestResults finalResults = new TestResults();
            TestResults results = null;

            if ((mode == ExecutionMode.GOLDEN_FILE) || (mode == ExecutionMode.SYSTEM_EXERCIZE)) {
                Collection<Especificos> especificos = t.getEspecificosCollection();
                results = executeNegativeTests(t, especificos);
                finalResults.accumulate(results);
                results = executePositiveTests(t, especificos);
                finalResults.accumulate(results);
            } else if (mode == ExecutionMode.SYSTEM_TEST) {
                finalResults = executeInTestMode(t, suite);
            }
            if ((mode.equals(ExecutionMode.GOLDEN_FILE)) || (mode.equals(ExecutionMode.SYSTEM_TEST))) {
                persistTestExecutionResults(finalResults);
                edtExec.stop();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), ex.getMessage());
            res = ExecutionResult.FAILURE;
            return res;
        } finally {            
            fireEvent(ExecutionCallback.ExecutionEventType.TEST_ENDED, "Test", t.getId(), t.getNome());
        }


        return res;
    } // executeValidationTest

    public ExecutionResult executeValidationSuite(
            ExecutionMode mode,
            int numOfThreads,
            OutputStream logStream,
            boolean abort) throws Exception {
        
        this.suiteServico = new SuiteServico();
        this.suite = this.suiteServico.getByName(runSuite);
        this.mode = mode;
        this.numOfThreads = numOfThreads;
        this.logStream = logStream;
        this.abort = abort;


        if (this.suite == null) {
            throw new ExecuteValidationTestException(ExecuteValidationTestException.ExceptionType.SUITE_NOT_FOUND, "Nome da suite: " + suite);
        }
        fireEvent(ExecutionCallback.ExecutionEventType.SUITE_STARTED, "Suite", suite.getId(), "Suite " + suite + "Mode " + mode + " Abort " + abort + " Threads " + numOfThreads);

        Collection<SuiteValidacaoTesteValidacao> suitesCaractTestes = suiteServico.getAllSuiteValTesteVal(suite.getNome());
        Collection<CaracterizacaoTesteValidacao> tests = new ArrayList();
        for (SuiteValidacaoTesteValidacao svtv_instance : suitesCaractTestes) {
            tests.add(svtv_instance.getCaracterizacaoTesteValidacao());
        }

        ExecutionResult res = ExecutionResult.SUCCESS;
        idGroup = ExecucaoTesteValidacaoDAO.getMaxIdGrupoExecPerSuite(suite);
        idGroup++;
        for (CaracterizacaoTesteValidacao t : tests) {
            res = executeValidationTest(t);
            if (needAbort(res)) {
                fireEvent(ExecutionCallback.ExecutionEventType.SUITE_ABORTED, "Suite", this.suite.getId(), "Suite " + suite);
                break;
            }
        } // for each test within suite
        fireEvent(ExecutionCallback.ExecutionEventType.SUITE_ENDED, "Suite", this.suite.getId(), "Suite " + suite);
        idGroup = -1;
        return res;

    }

    private void persistTestExecutionResults(TestResults results) throws Exception {
        currentExecution.setTermino(new Date());
        currentExecution.setCasosFalha(results.negative_negative + results.positive_negative);
        currentExecution.setCasosSucesso(results.positive_positive + results.negative_positive);
        currentExecution.setCasosTimeout(results.timeout);
        ExecucaoTesteValidacaoDAO.save(currentExecution);
    }

    private void persistTestExecution(CaracterizacaoTesteValidacao t) throws Exception {
        ExecucaoTesteValidacao etv = currentExecution = new ExecucaoTesteValidacao();
        etv.setIdGrupoExec(idGroup);
        etv.setIdCaracterizacaoTesteValidacao(t);
        etv.setIdSuite(suite);
        etv.setInicio(new Date());
        etv.setModoAtivacao(modeToString(mode));
        etv.setRelatorio("".getBytes()); // workaround : field must not be null
        ExecucaoTesteValidacaoDAO.save(etv);
    }

    private void createEdtExec(String workflowFileName, String resultDirectory) throws Exception {
        try {
            this.edtExec = new EDTIterativeManager(workflowFileName, resultDirectory);
            this.edtExec.init();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            throw ex;
        }

    }

    protected void fireEvent(ExecutionCallback.ExecutionEventType type, String objectType, long id, String msg) {
        if (executionCallback != null) {
            ExecutionEvent evt = new ExecutionEvent();
            evt.eventType = type;
            evt.objectType = objectType;
            evt.objectId = id;
            evt.message = msg;
            evt.timestamp = System.currentTimeMillis();
            executionCallback.executionEventHandler(evt);
        }
    }

    public void setExecutionCallback(ExecutionCallback executionCallback) {
        this.executionCallback = executionCallback;
    } // setExecutionCallback

    protected boolean needAbort(ExecutionResult res) {
        if (abortNow) {
            return true;
        }
        if (abort && ((res == ExecutionResult.FAILURE) || (res == ExecutionResult.TIMEOUT))) {
            return true;
        }
        return false;
    } // needAbort

    protected static String positiveToString(boolean positive) {
        if (positive) {
            return "P";
        }
        return "N";
    } // positiveToString

    protected static String modeToString(ExecutionMode mode) {
        if (mode.equals(ExecutionMode.GOLDEN_FILE)) {
            return "G";
        }
        if (mode.equals(ExecutionMode.SYSTEM_TEST)) {
            return "T";
        }
        if (mode.equals(ExecutionMode.SYSTEM_EXERCIZE)) {
            return "E";
        }
        return null;
    } // modeToString

    static private String resultToString(ExecutionResult res) {
        if (res.equals(ExecutionResult.SUCCESS)) {
            return "S";
        }
        if (res.equals(ExecutionResult.FAILURE)) {
            return "F";
        }
        if (res.equals(ExecutionResult.TIMEOUT)) {
            return "T";
        }
        return null;
    }

    protected class RetrievalResult {

        ExecutionResult result;
        AtributesAndValues document;
    }

    public void abortExecution() {
        abortNow = true;
        if (negativeTestExecution != null) {
            negativeTestExecution.abortNow = true;
        }
        if (positiveTestExecution != null) {
            positiveTestExecution.abortNow = true;
        }
    }

    public class TestResults {

        int positive_positive = 0;
        int positive_negative = 0;
        int negative_positive = 0;
        int negative_negative = 0;
        int timeout = 0;

        void accumulate(TestResults t) {
            positive_positive += t.positive_positive;
            positive_negative += t.positive_negative;
            negative_positive += t.negative_positive;
            negative_negative += t.negative_negative;
            timeout += t.timeout;
        }
    }
}

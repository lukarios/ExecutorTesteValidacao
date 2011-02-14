package executortestevalidacao;

import br.org.fdte.commons.exceptions.ExcFillData;
import br.org.fdte.dao.AtivacaoTesteValidacaoDAO;
import br.org.fdte.dao.ExecucaoTesteValidacaoDAO;
import javax.persistence.PersistenceException;

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
import java.util.List;
import javax.swing.JOptionPane;

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
    //lrb 14/02/2011 protected String suite;
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

    public void setRunParameters(String suite, ExecutionMode mode) {
        runSuite = suite;
        runMode = mode;
    }

    @Override
    public void run() {
        try {
            //lrb 14/02/2011 executeValidationSuite(runSuite, runMode, 1, null, false);
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

        //lrb 11/02/2011
        //uma lista é criada com os dados de AtributesAndValues que geraram a ativação
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
            }
        } catch (ExcFillData ex) {
            JOptionPane.showMessageDialog(JOptionPane.getRootFrame(), ex.getMessage());
        }

        return res;
    } // submit

    protected RetrievalResult executeActivation(CaracterizacaoTesteValidacao teste,
            AtributesAndValues inputDoc,
            long activationId) {

        fireEvent(ExecutionCallback.ExecutionEventType.ATIVATION_STARTED, "Activation", activationId, "");
        ExecutionResult res = ExecutionResult.SUCCESS;
        inputDoc.dump("Id" + activationId);
        tstCase.setIdActivation(activationId);
        if (currentExecution != null) {
            //lrb 14/02/2011 currentExecution pode ser nula caso estejamos executando um simples exercicio de sistema
            tstCase.setIdExecution(currentExecution.getId());
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
                0);
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
                initialId);
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
                initialId);
        testModeExecution.setExecutionCallback(executionCallback);
        TestResults res = testModeExecution.executeTests(t, s);
        return res;
    } // executeNegativaTests

    private ExecutionResult executeValidationTest(CaracterizacaoTesteValidacao t) throws Exception {
        fireEvent(ExecutionCallback.ExecutionEventType.TEST_STARTED, "Test", t.getId(), t.getNome());
        ExecutionResult res = ExecutionResult.SUCCESS;

        if ((mode.equals(ExecutionMode.GOLDEN_FILE)) || (mode.equals(ExecutionMode.SYSTEM_TEST))) {
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
        }
        fireEvent(ExecutionCallback.ExecutionEventType.TEST_ENDED, "Test", t.getId(), t.getNome());
        return res;
    } // executeValidationTest

    public ExecutionResult executeValidationSuite(
            ExecutionMode mode,
            int numOfThreads,
            OutputStream logStream,
            boolean abort) throws ExecuteValidationTestException {
        //lrb 14/02/2011 this.suite = suite;
        this.suiteServico = new SuiteServico();
        this.suite = this.suiteServico.getByName(runSuite);
        this.mode = mode;
        this.numOfThreads = numOfThreads;
        this.logStream = logStream;
        this.abort = abort;

        try {
            // SuiteTesteValidacao stv = SuiteTesteValidacaoDAO.getSuiteTesteValidacao(suite);
            //lrb 11/02/2011
            //SuiteServico suiteServico = new SuiteServico();
            //SuiteTesteValidacao stv = suiteServico.getByName(suite);
            if (this.suite == null) {
                throw new ExecuteValidationTestException(ExecuteValidationTestException.ExceptionType.SUITE_NOT_FOUND, "Nome da suite: " + suite);
            }
            fireEvent(ExecutionCallback.ExecutionEventType.SUITE_STARTED, "Suite", suite.getId(), "Suite " + suite + "Mode " + mode + " Abort " + abort + " Threads " + numOfThreads);
            //Collection<SuiteValidacaoTesteValidacao> suitesCaractTestes = SuiteValCarTstValDAO.getSuiteVal(stv.getId());
            //lrb 11/02/2011
            Collection<SuiteValidacaoTesteValidacao> suitesCaractTestes = suiteServico.getAllSuiteValTesteVal(suite.getNome());
            Collection<CaracterizacaoTesteValidacao> tests = new ArrayList();
            for (SuiteValidacaoTesteValidacao svtv_instance : suitesCaractTestes) {
                tests.add(svtv_instance.getCaracterizacaoTesteValidacao());
            }

            /*lrb 11/02/2011
            if (mode.equals(ExecutionMode.GOLDEN_FILE)) {
            removePrevious(tests);
            }*/

            ExecutionResult res = ExecutionResult.SUCCESS;
            idGroup = ExecucaoTesteValidacaoDAO.getMaxIdGrupoExecPerSuite(suite);
            idGroup++;
            for (CaracterizacaoTesteValidacao t : tests) {
                //res = executeValidationTest(this.suite, t);
                res = executeValidationTest(t);
                if (needAbort(res)) {
                    fireEvent(ExecutionCallback.ExecutionEventType.SUITE_ABORTED, "Suite", this.suite.getId(), "Suite " + suite);
                    break;
                }
            } // for each test within suite
            fireEvent(ExecutionCallback.ExecutionEventType.SUITE_ENDED, "Suite", this.suite.getId(), "Suite " + suite);
            idGroup = -1;
            return res;
        } catch (PersistenceException pe) {
            pe.printStackTrace();
            throw new ExecuteValidationTestException(ExecuteValidationTestException.ExceptionType.DATABASE_ACCESS_ERROR,
                    pe.toString());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExecuteValidationTestException(ExecuteValidationTestException.ExceptionType.GENERIC_EXCEPTION,
                    e.toString());

        }
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

    //lrb 11/02/2011
   /* protected void removePrevious(Collection<CaracterizacaoTesteValidacao> tests) throws Exception {
    Collection<String> testsToRemove = new ArrayList<String>();
    for (CaracterizacaoTesteValidacao c : tests) {
    testsToRemove.add(c.getNome());
    }
    removePreviousExecution(testsToRemove);
    }*/
    //public void removePreviousExecution(Collection<String> validationTestNames) throws Exception {
    //lrb 11/02/2011
    /*private void removePreviousExecution(Collection<String> validationTestNames) throws Exception {
    try {
    for (String nome : validationTestNames) {
    //CaracterizacaoTesteValidacao tstVal = CaracterizacaoTstValidacaoDAO.getCaracterizacaoTesteValidacao(nome);
    //Collection<ExecucaoTesteValidacao> execs = tstVal.getExecucaoTesteValidacaoCollection();
    SuiteTesteValidacao suiteVal = SuiteTesteValidacaoDAO.getSuiteTesteValidacao(suite);
    Collection<ExecucaoTesteValidacao> execs = suiteVal.getExecucaoTesteValidacaoCollection();

    if (execs != null) {
    for (ExecucaoTesteValidacao ex : execs) {
    if (ex.getModoAtivacao().equalsIgnoreCase("G")) {
    int retorno = AtivacaoTesteValidacaoDAO.deleteByExecution(ex);
    ExecucaoTesteValidacaoDAO.delete(ex.getId().intValue());
    }
    }
    }
    }
    } catch (PersistenceException pe) {
    pe.printStackTrace();
    throw new ExecuteValidationTestException(ExecuteValidationTestException.ExceptionType.DATABASE_ACCESS_ERROR,
    pe.toString());
    } catch (Exception e) {
    e.printStackTrace();
    throw new ExecuteValidationTestException(ExecuteValidationTestException.ExceptionType.GENERIC_EXCEPTION,
    e.toString());
    } // end catch
    } // removePreviousExecution*/

    /* lrb 11/02/2011
    public Collection<String> checkForExistingGoldenfile(String suite)
    throws ExecuteValidationTestException {
    try {
    Collection<String> retval = new ArrayList<String>();
    //lrb 11/02/2011
    //SuiteTesteValidacao stv = SuiteTesteValidacaoDAO.getSuiteTesteValidacao(suite);
    SuiteServico suiteServico = new SuiteServico();
    SuiteTesteValidacao stv = suiteServico.getByName(suite);
    if (stv == null) {
    return null;
    }
    //lrb 11/02/2011 List<SuiteValidacaoTesteValidacao> listSVCTV = SuiteValCarTstValDAO.getSuiteVal(stv.getId());
    List<SuiteValidacaoTesteValidacao> listSVCTV = suiteServico.getAllSuiteValTesteVal(suite);
    for (SuiteValidacaoTesteValidacao relSuiteTeste : listSVCTV) {
    CaracterizacaoTesteValidacao t = relSuiteTeste.getCaracterizacaoTesteValidacao();
    Collection<ExecucaoTesteValidacao> execs = ExecucaoTesteValidacaoDAO.findGoldenExecution(t, "G", stv);
    if (execs != null && execs.size() > 0) {
    retval.add(t.getNome());
    }
    }
    return retval;
    } catch (PersistenceException pe) {
    pe.printStackTrace();
    throw new ExecuteValidationTestException(ExecuteValidationTestException.ExceptionType.DATABASE_ACCESS_ERROR,
    pe.toString());
    } catch (Exception e) {
    e.printStackTrace();
    throw new ExecuteValidationTestException(ExecuteValidationTestException.ExceptionType.GENERIC_EXCEPTION,
    e.toString());
    }
    }*/
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

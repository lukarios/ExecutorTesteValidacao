package br.org.fdte.testCase;

import br.org.fdte.commons.exceptions.ExcFillData;
import java.util.List;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import br.org.fdte.persistence.ExecucaoTesteValidacao;
import java.util.ArrayList;

public class TestCase {

    String type;
    String workflowPath;
    String testCasePath;
    List<DataGroup> dataGroups = new ArrayList<DataGroup>();
    ExecucaoTesteValidacao execucao;
    Long idActivation;
    String fileNameXML;

    public String getFileNameXML() {
        return fileNameXML;
    }

    public void setFields(List<Field> fields) {
        DataGroup dtGroup = new DataGroup();
        dtGroup.fields = fields;
        dataGroups.clear();
        dataGroups.add(dtGroup);
    }

    public void setIdActivation(Long idActivation) {
        this.idActivation = idActivation;
    }

    public void setExecution(ExecucaoTesteValidacao execucao) {
        this.execucao = execucao;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void createFileXML() throws ExcFillData {
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("testCase");
            doc.appendChild(rootElement);

            Attr attrType = doc.createAttribute("type");
            attrType.setValue("p");
            rootElement.setAttributeNode(attrType);

            Element workflow = doc.createElement("workflow");
            rootElement.appendChild(workflow);

            Attr attrWorkflowName = doc.createAttribute("name");
            attrWorkflowName.setValue(workflowPath);
            workflow.setAttributeNode(attrWorkflowName);

            Element parameters = doc.createElement("parameters");
            rootElement.appendChild(parameters);

            for (DataGroup dtg : dataGroups) {

                Element dataGroup = doc.createElement("dataGroup");
                parameters.appendChild(dataGroup);

                Attr attrDtGroupName = doc.createAttribute("name");
                attrDtGroupName.setValue(dtg.name);
                dataGroup.setAttributeNode(attrDtGroupName);

                //para cada Field dentro de um datagroup
                for (Field field : dtg.fields) {

                    Element fieldElement = doc.createElement("field");
                    dataGroup.appendChild(fieldElement);

                    Attr attrFieldName = doc.createAttribute("name");
                    attrFieldName.setValue(field.name);
                    fieldElement.setAttributeNode(attrFieldName);

                    Attr attrFieldValue = doc.createAttribute("value");
                    attrFieldValue.setValue(field.value);
                    fieldElement.setAttributeNode(attrFieldValue);
                }
            }

            if (testCasePath == null) {
                throw new ExcFillData("Não existe um caminho para criar os casos de testes de entrada: ");
            }

            //write the content into xml file / / Escreve o conteúdo em arquivo xml
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            fileNameXML = testCasePath + "\\Exec" + execucao.getId().toString() + "_" + idActivation.toString() + ".xml";
            StreamResult result = new StreamResult(new File(fileNameXML));
            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfeTe) {
            tfeTe.printStackTrace();
        }

    }

    public void setTestCasePath(String testCasePath) {
        this.testCasePath = testCasePath;
    }

    public void setWorkflowPath(String workflowPath) {
        this.workflowPath = workflowPath;
    }


}

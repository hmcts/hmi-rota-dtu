package uk.gov.hmcts.reform.hmi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import uk.gov.hmcts.reform.hmi.config.ValidationConfiguration;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ValidationService {

    private final ValidationConfiguration validationConfiguration;

    public ValidationService(ValidationConfiguration validationConfiguration) {
        this.validationConfiguration = validationConfiguration;
    }

    private Validator initValidator(InputStream xsdInputStream) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(xsdInputStream);
        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    public boolean isValid(InputStream rotaXmlFile) throws IOException {
        try (InputStream xsdFile = this.getClass().getClassLoader()
            .getResourceAsStream(validationConfiguration.getRotaHmiXsd())) {
            Validator validator = initValidator(xsdFile);
            validator.validate(new StreamSource(rotaXmlFile));
            return true;
        } catch (SAXException e) {
            return false;
        }
    }
}

package uk.gov.hmcts.reform.hmi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import uk.gov.hmcts.reform.hmi.config.ValidationConfiguration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

@Slf4j
@Service
public class ValidationService {

    private final ValidationConfiguration validationConfiguration;

    public ValidationService(ValidationConfiguration validationConfiguration) {
        this.validationConfiguration = validationConfiguration;
    }

    private Validator initValidator() throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaFile = new StreamSource(getSchemaFile());
        Schema schema = factory.newSchema(schemaFile);
        return schema.newValidator();
    }

    public boolean isValid(byte[] rotaXml) throws SAXException {
        Validator validator = initValidator();
        try {
            validator.validate(new StreamSource(new ByteArrayInputStream(rotaXml)));
            return true;
        } catch (SAXException | IOException ex) {
            log.error(String.format("Failed to validate the schema with error message: %s", ex.getMessage()));
            return false;
        }
    }

    private File getSchemaFile() {
        return new File(Thread.currentThread().getContextClassLoader()
                            .getResource(validationConfiguration.getRotaHmiXsd())
                            .getFile());
    }
}

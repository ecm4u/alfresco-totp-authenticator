package org.saidone.alfresco.repo.security.authentication;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

public class TotpService {
    private static final Log logger = LogFactory.getLog(TotpService.class);
    private PersonService personService;
    private NodeService nodeService;
    private String issuer;

    public static final QName totpSecretQname = QName.createQName("org.saidone", "totpsecret");

    /**
     * @param personService PersonService
     */
    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    /**
     * @param nodeService NodeService
     */
    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    /**
     * @param issuer String
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void authorizeToken(String username, String token) {
        try {
            AuthenticationUtil.runAs(
                    (AuthenticationUtil.RunAsWork<String>) () -> {
                        NodeRef user = personService.getPerson(username);
                        String secret = (String) nodeService.getProperty(user, totpSecretQname);
                        if (secret != null)
                        // token required
                        {
                            TimeProvider timeProvider = new SystemTimeProvider();
                            CodeGenerator codeGenerator = new DefaultCodeGenerator();
                            CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
                            if (!verifier.isValidCode(secret, token)) {
                                throw new AuthenticationException("Invalid token");
                            }
                        }
                        return null;
                    }, AuthenticationUtil.getSystemUserName());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    public void generateSecret(String user) {
        SecretGenerator secretGenerator = new DefaultSecretGenerator();
        NodeRef userNodeRef = personService.getPerson(user);
        nodeService.setProperty(userNodeRef, totpSecretQname, secretGenerator.generate());
    }

    public void setSecret(String user, String secret) {
        NodeRef userNodeRef = personService.getPerson(user);
        if ("".equals(secret)) {
            nodeService.removeProperty(userNodeRef, totpSecretQname);
        } else {
            nodeService.setProperty(userNodeRef, totpSecretQname, secret);
        }
    }

    public String getSecret(String user) {
        NodeRef userNodeRef = personService.getPerson(user);
        return (String) nodeService.getProperty(userNodeRef, totpSecretQname);
    }

    public String getDataUri(String user) {
        String secret = this.getSecret(user);
        String dataUri;
        if (null == secret)
        {
            dataUri = null;
        }
        else {
            QrData data = new QrData.Builder()
                    .label(user)
                    .secret(secret)
                    .issuer(issuer)
                    .algorithm(HashingAlgorithm.SHA1)
                    .digits(6)
                    .period(30)
                    .build();
            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = null;
            try {
                imageData = generator.generate(data);
            } catch (QrGenerationException e) {
                e.printStackTrace();
            }
            String mimeType = generator.getImageMimeType();
            dataUri = getDataUriForImage(imageData, mimeType);
        }
        return dataUri;
    }

    public void init() {
        logger.info("Starting " + TotpService.class.getName());
    }
}

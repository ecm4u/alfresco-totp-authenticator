package org.saidone.alfresco.repo.web.scripts.bean;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.util.HashMap;
import java.util.Map;

public class TotpGenSecret extends TotpWebScript {

    protected Map<String, Object> executeImpl(
            WebScriptRequest req, Status status, Cache cache) {

        Map<String, Object> model = new HashMap<String, Object>();

        String user = AuthenticationUtil.getFullyAuthenticatedUser();

        String secret = totpService.generateSecret();
        model.put("secret", secret);

        String dataUri = totpService.getDataUri(user, secret);
        model.put("dataUri", dataUri);

        totpService.setSecret(user, secret);

        return model;
    }
}

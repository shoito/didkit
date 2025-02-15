package com.spruceid.didkitexample.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spruceid.DIDKit;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class VerifiablePresentation {
    private static Logger logger = LogManager.getLogger();

    public static Map<String, Object> verifyPresentation(
            final String key,
            final String presentation,
            final String challenge
    ) throws Exception {
        logger.info("Converting String VP into Map<String, Object>");
        logger.info("VP: " + presentation);
        final ObjectMapper mapper = new ObjectMapper();

        final Map<String, Object> presentationMap =
            mapper.readValue(presentation, new TypeReference<>() {});

        return VerifiablePresentation
            .verifyPresentation(key, presentationMap, challenge);
    }

    public static Map<String, Object> verifyPresentation(
            final String key,
            final Map<String, Object> presentation,
            final String challenge
    ) {
        logger.info("Attempting to verify Map presentation");

        final ObjectMapper mapper = new ObjectMapper();

        try {
            final DIDKitOptions options = new DIDKitOptions(
                    "authentication",
                    null,
                    challenge,
                    Resources.baseUrl
            );
            final String vpStr = mapper.writeValueAsString(presentation);
            final String optionsStr = mapper.writeValueAsString(options);

            logger.info("vpStr: " + vpStr);
            logger.info("optionsStr: " + optionsStr);


            final String result = DIDKit.verifyPresentation(vpStr, optionsStr);
            logger.info("DIDKit.verifyPresentation result: " + result);
            final Map<String, Object> resultMap =
                mapper.readValue(result, new TypeReference<>() { });

            if (((List<String>) resultMap.get("errors")).size() > 0) {
                logger.error("VP: " + resultMap.get("errors"));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid presentation");
            }
        } catch (Exception e) {
            logger.error("Failed to verify presentation: " + e.toString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify presentation");
        }

        //Select the first vc if we have multiple in the presentation
        final Object vcs = presentation.get("verifiableCredential");
        logger.info("vcs type: " + vcs.getClass());
        //final Map<String, Object> vc = (Map<String, Object>) (vcs instanceof Object[] ? ((Object[]) vcs)[0] : vcs);
        final Map<String, Object> vc = getFirstVc(vcs);

        try {
            final DIDKitOptions options = new DIDKitOptions(
                    "assertionMethod",
                    null,
                    null,
                    null
            );
            final String vcStr = mapper.writeValueAsString(vc);
            final String optionsStr = mapper.writeValueAsString(options);

            final String result = DIDKit.verifyCredential(vcStr, optionsStr);
            final Map<String, Object> resultMap = mapper.readValue(result, new TypeReference<>() {
            });

            if (((List<String>) resultMap.get("errors")).size() > 0) {
                System.out.println("[ERROR] VC: " + resultMap.get("errors"));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credential");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to verify credential");
        }

        return vc;
    }

    private static Map<String, Object> getFirstVc(Object vcs) {

        if(vcs instanceof Object[]) {
            Object r = ((Object[]) vcs)[0];
            logger.info("r type: " + r.getClass());
            return (Map<String, Object>) r;
        }
        else if(vcs instanceof AbstractList) {
            Object r = ((AbstractList) vcs).get(0);
            logger.info("r type: " + r.getClass());
            return (Map<String, Object>) r;
        }
        else {
            logger.info("vc type: " + vcs.getClass());
            return (Map<String, Object>) vcs;
        }
    }
}

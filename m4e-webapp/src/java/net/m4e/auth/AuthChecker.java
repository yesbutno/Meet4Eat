/*
 * Copyright (c) 2017 by Botorabi. All rights reserved.
 * https://github.com/botorabi/Meet4Eat
 * 
 * License: MIT License (MIT), read the LICENSE text in
 *          main directory for more details.
 */
package net.m4e.auth;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import net.m4e.common.AnnotationUtils;
import net.m4e.core.Log;

/**
 * Class performing authorization checks for accessing resources provided by Java Beans.
 * It uses annotations of type 'AuthRole' on Java Bean classes in order to setup
 * the access control rules.
 * 
 * A List of super user roles can be set via method setGrantAlwaysRoles which is
 * allowed access to any available resource.
 * 
 * Example: The following annotation on a REST bean method defines an access
 * authorization rule.
 * 
 *   This grants access to role MODERATOR.
 *   @AuthRole(grant={AuthRole.USER_ROLE_MODERATOR})
 * 
 *   This grants access to authenticated users.
 *   @AuthRole(grant={AuthRole.VIRT_ROLE_USER})
 *
 *   This grants access to any role, the business logic has to check for authorization.
 *   @AuthRole(grant={AuthRole.VIRT_ENDPOINT_CHECK})
 * 
 * For available user roles see annotation interface AuthRole.
 * 
 * @author boto
 * Date of creation Aug 23, 2017
 */
public class AuthChecker {

    /**
     * Used for logging
     */
    private final static String TAG = "AuthChecker";


    /**
     * A lookup for access rules with fix paths. In order to speed up the lookup, the resource path is used as key.
     */
    private final Map<String /*resource path*/, AuthAccessRuleChecker> accessRulesFixPath;

    /**
     * A list of access rules with comples paths consisting of regular expressions.
     */
    private final List<AuthAccessRuleChecker> accessRulesComplexPath;

    /**
     * A list of roles getting access to any protected resource (e.g. super user roles)
     */
    private final List<String> grantAlwaysRoles = new ArrayList<>();


    /**
     * Create the authorization checker.
     */
    public AuthChecker() {
        accessRulesFixPath = new HashMap<>();
        accessRulesComplexPath = new ArrayList<>();
    }

    /**
     * Initialize the checker given a list of java beans providing resource
     * access (e.g. REST facades).
     * 
     * @param <T>           Type of java bean class
     * @param beanClasses   List of java beans
     */
    public <T> void initialize(List<Class<T>> beanClasses) {
        Log.info(TAG, "Initializing authorization checker");
        setupRules(beanClasses);
    }

    /**
     * Set all roles with no access restriction. A previous list is replaced by the given list.
     * 
     * @param roles List of roles which are always granted access
     */
    public void setGrantAlwaysRoles(List<String> roles) {
        grantAlwaysRoles.clear();
        grantAlwaysRoles.addAll(roles);
        Log.debug(TAG, "Setting grant-always roles: " + String.join(",", roles));
    }

    /**
     * Get all roles which have no access restriction (e.g. an admin role).
     * 
     * @return List of roles without access restriction
     */
    public List<String> getGrantAlwaysRoles() {
        return grantAlwaysRoles;
    }

    /**
     * Setup all authorization rules found on given methods.
     * 
     * @param <T>           Type of java bean class
     * @param beanClasses   List of java beans
     */
    private <T> void setupRules(List<Class<T>> beanClasses) {
        // gather information from all bean classes about authorization relevant annotations
        AnnotationUtils autils = new AnnotationUtils();
        beanClasses.stream().map((cls) -> {
            Log.debug(TAG, " adding rules for bean class " + cls.getName());
            return cls;
        }).forEach((cls) -> {
            // get the base path of the bean class (checking for class' Path annotation)
            String classrulepath = autils.getClassPath(cls);

            autils.getMethodsAuthRule(cls).entrySet().stream().forEach((pathentry) -> {
                
                String fullrespath = classrulepath + (pathentry.getKey().isEmpty() ? "" : "/" + pathentry.getKey());
                AuthAccessRuleChecker rule = new AuthAccessRuleChecker(fullrespath);

                pathentry.getValue().entrySet().stream().forEach((accessentry) -> {
                    rule.addAccessRoles(accessentry.getKey(), accessentry.getValue());
                });
                // path entry is relative to class' path
                // check if a complex path was defined
                if (fullrespath.contains("{")) {
                    accessRulesComplexPath.add(rule);
                }
                else {
                    accessRulesFixPath.put(fullrespath, rule);
                }
                Log.debug(TAG, "   adding rule: " + rule.toString() );
            });
        });
    }

    /**
     * Check authorization of incoming request.
     * 
     * @param basePath  The base path of requesting resource
     * @param request   Incoming request
     * @param userRoles User roles
     * @return          Return true if access authorization was ok, otherwise false.
     */
    public boolean checkAccess(String basePath, HttpServletRequest request, List<String> userRoles) {

        // check for no-restriction access
        if (grantAlwaysRoles.stream().anyMatch((r) -> userRoles.contains(r))) {
            Log.debug(TAG, "Access granted to Grant-Always roles");
            return true;
        }

        boolean grantaccess = false;
        try {
            URL url = new URL(request.getRequestURL().toString());
            String path = url.getPath();

            // do some checks first
            if (!path.startsWith(basePath)) {
                Log.warning(TAG, "Access denied: given path '" + path + "' does not start with expected base path '" + basePath + "'");
                return false;
            }

            String respath = path.substring(basePath.length());
            Log.debug(TAG, "Checking resource path [" + request.getMethod() + "]: " + respath);

            // first check for fix path match
            AuthAccessRuleChecker accrule = accessRulesFixPath.get(respath);
            if (!Objects.isNull(accrule)) {
                grantaccess = accrule.checkFixPath(respath, request.getMethod(), userRoles);
            }
            else {
                // if no hit then check for complex path match
                for (AuthAccessRuleChecker acc: accessRulesComplexPath) {
                    if (acc.checkComplexPath(respath, request.getMethod(), userRoles)) {
                        grantaccess = true;
                        break;
                    }
                }
            }
            
            Log.debug(TAG, "Access granted: " + (grantaccess ? "Yes" : "No"));
        }
        catch(MalformedURLException | SecurityException ex) {
            Log.error(TAG, "An exception happended during auth check: " + ex.getLocalizedMessage());
            return false;
        }
        return grantaccess;
    }
}